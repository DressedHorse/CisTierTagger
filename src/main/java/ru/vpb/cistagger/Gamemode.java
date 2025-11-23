package ru.vpb.cistagger;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public enum Gamemode {
    OP("op", "§cOP", '\uE703'),
    NETHERITE("netherite", "§cNPot", '\uE702'),
    VANILLA("vanilla", "§dVanilla", '\uE707'),
    SWORD("sword", "§bSword", '\uE705'),
    UHC("uhc", "§6UHC", '\uE706'),
    SMP("smp", "§9SMP", '\uE704'),
    DPOT("dpot", "§3DPot", '\uE701'),
    NONE("none", "§cOFF", "");

    private final String name;
    private final String displayName;
    private final String textureCode;
    private final Map<String, String> tierMaps = new ConcurrentHashMap<>();

    private static Gamemode current = SWORD;

    Gamemode(String name, String displayName, String textureCode) {
        this.name = name;
        this.displayName = displayName;
        this.textureCode = textureCode;
    }

    Gamemode(String name, String displayName, char textureCode) {
        this.name = name;
        this.displayName = displayName;
        this.textureCode = String.valueOf(textureCode);
    }

    public static Gamemode getByName(String name) {
        for (Gamemode gm : values()) {
            if (gm.name.equalsIgnoreCase(name)) {
                return gm;
            }
        }
        return NONE;
    }

    public static synchronized Gamemode getCurrent() {
        return current;
    }

    public static synchronized void setCurrent(Gamemode gamemode) {
        current = gamemode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
