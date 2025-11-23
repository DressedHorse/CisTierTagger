package ru.vpb.cistagger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.thread.ThreadExecutor;
import ru.vpb.cistagger.Gamemode;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ConfigManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final File CFG_FILE = new File(mc.runDirectory, "config/cis-tagger.json");

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void save() {
        executorService.submit(ConfigManager::saveInternal);
    }

    public static void load() {
        executorService.submit(ConfigManager::loadInternal);
    }

    private static void saveInternal() {
        JsonObject jsonObject = write();
        String jsonContent = new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);

        try (FileWriter fw = new FileWriter(CFG_FILE)){
            fw.write(jsonContent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void loadInternal() {
        try (FileReader fr = new FileReader(CFG_FILE)) {
            JsonObject jsonObject = new Gson().fromJson(fr, JsonObject.class);

            load(jsonObject);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonObject write() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("gamemode", Gamemode.getCurrent().getName());

        return jsonObject;
    }

    private static void load(JsonObject jsonObject) {
        if (jsonObject.isJsonNull() || !jsonObject.has("gamemode")) return;

        Gamemode gamemode = Gamemode.getByName(jsonObject.get("gamemode").getAsString());

        Gamemode.setCurrent(gamemode);
    }
}
