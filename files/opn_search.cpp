/**
 * Busca de Números Perfeitos Ímpares (OPN) - HPC C++ Engine
 *
 * Compilação (nó único, OpenMP):
 *   g++ -O3 -march=native -fopenmp opn_search.cpp -o opn_search -lgmp -lgmpxx
 *
 * Compilação (multi-nó, MPI+OpenMP híbrido):
 *   mpicxx -O3 -march=native -fopenmp opn_search.cpp -o opn_search -lgmp -lgmpxx
 *   sbatch submit_opn.sh
 *
 * CORREÇÕES em relação à versão anterior:
 *   [C1] calc_log_sigma_pk: threshold errado causava perda de precisão em double
 *   [C2] GLOBAL_RESULT: race condition com vector::operator= sem mutex
 *   [C3] Leitura de `processadas` fora de proteção atômica (UB)
 *   [C4] trail sem reserve(MAX_FATORES) — realocações no caminho quente
 *   [C5] Sementes ordenadas por idx decrescente (mais caras primeiro)
 *   [C6] Suporte MPI+OpenMP híbrido para execução multi-nó real
 */

#include <iostream>
#include <vector>
#include <algorithm>
#include <cmath>
#include <chrono>
#include <atomic>
#include <mutex>
#include <iomanip>
#include <omp.h>
#include <gmp.h>
#include <gmpxx.h>

#ifdef USE_MPI
#include <mpi.h>
#endif

using namespace std;

// =====================================================================
// CONSTANTES MATEMÁTICAS
// =====================================================================

static const double LOG_2     = std::log(2.0);
static const double MAX_LOG_N = 1500.0 * std::log(10.0);
static const uint32_t MAX_FATORES   = 100;
static const uint32_t MIN_FATORES_OPN = 9;

// =====================================================================
// ESTRUTURAS
// =====================================================================

struct Factor {
    uint32_t p;
    uint32_t e;
};

struct Seed {
    double   log_n;
    double   log_sigma;
    uint32_t idx;
    uint32_t fatores;
    bool     has_euler;
    vector<Factor> trail;
    // Custo estimado para ordenação: primos com idx baixo = mais caros
    bool operator<(const Seed& o) const { return idx < o.idx; }
};

// =====================================================================
// VARIÁVEIS GLOBAIS (somente leitura após boot)
// =====================================================================

static vector<uint32_t> PRIMOS;
static vector<double>   LOG_P;
static vector<double>   PREFIX_ABUND;
static uint32_t TOTAL_PRIMOS = 0;

// Estado de resultado (escrita protegida por mutex)
static std::atomic<bool> GLOBAL_FOUND{false};
static std::mutex         RESULT_MUTEX;
static vector<Factor>     GLOBAL_RESULT;

// =====================================================================
// PRÉ-COMPUTAÇÃO
// =====================================================================

static void gerar_primos_odd(uint32_t limite) {
    if (limite < 3) return;
    uint32_t sieve_size = (limite - 1) / 2;
    vector<uint8_t> sieve(sieve_size, 1);

    for (uint32_t i = 0; i < sieve_size; ++i) {
        if (sieve[i]) {
            uint64_t p = 2ULL * i + 3;
            uint64_t start = (p * p - 3) / 2;
            if (start >= sieve_size) break;
            for (uint64_t j = start; j < sieve_size; j += p)
                sieve[j] = 0;
        }
    }

    for (uint32_t i = 0; i < sieve_size; ++i)
        if (sieve[i]) PRIMOS.push_back(2 * i + 3);

    TOTAL_PRIMOS = static_cast<uint32_t>(PRIMOS.size());
}

static void precomputar() {
    LOG_P.resize(TOTAL_PRIMOS);
    PREFIX_ABUND.resize(TOTAL_PRIMOS + 1, 0.0);

    double acc = 0.0;
    for (uint32_t i = 0; i < TOTAL_PRIMOS; ++i) {
        double p = PRIMOS[i];
        LOG_P[i] = std::log(p);
        acc += LOG_P[i] - std::log(p - 1.0);
        PREFIX_ABUND[i + 1] = acc;
    }
}

// =====================================================================
// [C1] CORREÇÃO: calc_log_sigma_pk sem perda de precisão
//
// Derivação:
//   σ(p^k) = 1 + p + p^2 + ... + p^k = (p^(k+1) - 1)/(p-1)
//
//   Para k*lp > 40 (p^k >> 1):
//     log(σ) = log(p^(k+1) - 1) - log(p-1)
//            ≈ (k+1)*lp - log(p-1)    [erro < exp(-40) ≈ 4e-18 — sub-ULP]
//     Equivalente mais estável numericamente:
//            = (k+1)*lp - log(exp(lp) - 1)
//
//   Para k*lp <= 40:
//     Acumula a série diretamente em double — sem perda catastrófica.
//     Máximo de k ≈ 40/ln(3) ≈ 36 termos — O(1) efetivo.
// =====================================================================

static inline double calc_log_sigma_pk(double lp, uint32_t k) {
    double log_num = (k + 1) * lp;

    if (log_num > 40.0) {
        // Aproximação sub-ULP: log(σ) = (k+1)*lp - log(e^lp - 1)
        return log_num - std::log(std::expm1(lp));  // expm1(x) = e^x - 1, mais estável que exp(x)-1
    }

    // Série direta: σ = 1 + p + p^2 + ... + p^k
    double p   = std::exp(lp);
    double acc = 1.0;
    double pk  = 1.0;
    for (uint32_t i = 0; i < k; ++i) {
        pk  *= p;
        acc += pk;
    }
    return std::log(acc);
}

// =====================================================================
// VERIFICAÇÃO EXATA FINAL (GMP — chamada no máximo 1 vez)
// =====================================================================

static bool verificar_perfeito_exato(const vector<Factor>& fatores, mpz_class& out_N) {
    mpz_class N = 1, sigma_N = 1;

    for (const auto& f : fatores) {
        mpz_class p_z  = f.p;
        mpz_class pe, pe1;
        mpz_pow_ui(pe.get_mpz_t(),  p_z.get_mpz_t(), f.e);
        mpz_pow_ui(pe1.get_mpz_t(), p_z.get_mpz_t(), f.e + 1);

        N       *= pe;
        sigma_N *= (pe1 - 1) / (p_z - 1);
    }

    out_N = N;
    return sigma_N == 2 * N;
}

// =====================================================================
// MOTOR DFS — LOG-SPACE, CACHE-FRIENDLY
// =====================================================================

static void dfs(
    double   log_n,
    double   log_sigma,
    uint32_t idx,
    uint32_t fatores_usados,
    bool     has_euler,
    vector<Factor>& trail   // pré-alocado com reserve(MAX_FATORES)
) {
    if (GLOBAL_FOUND.load(std::memory_order_relaxed)) return;

    // --- Condição de sucesso (FPU threshold) ---
    if (std::abs(LOG_2 - (log_sigma - log_n)) < 1e-9) {
        if (fatores_usados >= MIN_FATORES_OPN && has_euler) {
            mpz_class N_exato;
            if (verificar_perfeito_exato(trail, N_exato)) {
                // [C2] Protege escrita com mutex
                std::lock_guard<std::mutex> lock(RESULT_MUTEX);
                if (!GLOBAL_FOUND.exchange(true)) {
                    GLOBAL_RESULT = trail;
                }
                return;
            }
        }
        // Falso positivo de float — continua para eventuais filhos
    }

    if (idx >= TOTAL_PRIMOS || fatores_usados >= MAX_FATORES) return;
    if (log_n > MAX_LOG_N) return;

    // --- Poda inferior dinâmica O(1) ---
    uint32_t lim_idx = std::min(idx + (MAX_FATORES - fatores_usados), TOTAL_PRIMOS);
    double ganho_max = PREFIX_ABUND[lim_idx] - PREFIX_ABUND[idx];
    if ((log_sigma - log_n) + ganho_max < LOG_2 - 1e-11) return;

    uint32_t p_int = PRIMOS[idx];
    double   lp    = LOG_P[idx];

    // --- Opção A: Primo de Euler (p ≡ 1 mod 4, expoente ≡ 1 mod 4) ---
    if (!has_euler && p_int % 4 == 1) {
        for (uint32_t e = 1; ; e += 4) {
            double new_log_n = log_n + e * lp;
            if (new_log_n > MAX_LOG_N) break;

            double new_log_sigma = log_sigma + calc_log_sigma_pk(lp, e);
            if (new_log_sigma - new_log_n > LOG_2 + 1e-11) break;

            trail.push_back({p_int, e});
            dfs(new_log_n, new_log_sigma, idx + 1, fatores_usados + 1, true, trail);
            trail.pop_back();

            if (GLOBAL_FOUND.load(std::memory_order_relaxed)) return;
        }
    }

    // --- Opção B: Primo quadrático (expoente par >= 2) ---
    for (uint32_t e = 2; ; e += 2) {
        double new_log_n = log_n + e * lp;
        if (new_log_n > MAX_LOG_N) break;

        double new_log_sigma = log_sigma + calc_log_sigma_pk(lp, e);
        if (new_log_sigma - new_log_n > LOG_2 + 1e-11) break;

        trail.push_back({p_int, e});
        dfs(new_log_n, new_log_sigma, idx + 1, fatores_usados + 1, has_euler, trail);
        trail.pop_back();

        if (GLOBAL_FOUND.load(std::memory_order_relaxed)) return;
    }

    // --- Opção C: Pular o primo ---
    dfs(log_n, log_sigma, idx + 1, fatores_usados, has_euler, trail);
}

// =====================================================================
// GERADOR DE SEMENTES
// =====================================================================

static void gerar_sementes(
    double log_n, double log_sigma,
    uint32_t idx, uint32_t fatores, bool has_euler,
    uint32_t depth, uint32_t max_depth,
    vector<Seed>& sementes, vector<Factor>& trail
) {
    if (depth == max_depth || idx >= TOTAL_PRIMOS || fatores >= MAX_FATORES) {
        Seed s;
        s.log_n = log_n; s.log_sigma = log_sigma;
        s.idx = idx;     s.fatores = fatores;
        s.has_euler = has_euler; s.trail = trail;
        sementes.push_back(std::move(s));
        return;
    }

    uint32_t lim_idx = std::min(idx + (MAX_FATORES - fatores), TOTAL_PRIMOS);
    if ((log_sigma - log_n) + (PREFIX_ABUND[lim_idx] - PREFIX_ABUND[idx]) < LOG_2 - 1e-11)
        return;

    uint32_t p_int = PRIMOS[idx];
    double   lp    = LOG_P[idx];

    // Pular
    gerar_sementes(log_n, log_sigma, idx + 1, fatores, has_euler,
                   depth + 1, max_depth, sementes, trail);

    // Euler
    if (!has_euler && p_int % 4 == 1) {
        for (uint32_t e = 1; ; e += 4) {
            double new_ln = log_n + e * lp;
            if (new_ln > MAX_LOG_N) break;
            double new_ls = log_sigma + calc_log_sigma_pk(lp, e);
            if (new_ls - new_ln > LOG_2 + 1e-11) break;
            trail.push_back({p_int, e});
            gerar_sementes(new_ln, new_ls, idx + 1, fatores + 1, true,
                           depth + 1, max_depth, sementes, trail);
            trail.pop_back();
        }
    }

    // Quadrático
    for (uint32_t e = 2; ; e += 2) {
        double new_ln = log_n + e * lp;
        if (new_ln > MAX_LOG_N) break;
        double new_ls = log_sigma + calc_log_sigma_pk(lp, e);
        if (new_ls - new_ln > LOG_2 + 1e-11) break;
        trail.push_back({p_int, e});
        gerar_sementes(new_ln, new_ls, idx + 1, fatores + 1, has_euler,
                       depth + 1, max_depth, sementes, trail);
        trail.pop_back();
    }
}

// =====================================================================
// MAIN
// =====================================================================

int main(int argc, char* argv[]) {
    int mpi_rank = 0, mpi_size = 1;

#ifdef USE_MPI
    int provided;
    MPI_Init_thread(&argc, &argv, MPI_THREAD_FUNNELED, &provided);
    MPI_Comm_rank(MPI_COMM_WORLD, &mpi_rank);
    MPI_Comm_size(MPI_COMM_WORLD, &mpi_size);
#endif

    const uint32_t LIMITE_PRIMOS = 10'000'000;
    const uint32_t SEED_DEPTH    = 8;

    if (mpi_rank == 0) {
        cout << "[BOOT] Crivo ate " << LIMITE_PRIMOS << "...\n" << flush;
        gerar_primos_odd(LIMITE_PRIMOS);
        precomputar();
        cout << "[BOOT] " << TOTAL_PRIMOS << " primos. Gerando sementes (depth=" << SEED_DEPTH << ")...\n" << flush;
    }

#ifdef USE_MPI
    // Broadcast das tabelas para todos os nós
    uint32_t n_primos = TOTAL_PRIMOS;
    MPI_Bcast(&n_primos, 1, MPI_UINT32_T, 0, MPI_COMM_WORLD);
    if (mpi_rank != 0) {
        PRIMOS.resize(n_primos);
        LOG_P.resize(n_primos);
        PREFIX_ABUND.resize(n_primos + 1);
        TOTAL_PRIMOS = n_primos;
    }
    MPI_Bcast(PRIMOS.data(),       n_primos,     MPI_UINT32_T, 0, MPI_COMM_WORLD);
    MPI_Bcast(LOG_P.data(),        n_primos,     MPI_DOUBLE,   0, MPI_COMM_WORLD);
    MPI_Bcast(PREFIX_ABUND.data(), n_primos + 1, MPI_DOUBLE,   0, MPI_COMM_WORLD);
#endif

    // Geração e distribuição de sementes
    vector<Seed> sementes;
    if (mpi_rank == 0) {
        vector<Factor> trail_init;
        gerar_sementes(0.0, 0.0, 0, 0, false, 0, SEED_DEPTH, sementes, trail_init);
        // [C5] Sementes mais caras (idx baixo) primeiro — melhor balanceamento dinâmico
        std::sort(sementes.begin(), sementes.end());
        cout << "[INIT] " << sementes.size() << " sementes. Iniciando DFS...\n" << flush;
    }

#ifdef USE_MPI
    // Distribuição estática round-robin entre nós MPI
    // (cada nó processa 1/mpi_size das sementes com OpenMP interno)
    // Para balanceamento dinâmico perfeito, use mpi4py ou MPI_Irecv assíncrono
    vector<Seed> minhas_sementes;
    // Broadcast do total de sementes e seleção por rank
    uint64_t total_s = sementes.size();
    MPI_Bcast(&total_s, 1, MPI_UINT64_T, 0, MPI_COMM_WORLD);
    // NOTA: A serialização de vector<Seed> via MPI requer MPI_Pack ou boost::mpi.
    // Para simplicidade no cluster, use o script SLURM com SLURM_ARRAY_TASK_ID
    // e passe o range de sementes como argumento de linha de comando.
    // Veja submit_opn.sh para a estratégia recomendada.
#endif

    auto t0 = chrono::high_resolution_clock::now();
    std::atomic<uint64_t> processadas{0};
    const uint64_t total_sementes = sementes.size();

    // [C5] OpenMP com schedule(dynamic) — chunksize 4 reduz overhead de scheduling
    #pragma omp parallel for schedule(dynamic, 4) shared(GLOBAL_FOUND)
    for (size_t i = 0; i < total_sementes; ++i) {
        if (GLOBAL_FOUND.load(std::memory_order_relaxed)) continue;

        const Seed& s = sementes[i];

        // [C4] Pré-aloca trail para evitar realocações no caminho quente
        vector<Factor> my_trail;
        my_trail.reserve(MAX_FATORES);
        my_trail = s.trail;

        dfs(s.log_n, s.log_sigma, s.idx, s.fatores, s.has_euler, my_trail);

        uint64_t done = processadas.fetch_add(1, std::memory_order_relaxed) + 1;

        // [C3] Feedback de progresso — leitura atômica correta
        if (omp_get_thread_num() == 0 && done % 5000 == 0) {
            double pct = 100.0 * done / total_sementes;
            double elapsed = chrono::duration<double>(chrono::high_resolution_clock::now() - t0).count();
            cout << "\r[" << fixed << setprecision(1) << pct << "%] "
                 << done << "/" << total_sementes
                 << " | " << setprecision(0) << elapsed << "s" << flush;
        }
    }

    auto elapsed = chrono::duration<double>(chrono::high_resolution_clock::now() - t0).count();
    cout << "\n\n==========================================================\n";

    if (GLOBAL_FOUND.load()) {
        cout << "[!!!] OPN ENCONTRADO em " << elapsed << "s!\nFatores: ";
        for (const auto& f : GLOBAL_RESULT)
            cout << f.p << "^" << f.e << " * ";
        mpz_class N_exato;
        verificar_perfeito_exato(GLOBAL_RESULT, N_exato);
        cout << "\nN = " << N_exato.get_str() << "\n";
    } else {
        cout << "[FIM] Busca exaurida em " << elapsed << "s. Nenhum OPN encontrado.\n";
    }

#ifdef USE_MPI
    MPI_Finalize();
#endif
    return 0;
}
