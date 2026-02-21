package com.campersamu.chatheads;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.text.Text.literal;
import static net.minecraft.text.TextColor.fromRgb;

public class ChatHeads implements ModInitializer {
    public static final String MODID = "chatheads";
    public static final String PLAYER = "player";
    public static final Identifier pixel = Identifier.of(MODID, "pixel");
    public static final Identifier noxel = Identifier.of(MODID, "noxel");

    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static final ChatHeadsConfig CONFIG = ChatHeadsConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", ChatHeads.MODID, ChatHeadsConfig.class);
    public static final TextColor[][] DEFAULT_HEAD_TEXTURE = new TextColor[][]{   //hex 0xC01044 -> TextColor.fromRgb(0xC01044)
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0xffffff), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0xffffff), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
            {fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e), fromRgb(0x2e2e2e)},
    };
    public static final Text DEFAULT_HEAD = paintHead(DEFAULT_HEAD_TEXTURE);
    public static final Map<String, TextColor[][]> HEAD_CACHE = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        //Add Mod Resources to Polymer Resource Pack
        PolymerResourcePackUtils.addModAssets(MODID);

        //Register Placeholder
        Placeholders.register(Identifier.of(MODID, PLAYER), (ctx, arg) -> {
            if (arg == null || arg.isEmpty()) return PlaceholderResult.value(paintHead(getPlayerHead(ctx.player(), false)));
            return PlaceholderResult.value(paintHead(getPlayerHead(arg, true)));
        });
    }

    public static TextColor[][] getPlayerHead(ServerPlayerEntity player) {
        return getPlayerHead(player, true);
    }

    public static TextColor[][] getPlayerHead(ServerPlayerEntity player, boolean compute) {
        if (player == null) return DEFAULT_HEAD_TEXTURE;
        String skinId;
        try {
            skinId = player.getServer().getSessionService().getTextures(player.getGameProfile()).skin().getHash();
        } catch (Exception e) {
            return DEFAULT_HEAD_TEXTURE;
        }
        return getPlayerHead(skinId, compute);
    }

    public static TextColor[][] getPlayerHead(String skinId, boolean compute) {
        return compute ? HEAD_CACHE.computeIfAbsent(skinId, ChatHeads::getPlayerHeadImmediate) : HEAD_CACHE.getOrDefault(skinId, null);
    }

    public static TextColor[][] getPlayerHeadImmediate(String hash) {
        //get skin url
        final String playerSkinUrl = CONFIG.url.value().replace("<id>", hash);

        //pull the picture
        final BufferedImage image;
        try {
            LOGGER.info("[ChatHeads] Grabbing skin from %s".formatted(playerSkinUrl), hash);
            URLConnection conn = URI.create(playerSkinUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "ServerChatHeads/1.0 (+https://github.com/sisby-folk/ServerChatHeads; <sleepingdragoninn@gmail.com>)");
            image = ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            LOGGER.warn("[ChatHeads] Failed to get image for {}", hash);
            LOGGER.warn(e.toString());
            return DEFAULT_HEAD_TEXTURE;
        }

        //generate the head
        final TextColor[][] playerHead = new TextColor[8][8];

        boolean fullSkin = image.getWidth() == 64;

        if (fullSkin) {
            int faceX = 8;
            int faceY = 8;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int rgb = image.getRGB(faceX + x, faceY + y);
                    if (rgb != 0) playerHead[y][x] = fromRgb(rgb & 0xffffff);
                }
            }
            int hatX = 40;
            int hatY = 8;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int rgb = image.getRGB(hatX + x, hatY + y);
                    if (rgb != 0) playerHead[y][x] = fromRgb(rgb & 0xffffff);
                }
            }
        } else {
            // look man we gotta do SOMETHING
            // calculate the non-transparent square bounds
            int startXY = Integer.MAX_VALUE;
            int endXY = Integer.MIN_VALUE;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    if (image.getRGB(x, y) != 0) {
                        startXY = Math.min(startXY, Math.min(x, y));
                        endXY = Math.max(endXY, Math.max(x, y));
                    }
                }
            }
            // grab roughly 8x8 pixels from it
            if (startXY < endXY) {
                float step = (endXY + 1 - startXY) / 8.0F;
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        int sourceX = MathHelper.clamp(startXY + Math.round((x + 0.49F) * step), startXY, endXY);
                        int sourceY = MathHelper.clamp(startXY + Math.round((y + 0.49F) * step), startXY, endXY);
                        int rgb = image.getRGB(sourceX, sourceY);
                        if (rgb != 0) playerHead[y][x] = fromRgb(rgb & 0xffffff);
                    }
                }
            }
        }

        return playerHead;
    }

    public static @NotNull Text paintHead(TextColor[][] head) {
        if (head == null) return Objects.requireNonNull(DEFAULT_HEAD);
        MutableText text = Text.empty();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Identifier font = head[y][x] == null ? noxel : pixel;
                TextColor color = head[y][x];

                text = text
                        .append(literal("" + (char) (((int) '\uF810') + y)).setStyle(Style.EMPTY.withColor(color).withFont(font)))
                        .append(literal("\uE001").fillStyle(Style.EMPTY.withFont(font)));
            }
            text = text.append(literal("\uE008").fillStyle(Style.EMPTY.withFont(pixel)));
        }

        text.append(literal("  "));

        return text;
    }
}
