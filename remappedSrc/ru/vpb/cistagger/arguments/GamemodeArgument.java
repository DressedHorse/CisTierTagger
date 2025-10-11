package ru.vpb.cistagger.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ru.vpb.cistagger.Gamemode;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GamemodeArgument implements ArgumentType<Gamemode> {
    private GamemodeArgument() {
    }

    public static GamemodeArgument gameMode() {
        return new GamemodeArgument();
    }

    @Override
    public Gamemode parse(StringReader stringReader) throws CommandSyntaxException {
        return Gamemode.getByName(stringReader.readString());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return net.minecraft.command.CommandSource.suggestMatching(Arrays.stream(Gamemode.values()).map(Gamemode::getName), builder);
    }
}
