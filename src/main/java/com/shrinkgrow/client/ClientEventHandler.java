package com.shrinkgrow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderPlayerEvent;

import org.lwjgl.opengl.GL11;

import com.shrinkgrow.Config;
import com.shrinkgrow.ScaleManager;
import com.shrinkgrow.ScaleProps;
import com.shrinkgrow.network.MsgSetScale;
import com.shrinkgrow.network.PacketHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ClientEventHandler {

    /** Guards the matrix stack: RenderPlayerEvent.Post does not fire if Pre was cancelled. */
    private boolean pushedMatrix;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            ClientProxy.PENDING_IDS.clear();
            ClientProxy.PENDING_SCALES.clear();
            return;
        }

        int[] id;
        while ((id = ClientProxy.PENDING_IDS.poll()) != null) {
            Float scale = ClientProxy.PENDING_SCALES.poll();
            if (scale == null) break;

            Entity entity = world.getEntityByID(id[0]);
            if (entity instanceof EntityPlayer) {
                ScaleProps props = ScaleProps.get((EntityPlayer) entity);
                if (props != null) {
                    props.setScale(scale.floatValue());
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        float delta = mc.thePlayer.isSneaking() ? Config.coarseStep : Config.step;

        if (ClientProxy.keyShrink.isPressed()) {
            request(-delta);
        } else if (ClientProxy.keyGrow.isPressed()) {
            request(delta);
        } else if (ClientProxy.keyReset.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new MsgSetScale(1.0F));
        }
    }

    private void request(float delta) {
        Minecraft mc = Minecraft.getMinecraft();
        float current = ScaleManager.getScale(mc.thePlayer);
        PacketHandler.INSTANCE.sendToServer(new MsgSetScale(Config.clamp(current + delta)));
    }

    /**
     * RenderPlayerEvent.Pre fires as the very first statement of
     * RenderPlayer.doRender, before any GL translation. A bare glScalef here
     * would therefore scale the world-space translate that vanilla applies
     * afterwards, flinging the model away from its feet.
     *
     * Vanilla's translate is (x, y - yOffset, z) using RenderManager's
     * interpolated position. Given we emit translate(A) then scale(s), and
     * vanilla then emits translate(T), a vertex ends up at
     *
     * A + s*T (scaled about the feet)
     *
     * so to land on translate(T') * scale(s) we need A = T' - s*T. That is what
     * is computed below; T' also folds in the camera's posY shift so the local
     * player's body stays on the ground in third person.
     */
    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.entityPlayer;
        float scale = ScaleManager.getScale(player);
        if (scale == 1.0F) return;

        float partial = event.partialRenderTick;
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial - RenderManager.renderPosX;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial - RenderManager.renderPosY;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial - RenderManager.renderPosZ;

        // RenderPlayer.doRender renders at y - yOffset, i.e. from the feet.
        double feet = y - player.yOffset;

        // CameraHandler drags posY down while rendering; undo that for our own body.
        double shift = (CameraHandler.shifted && player == Minecraft.getMinecraft().thePlayer) ? CameraHandler.posYShift
            : 0.0D;

        GL11.glPushMatrix();
        GL11.glTranslated(x * (1.0D - scale), feet * (1.0D - scale) + shift, z * (1.0D - scale));
        GL11.glScalef(scale, scale, scale);
        pushedMatrix = true;
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!pushedMatrix) return;
        GL11.glPopMatrix();
        pushedMatrix = false;
    }
}
