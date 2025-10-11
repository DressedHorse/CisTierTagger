package ru.vpb.cistagger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TierTagger {
    private static final Map<String, String> VANILLA_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, String> SWORD_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, String> NPOT_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, String> OP_TIERS = new ConcurrentHashMap<>();

    public static void onInitialize() {
        updateTiers();
    }

    public static void updateTiers() {
        HttpClient client = HttpClient.newHttpClient();

        loadTiers(client, "op", OP_TIERS);
        loadTiers(client, "vanilla", VANILLA_TIERS);
        loadTiers(client, "sword", SWORD_TIERS);
        loadTiers(client, "netherite", NPOT_TIERS);
    }

    private static void loadTiers(HttpClient client, String kit, Map<String, String> targetMap) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.cistiers.net/v1/get-table/" + kit))
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Accept-Language", "ru,en-US;q=0.9,en;q=0.8,nl;q=0.7,de;q=0.6,es;q=0.5")
                .header("Cache-Control", "max-age=0")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 OPR/122.0.0.0")
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    try {
                        JsonElement parsed = JsonParser.parseString(response);
                        JsonObject json = parsed.getAsJsonObject();

                        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                            String tier = entry.getKey().toUpperCase();
                            entry.getValue().getAsJsonArray();

                            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                                if (!element.isJsonObject()) continue;
                                JsonObject obj = element.getAsJsonObject();
                                if (!obj.has("nickname") || obj.get("nickname").isJsonNull()) continue;

                                String nickname = obj.get("nickname").getAsString();
                                if (nickname.equalsIgnoreCase("Fepis")) System.out.println("AIYIDFTGIUHAIJD");

                                targetMap.put(nickname, tier);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка при парсинге JSON " + kit + ": " + e.getMessage());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Ошибка при загрузке тиров " + kit + ": " + ex.getMessage());
                    return null;
                });
    }

    public static CompletableFuture<Text> getTiersByNickname(String nickname) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://api.cistiers.net/v1/get-user-by-nickname/" + nickname))
                .GET()
                .header("Accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(response -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        JsonObject tierJson = json.getAsJsonObject("tier");

                        MutableText sb = Text.literal(nickname).formatted(Formatting.GRAY);

                        Map<String, String> tiers = new LinkedHashMap<>();
                        tiers.put("Vanilla", tierJson.has("vanilla") && !tierJson.get("vanilla").isJsonNull() ?
                                tierJson.get("vanilla").getAsString() : null);
                        tiers.put("Sword", tierJson.has("sword") && !tierJson.get("sword").isJsonNull() ?
                                tierJson.get("sword").getAsString() : null);
                        tiers.put("OP", tierJson.has("op") && !tierJson.get("op").isJsonNull() ?
                                tierJson.get("op").getAsString() : null);
                        tiers.put("Netherite", tierJson.has("netherite") && !tierJson.get("netherite").isJsonNull() ?
                                tierJson.get("netherite").getAsString() : null);
                        tiers.put("SMP", tierJson.has("smp") && !tierJson.get("smp").isJsonNull() ?
                                tierJson.get("smp").getAsString() : null);
                        tiers.put("UHC", tierJson.has("uhc") && !tierJson.get("uhc").isJsonNull() ?
                                tierJson.get("uhc").getAsString() : null);
                        tiers.put("DPot", tierJson.has("dpot") && !tierJson.get("dpot").isJsonNull() ?
                                tierJson.get("dpot").getAsString() : null);
                        tiers.put("Crystal", tierJson.has("crystal") && !tierJson.get("crystal").isJsonNull() ?
                                tierJson.get("crystal").getAsString() : null);

                        boolean hasAnyTier = false;

                        for (Map.Entry<String, String> entry : tiers.entrySet()) {
                            String tierName = entry.getValue();
                            if (tierName != null && !tierName.isEmpty()) {
                                hasAnyTier = true;
                                int color = getTierColor(tierName);
                                sb.append(Text.literal(" | ").formatted(Formatting.WHITE).append(Text.literal(entry.getKey() + ": ").formatted(Formatting.DARK_GRAY)));
                                sb.append(Text.literal(tierName).styled(style -> style.withColor(color)));
                            }
                        }

                        if (!hasAnyTier) {
                            return Text.literal(nickname).formatted(Formatting.GRAY).append(" | ").formatted(Formatting.WHITE).append(Text.literal("No tiers found").formatted(Formatting.RED));
                        }

                        return sb;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Text.literal(nickname).formatted(Formatting.GRAY).append(" | ").formatted(Formatting.WHITE).append(Text.literal("No tiers found").formatted(Formatting.RED));
                    }
                });
    }

    public static Text appendTier(PlayerEntity player, Text baseText) {
        MutableText tierText = getPlayerTier(player.getName().getString());

        if (tierText != null) {
            return Text.literal("")
                    .append(tierText)
                    .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
                    .append(baseText);
        }
        return baseText;
    }

    @Nullable
    private static MutableText getPlayerTier(String username) {
        String mode = Gamemode.getCurrent().getName();

        String foundTier = switch (mode.toLowerCase()) {
            case "vanilla" -> VANILLA_TIERS.get(username);
            case "sword" -> SWORD_TIERS.get(username);
            case "netherite" -> NPOT_TIERS.get(username);
            case "op" -> OP_TIERS.get(username);
            default -> null;
        };

        if (foundTier == null)
            return null;

        int color = getTierColor(foundTier);
        return Text.literal(foundTier).styled(style -> style.withColor(color));
    }

    private static int getTierColor(String tier) {
        return switch (tier.toUpperCase(Locale.ROOT)) {
            case "HT1" -> 0xFF5555;  // Красный
            case "LT1" -> 0xFFAA00;  // Оранжевый
            case "HT2" -> 0xFFFF55;  // Жёлтый
            case "LT2" -> 0x55FF55;  // Зелёный
            case "HT3" -> 0x55FFFF;  // Голубой
            case "LT3" -> 0x5555FF;  // Синий
            case "HT4" -> 0xAA00FF;  // Фиолетовый
            case "LT4" -> 0xFF55FF;  // Розовый
            case "HT5" -> 0xAAAAAA;  // Серый
            case "LT5" -> 0xFFFFFF;  // Белый
            default -> 0xD3D3D3;     // Светло-серый по умолчанию
        };
    }
}
