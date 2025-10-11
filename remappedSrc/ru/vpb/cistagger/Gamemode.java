package ru.vpb.cistagger;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum Gamemode {
    OP("op", "§cOP"),
    NETHERITE("netherite", "§cNPot"),
    VANILLA("vanilla", "§dVanilla"),
    SWORD("sword", "§bSword"),
    NONE("none", "§cOFF");

    private final String name;
    private final String displayName;

    @Getter @Setter
    private static Gamemode current = SWORD;

    Gamemode(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
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
