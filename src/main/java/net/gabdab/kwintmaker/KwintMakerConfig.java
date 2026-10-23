package net.gabdab.kwintmaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KwintMakerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("kwintmaker.json");

    public static KwintMakerConfig I = new KwintMakerConfig();

    public boolean enabled = true;
    public double height = 6.0;          // з якої висоти над землею брати булаву
    public int delay = 0;                // тіки очікування перед вибором
    public boolean onlyFalling = true;   // тільки коли гравець падає
    public boolean fromInventory = true; // якщо булави немає в хотбарі - взяти з інвентаря
    public boolean returnAfterLand = true;
    public int returnDelay = 2;
    public boolean skipGliding = false;  // не діяти під час польоту на елітрах
    public boolean showMessages = false;

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                KwintMakerConfig loaded = GSON.fromJson(Files.readString(PATH), KwintMakerConfig.class);
                if (loaded != null) I = loaded;
            } else {
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            I = new KwintMakerConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(I));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
