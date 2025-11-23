package ru.vpb.cistagger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.vpb.cistagger.arguments.GamemodeArgument;
import ru.vpb.cistagger.arguments.PlayerNameArgument;
import ru.vpb.cistagger.config.ConfigManager;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class CisTagger implements ClientModInitializer {
	public static final String MOD_ID = "cis-tagger";
	private static final MinecraftClient mc = MinecraftClient.getInstance();

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> registerCommands(dispatcher));
		TierTagger.onInitialize();

		ConfigManager.load();
	}

	private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		// Tier Check Command

		LiteralArgumentBuilder<FabricClientCommandSource> cmd =
				ClientCommandManager.literal("cistagger");

		cmd.then(argument("PlayerName", word()).executes(ctx -> {
			TierTagger.getTiersByNickname(ctx.getArgument("PlayerName", String.class))
					.thenAccept(CisTagger::sendChat);
			return 1;
		}));

		cmd.then(argument("PlayerName", PlayerNameArgument.playerName()).executes(ctx -> {
			TierTagger.getTiersByNickname(ctx.getArgument("PlayerName", String.class))
					.thenAccept(CisTagger::sendChat);
			return 1;
		}));

		// Reload Command

		LiteralArgumentBuilder<FabricClientCommandSource> cmd2 =
				ClientCommandManager.literal("cistaggerreload");
		cmd2.executes(a -> {
			TierTagger.updateTiers();


			sendChat(Formatting.GREEN + "Successfully reloaded!");

			return 1;
		});

		// Change Gamemode Command

		LiteralArgumentBuilder<FabricClientCommandSource> cmd3 =
				ClientCommandManager.literal("cistaggerset");

		cmd3.then(argument("GameMode", GamemodeArgument.gameMode()).executes(ctx -> {
			Gamemode gm = ctx.getArgument("GameMode", Gamemode.class);
			Gamemode.setCurrent(gm);

			sendChat(Formatting.GREEN + "Successfully set displaying kit to: " + Formatting.GREEN + gm.getDisplayName());

			return 1;
		}));

		dispatcher.register(cmd);
		dispatcher.register(cmd2);
		dispatcher.register(cmd3);
	}

	public static void sendChat(Object object) {
		MutableText msg = Text.literal("CIS Tagger")
				.append(Text.literal(" » ").formatted(Formatting.GRAY));

		if (object == null) {
			msg.append("NULL");
		} else if (object instanceof Text text) {
			msg.append(text);
		} else {
			msg.append(colored(object.toString()));
		}

		mc.inGameHud.getChatHud().addMessage(msg);
	}

	private <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
		return ClientCommandManager.argument(name, type);
	}

	private static Text colored(String raw) {
		MutableText result = Text.empty();
		Formatting current = Formatting.WHITE;

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c == '§' && i + 1 < raw.length()) {
				if (!buffer.isEmpty()) {
					result.append(Text.literal(buffer.toString()).formatted(current));
					buffer.setLength(0);
				}

				char code = raw.charAt(++i);
				current = Formatting.byCode(code);
				if (current == null) current = Formatting.WHITE;
			} else {
				buffer.append(c);
			}
		}

		if (!buffer.isEmpty()) {
			result.append(Text.literal(buffer.toString()).formatted(current));
		}

		return result;
	}
}
