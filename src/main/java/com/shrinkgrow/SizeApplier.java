package com.shrinkgrow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

/**
 * The one place that mutates vanilla player size state.
 *
 * Verified against the decompiled 1.7.10 sources:
 *
 * Entity.setPosition() minY = posY - yOffset + ySize, so feet = posY - yOffset
 * EntityPlayer yOffset = 1.62F, getDefaultEyeHeight() = 0.12F
 * orientCamera() camera Y = posY - (yOffset - 1.62F), i.e. always 1.62
 * above the feet, and completely ignores eyeHeight
 * isEntityInsideOpaqueBlock() probes posY + getEyeHeight()
 *
 * So eyeHeight controls the suffocation probe and ray-trace origin, while the
 * camera has to be moved separately - see CameraHandler.
 */
public final class SizeApplier {

    public static final float BASE_WIDTH = 0.6F;
    public static final float BASE_HEIGHT = 1.8F;
    public static final float BASE_STEP_HEIGHT = 0.5F;

    /** Hardcoded in EntityRenderer.orientCamera. Camera height above the feet. */
    public static final float VANILLA_CAMERA_HEIGHT = 1.62F;

    private SizeApplier() {}

    public static void apply(EntityPlayer player, float scale) {
        setSize(player, BASE_WIDTH * scale, BASE_HEIGHT * scale);
        player.eyeHeight = eyeHeightField(player, scale);

        if (Config.scaleStepHeight) {
            player.stepHeight = BASE_STEP_HEIGHT * scale;
        }
    }

    public static void reset(EntityPlayer player) {
        setSize(player, BASE_WIDTH, BASE_HEIGHT);
        player.eyeHeight = player.getDefaultEyeHeight();
        player.stepHeight = BASE_STEP_HEIGHT;
    }

    /**
     * Vanilla probes suffocation at posY + eyeHeight, which by default sits
     * yOffset + 0.12 above the feet. Scale that whole distance, then express it
     * back as an eyeHeight offset.
     */
    /**
     * Eye height must NOT be derived from yOffset: it is 1.62 on the client and
     * 0 on the server (EntityPlayerMP zeroes it), so any formula involving it
     * yields a different answer per side. Worse, vanilla already compensates for
     * that split itself - Item.getMovingObjectPositionFromPlayer uses
     *
     * client: posY + (getEyeHeight() - getDefaultEyeHeight())
     * server: posY + getEyeHeight()
     *
     * so a yOffset-based value makes the two errors compound instead of cancel.
     * An earlier version used (yOffset + defaultEyeHeight) * scale - yOffset,
     * which is correct only on the server; on the client it placed the ray
     * origin 1.62 * (scale - 1) blocks off - about 1.2 blocks below the eyes at
     * quarter size, often underground. Vanilla block targeting still looked fine
     * because getMouseOver() runs inside the render tick, where CameraHandler
     * has already parked eyeHeight at its default; it was only mods calling
     * getMovingObjectPositionFromPlayer outside that window that broke.
     *
     * Scaling the default directly satisfies both sides' formulas at once, and
     * is identity at scale 1.
     *
     * Note the suffocation probe (posY + getEyeHeight()) stays correct on the
     * server, which is what applies the damage; the client's copy of that probe
     * is off by yOffset, but it only drives prediction.
     */
    public static float eyeHeightField(EntityPlayer player, float scale) {
        return player.getDefaultEyeHeight() * scale;
    }

    /**
     * Entity.setSize is protected, so reproduce it here.
     *
     * Vanilla treats the bounding box as authoritative and *derives* position
     * from it - moveEntity() does posY = boundingBox.minY + yOffset - ySize.
     * So the box bottom is anchored and the box grows or shrinks upward, which
     * keeps the feet planted. Deliberately NOT setPosition(), which inverts that
     * dependency and rebuilds minY from posY, relocating the whole box whenever
     * posY is momentarily out of step with it.
     *
     * Unlike vanilla this also re-centres in X/Z; vanilla anchors minX/minZ and
     * lets the box drift off-centre as the width changes.
     */
    private static void setSize(EntityPlayer player, float width, float height) {
        if (width == player.width && height == player.height) return;

        AxisAlignedBB box = player.boundingBox;
        double centreX = (box.minX + box.maxX) / 2.0D;
        double centreZ = (box.minZ + box.maxZ) / 2.0D;
        double half = width / 2.0D;

        player.width = width;
        player.height = height;

        box.minX = centreX - half;
        box.maxX = centreX + half;
        box.minZ = centreZ - half;
        box.maxZ = centreZ + half;
        box.maxY = box.minY + height;

        player.myEntitySize = entitySizeFor(width);
    }

    private static Entity.EnumEntitySize entitySizeFor(float width) {
        float f = width % 2.0F;
        if (f < 0.375F) return Entity.EnumEntitySize.SIZE_1;
        if (f < 0.75F) return Entity.EnumEntitySize.SIZE_2;
        if (f < 1.0F) return Entity.EnumEntitySize.SIZE_3;
        if (f < 1.375F) return Entity.EnumEntitySize.SIZE_4;
        if (f < 1.75F) return Entity.EnumEntitySize.SIZE_5;
        return Entity.EnumEntitySize.SIZE_6;
    }
}
