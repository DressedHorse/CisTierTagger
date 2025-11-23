package ru.vpb.cistagger;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum Gamemode {
    OP("op", "§cOP", '\uE703'),
    NETHERITE("netherite", "§cNPot", '\uE702'),
    VANILLA("vanilla", "§dVanilla", '\uE707'),
    SWORD("sword", "§bSword", '\uE705'),
    NONE("none", "§cOFF", "");

    private final String name;
    private final String displayName;
    private final String textureCode;

    @Getter @Setter
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

    @Override
    public String toString() {
        return displayName;
    }
}
