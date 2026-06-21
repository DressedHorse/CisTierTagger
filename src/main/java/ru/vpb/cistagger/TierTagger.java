package ru.vpb.cistagger;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class TierTagger {
    public static void onInitialize() {
        updateTiers();
    }

    public static void updateTiers() {
        HttpClient client = HttpClient.newHttpClient();

        for (Gamemode gamemode : Gamemode.values()) {
            if (gamemode == Gamemode.NONE) continue;

            loadTiers(client, gamemode.getName(), gamemode.getTierMaps());
        }
    }

    private static void loadTiers(HttpClient client, String kit, Map<String, String> targetMap) {
        String url = "https://cistiers.com/api/table/" + kit;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate, br, zstd") // <- Этот заголовок вызывает сжатие
                .header("Accept-Language", "ru,en-US;q=0.9,en;q=0.8,nl;q=0.7,de;q=0.6,es;q=0.5")
                .header("Cache-Control", "max-age=0")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 OPR/122.0.0.0")
                .build();

        try {

            // Получаем ответ в виде байтов, чтобы вручную распаковать
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                return;
            }

            byte[] bodyBytes = response.body();
            if (bodyBytes == null || bodyBytes.length == 0) {
                return;
            }

            // Определяем, сжато ли содержимое
            String contentEncoding = response.headers().firstValue("Content-Encoding").orElse("");
            String jsonString;

            if (contentEncoding.contains("gzip")) {
                // Распаковываем gzip
                try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(bodyBytes));
                     InputStreamReader reader = new InputStreamReader(gzipStream, java.nio.charset.StandardCharsets.UTF_8)) {

                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int length;
                    while ((length = reader.read(buffer)) > 0) {
                        sb.append(buffer, 0, length);
                    }
                    jsonString = sb.toString();
                }
            } else if (contentEncoding.contains("deflate")) {
                // Распаковываем deflate (редко, но на всякий случай)
                try (java.util.zip.InflaterInputStream inflater = new java.util.zip.InflaterInputStream(
                        new ByteArrayInputStream(bodyBytes));
                     InputStreamReader reader = new InputStreamReader(inflater, java.nio.charset.StandardCharsets.UTF_8)) {

                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[8192];
                    int length;
                    while ((length = reader.read(buffer)) > 0) {
                        sb.append(buffer, 0, length);
                    }
                    jsonString = sb.toString();
                }
            } else {
                // Не сжато — читаем как строку
                jsonString = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            }

            // Удаляем BOM и невидимые символы
            String cleanResponse = jsonString.trim()
                    .replace("\uFEFF", "")
                    .replaceAll("^[\\p{Cntrl}&&[^\r\n\t]]+", "");

            // Логируем первые 200 символов для отладки
            String preview = cleanResponse.length() > 200 ? cleanResponse.substring(0, 200) + "..." : cleanResponse;

            // Проверяем, что это JSON
            if (!cleanResponse.startsWith("{") && !cleanResponse.startsWith("[")) {
                return;
            }

            // Парсим JSON
            JsonElement parsed;
            try {
                JsonReader reader = new JsonReader(new java.io.StringReader(cleanResponse));
                reader.setLenient(true);
                parsed = JsonParser.parseReader(reader);
            } catch (Exception e) {
                throw e;
            }

            if (!parsed.isJsonObject()) {
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            int totalEntries = 0;

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String tierKey = entry.getKey();
                JsonElement tierValue = entry.getValue();

                if (!tierValue.isJsonArray()) {
                    continue;
                }

                String tier = tierKey.toUpperCase();

                for (JsonElement playerElement : tierValue.getAsJsonArray()) {
                    if (!playerElement.isJsonObject()) {
                        continue;
                    }

                    JsonObject player = playerElement.getAsJsonObject();

                    if (!player.has("nickname") || player.get("nickname").isJsonNull()) {
                        continue;
                    }

                    String nickname = player.get("nickname").getAsString();
                    if (nickname == null || nickname.trim().isEmpty()) {
                        continue;
                    }

                    targetMap.put(nickname, tier);
                    totalEntries++;
                }
            }

            int finalTotalEntries = totalEntries;

        } catch (Exception e) {
            System.err.println("Ошибка в loadTiers для kit=" + kit + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Text> getTiersByNickname(String nickname) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("https://cistiers.com/api/profile/" + nickname))
                .GET()
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ru,en-US;q=0.9,en;q=0.8,nl;q=0.7,de;q=0.6,es;q=0.5")
                .header("Cache-Control", "max-age=0")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 OPR/122.0.0.0")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() != 200) {
                            System.err.println("Profile API error: " + response.statusCode());
                            return noTiersText(nickname);
                        }

                        byte[] bodyBytes = response.body();
                        if (bodyBytes == null || bodyBytes.length == 0) {
                            return noTiersText(nickname);
                        }

                        String encoding = response.headers().firstValue("Content-Encoding").orElse("");
                        String jsonString;

                        if (encoding.contains("gzip")) {
                            try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(bodyBytes));
                                 InputStreamReader reader = new InputStreamReader(gzipStream, StandardCharsets.UTF_8)) {

                                StringBuilder sb = new StringBuilder();
                                char[] buffer = new char[8192];
                                int length;
                                while ((length = reader.read(buffer)) > 0) {
                                    sb.append(buffer, 0, length);
                                }
                                jsonString = sb.toString();
                            }
                        } else if (encoding.contains("deflate")) {
                            try (java.util.zip.InflaterInputStream inflater = new java.util.zip.InflaterInputStream(
                                    new ByteArrayInputStream(bodyBytes));
                                 InputStreamReader reader = new InputStreamReader(inflater, StandardCharsets.UTF_8)) {

                                StringBuilder sb = new StringBuilder();
                                char[] buffer = new char[8192];
                                int length;
                                while ((length = reader.read(buffer)) > 0) {
                                    sb.append(buffer, 0, length);
                                }
                                jsonString = sb.toString();
                            }
                        } else {
                            jsonString = new String(bodyBytes, StandardCharsets.UTF_8);
                        }

                        String cleanResponse = jsonString.trim()
                                .replace("\uFEFF", "")
                                .replaceAll("^[\\p{Cntrl}&&[^\r\n\t]]+", "");

//                        System.out.println("Profile API response for " + nickname + ": " + cleanResponse);

                        JsonElement parsed;
                        try {
                            JsonReader reader = new JsonReader(new java.io.StringReader(cleanResponse));
                            reader.setLenient(true);
                            parsed = JsonParser.parseReader(reader);
                        } catch (Exception e) {
                            System.err.println("JSON parse error for " + nickname + ": " + e.getMessage());
                            return noTiersText(nickname);
                        }

                        if (!parsed.isJsonObject()) {
                            return noTiersText(nickname);
                        }

                        JsonObject json = parsed.getAsJsonObject();

                        if (!json.has("tier_stats") || json.get("tier_stats").isJsonNull()) {
                            return noTiersText(nickname);
                        }

                        JsonObject tierStats = json.getAsJsonObject("tier_stats");
                        JsonElement currentTiersElement = tierStats.get("current_tiers");

                        if (currentTiersElement == null || !currentTiersElement.isJsonArray()) {
                            return noTiersText(nickname);
                        }

                        MutableText sb = Text.literal(nickname).formatted(Formatting.GRAY);
                        Map<String, String> tiers = new LinkedHashMap<>();

                        for (JsonElement element : currentTiersElement.getAsJsonArray()) {
                            if (!element.isJsonObject()) continue;
                            JsonObject tierObj = element.getAsJsonObject();

                            String kit = tierObj.has("kit") && !tierObj.get("kit").isJsonNull()
                                    ? tierObj.get("kit").getAsString() : null;
                            String tier = tierObj.has("tier") && !tierObj.get("tier").isJsonNull()
                                    ? tierObj.get("tier").getAsString() : null;

                            if (kit != null && tier != null) {
                                String formattedKit = kit.substring(0, 1).toUpperCase() + kit.substring(1);
                                tiers.put(formattedKit, tier);
                            }
                        }

                        if (tiers.isEmpty()) {
                            return noTiersText(nickname);
                        }

                        for (Map.Entry<String, String> entry : tiers.entrySet()) {
                            String tierName = entry.getValue();
                            int color = getTierColor(tierName);
                            sb.append(Text.literal(" | ").formatted(Formatting.WHITE)
                                    .append(Text.literal(entry.getKey() + ": ").formatted(Formatting.DARK_GRAY)));
                            sb.append(Text.literal(tierName.toUpperCase()).styled(style -> style.withColor(color)));
                        }

                        return sb;

                    } catch (Exception e) {
                        System.err.println("Error processing profile for " + nickname + ": " + e.getMessage());
                        e.printStackTrace();
                        return noTiersText(nickname);
                    }
                });
    }

    // Вспомогательный метод для единообразного текста "нет тиров"
    private static Text noTiersText(String nickname) {
        return Text.literal(nickname).formatted(Formatting.GRAY)
                .append(Text.literal(" | ").formatted(Formatting.WHITE))
                .append(Text.literal("No tiers found").formatted(Formatting.RED));
    }

    public static Text appendTier(Text playerName, Text baseText) {
        MutableText tierText = getPlayerTier(Formatting.strip(playerName.getString()));

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
        Gamemode currentGameMode = Gamemode.getCurrent();
        String foundTier = currentGameMode.getTierMaps().get(username);

        if (foundTier == null)
            return null;

        int color = getTierColor(foundTier);
        return Text.literal(currentGameMode.getTextureCode())
                .append(Text.literal(foundTier).styled(style -> style.withColor(color)));
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
