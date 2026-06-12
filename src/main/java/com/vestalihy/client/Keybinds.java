package com.vestalihy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final String CATEGORY_VESTALIHY = "key.category.vestalihy";

    public static final KeyMapping THERMAL_VISION_KEY = new KeyMapping(
            "key.vestalihy.thermal_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY_VESTALIHY
    );
}
