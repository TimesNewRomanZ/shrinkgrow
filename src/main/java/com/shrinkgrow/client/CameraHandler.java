package com.shrinkgrow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.shrinkgrow.Config;
import com.shrinkgrow.ScaleManager;
import com.shrinkgrow.SizeApplier;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Moves the first-person camera to match the player's size.
 *
 * EntityRenderer.orientCamera computes the camera as
 * posY - (yOffset - 1.62F), so with the player's stock yOffset of 1.62 the
 * camera sits exactly at posY - and writing eyeHeight does nothing to it.
 * The only lever is posY itself.
 *
 * So, as Morph does on this version: shift posY for the duration of the render
 * tick and put it back afterwards. Everything outside rendering keeps seeing
 * untouched values, which is what stops this from desyncing movement.
 *
 * eyeHeight is also parked at its default during the shift, so that Forge's
 * getPosition() - posY + (eyeHeight - defaultEyeHeight) - lands on the camera
 * too, keeping the mouseover ray aligned with what you see.
 */
public class CameraHandler {

    /** How far posY was shifted down this frame. Read by the player renderer. */
    public static float posYShift;
    public static boolean shifted;

    private float savedEyeHeight;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (mc.theWorld == null || player == null) return;

        if (event.phase == TickEvent.Phase.START) {
            if (shifted) return;

            float scale = ScaleManager.getScale(player);
            if (scale == 1.0F) {
                posYShift = 0.0F;
                return;
            }

            float target = (SizeApplier.VANILLA_CAMERA_HEIGHT + Config.cameraFudge) * scale;
            posYShift = SizeApplier.VANILLA_CAMERA_HEIGHT - target;

            savedEyeHeight = player.eyeHeight;
            player.eyeHeight = player.getDefaultEyeHeight();
            player.posY -= posYShift;
            player.prevPosY -= posYShift;
            player.lastTickPosY -= posYShift;
            shifted = true;
        } else {
            if (!shifted) return;

            player.posY += posYShift;
            player.prevPosY += posYShift;
            player.lastTickPosY += posYShift;
            player.eyeHeight = savedEyeHeight;
            shifted = false;
        }
    }
}
