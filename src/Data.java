package src;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

public class Data {
    private final String DATA_PATH = System.getProperty("user.home") + "/.todo/data.json";
    private final Gson gson;

    public Data() {
        // O setPrettyPrinting() deixa o JSON "bonitinho" e legível no arquivo
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // SALVAR: Pega o objeto Java e transforma em arquivo JSON
    public void save(DataSystem sistema) {
        try (Writer writer = new FileWriter(DATA_PATH)) {
            gson.toJson(sistema, writer);
        } catch (IOException e) {
            ConsoleUI.error("Erro ao save dados: " + e.getMessage());
        }
    }

    // CARREGAR: Lê o arquivo JSON e transforma em objeto Java
    public DataSystem load() {
        File arquivo = new File(DATA_PATH);
        
        if (!arquivo.exists()) {
            return new DataSystem();
        }

        try (Reader reader = new FileReader(DATA_PATH)) {
            return gson.fromJson(reader, DataSystem.class);
        } catch (IOException e) {
            ConsoleUI.error("Erro ao carregar dados: " + e.getMessage());
            return new DataSystem();
        }
    }
}