package com.example.diamondflash.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DiamondFlashClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("diamondflash", "main"));

    private static KeyMapping spawnKey;

    @Override
    public void onInitializeClient() {
        // Default key: ' (apostrophe). Can be changed in Options > Controls.
        spawnKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.diamondflash.spawn",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spawnKey.consumeClick()) {
                if (client.player != null) {
                    client.player.connection.sendCommand("diamondflash");
                }
            }
        });
    }
}
