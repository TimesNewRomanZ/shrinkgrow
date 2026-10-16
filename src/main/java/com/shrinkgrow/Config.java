package com.shrinkgrow;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static float minScale = 0.15F;
    public static float maxScale = 3.0F;
    public static float step = 0.05F;
    public static float coarseStep = 0.25F;

    public static boolean scaleStepHeight = true;
    public static boolean scaleReach = false;
    public static boolean requireOpToScaleOthers = true;

    /** Fine tuning knob for the first-person camera; see SizeApplier. */
    public static float cameraFudge = 0.0F;
    public static boolean enableCameraShift = true;

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();

        minScale = cfg.getFloat(
            "minScale",
            "scale",
            0.15F,
            0.05F,
            1.0F,
            "Smallest scale a player may shrink to. 1.0 = normal height (1.8 blocks).");
        maxScale = cfg.getFloat("maxScale", "scale", 3.0F, 1.0F, 10.0F, "Largest scale a player may grow to.");
        step = cfg.getFloat("step", "scale", 0.05F, 0.01F, 1.0F, "Scale change per keypress.");
        coarseStep = cfg
            .getFloat("coarseStep", "scale", 0.25F, 0.01F, 2.0F, "Scale change per keypress while holding sneak.");

        scaleStepHeight = cfg.getBoolean(
            "scaleStepHeight",
            "gameplay",
            true,
            "Scale step height with the player, so small players cannot step up full blocks.");
        scaleReach = cfg.getBoolean(
            "scaleReach",
            "gameplay",
            false,
            "Scale block reach distance with the player. Off by default: shrinking is meant to help "
                + "with maintenance, and short reach makes that annoying.");
        requireOpToScaleOthers = cfg.getBoolean(
            "requireOpToScaleOthers",
            "gameplay",
            true,
            "Require permission level 2 to run /scale on another player.");

        enableCameraShift = cfg.getBoolean(
            "enableCameraShift",
            "client",
            true,
            "Lower the first-person camera to match your size. Turn OFF to diagnose aiming problems: "
                + "with it off the camera stays at full height while small, which looks wrong but proves "
                + "whether the camera shift is what is throwing the interaction ray off.");

        cameraFudge = cfg.getFloat(
            "cameraFudge",
            "client",
            0.0F,
            -2.0F,
            2.0F,
            "Additive tweak to first-person camera height, in blocks, scaled by player size. "
                + "Leave at 0 unless the camera sits wrong for you.");

        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    public static float clamp(float value) {
        if (Float.isNaN(value)) return 1.0F;
        if (value < minScale) return minScale;
        if (value > maxScale) return maxScale;
        return value;
    }
}
