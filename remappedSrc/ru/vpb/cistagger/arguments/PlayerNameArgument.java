package ru.vpb.cistagger.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerNameArgument implements ArgumentType<String> {
    private PlayerNameArgument() {
    }

    public static PlayerNameArgument playerName() {
        return new PlayerNameArgument();
    }

    @Override
    public String parse(StringReader stringReader) throws CommandSyntaxException {
        return stringReader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> playerNames = this.getTabPlayerList();
        return net.minecraft.command.CommandSource.suggestMatching(playerNames, builder);
    }

    private List<String> getTabPlayerList() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return new ArrayList<>();

        return mc.getNetworkHandler().getPlayerList().stream()
                .map(info -> info.getProfile().getName())
                .collect(Collectors.toList());
    }
}