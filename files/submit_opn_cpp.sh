#!/bin/bash
#SBATCH --job-name=opn_cpp
#SBATCH --nodes=1
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=48
#SBATCH --time=72:00:00
#SBATCH --partition=cpu
#SBATCH --output=opn_%j.log
#SBATCH --error=opn_%j.err
#SBATCH --mail-type=END,FAIL
#SBATCH --mail-user=seu@email.com

# ---------------------------------------------------------------------------
# Módulos — ajuste conforme o cluster
# ---------------------------------------------------------------------------
module load gcc/11.2.0
module load gmp/6.2.1

# ---------------------------------------------------------------------------
# Compilação: modo OpenMP puro (1 nó, 48 cores)
# Remove -DUSE_MPI para compilação sem MPI
# ---------------------------------------------------------------------------
g++ -O3 -march=native -fopenmp \
    opn_search.cpp -o opn_search \
    -lgmp -lgmpxx

# OMP_NUM_THREADS = todos os cores alocados
export OMP_NUM_THREADS=$SLURM_CPUS_PER_TASK

# OMP_PROC_BIND=close: cada thread fica no mesmo NUMA node que criou ela
# OMP_PLACES=cores: uma thread por core físico (não hyperthreading)
export OMP_PROC_BIND=close
export OMP_PLACES=cores

echo "[SLURM] Job $SLURM_JOB_ID | Nó: $(hostname) | Threads: $OMP_NUM_THREADS"
./opn_search
echo "[SLURM] Finalizado em $(date)"

# ===========================================================================
# PARA MÚLTIPLOS NÓS (MPI + OpenMP híbrido):
# Descomente e ajuste as linhas abaixo, e comente a seção acima.
# ===========================================================================
# #SBATCH --nodes=8
# #SBATCH --ntasks-per-node=1        # 1 rank MPI por nó
# #SBATCH --cpus-per-task=48         # 48 threads OpenMP por rank
#
# module load openmpi/4.1
#
# mpicxx -O3 -march=native -fopenmp -DUSE_MPI \
#     opn_search.cpp -o opn_search_mpi \
#     -lgmp -lgmpxx
#
# export OMP_NUM_THREADS=$SLURM_CPUS_PER_TASK
# export OMP_PROC_BIND=close
# export OMP_PLACES=cores
#
# srun --mpi=pmix --ntasks=$SLURM_NNODES ./opn_search_mpi
