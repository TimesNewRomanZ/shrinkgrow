package com.shrinkgrow;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Handlers that live on the FML bus. */
public class FmlEventHandler {

    /**
     * Re-assert size every tick. Vanilla and other mods (Tinkers', Ars Magica 2)
     * both reset player size and eye height, so a one-shot apply loses the race.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player == null) return;

        EntityPlayer player = event.player;
        ScaleProps props = ScaleProps.get(player);
        if (props == null) return;

        float scale = props.getScale();
        if (player.width != SizeApplier.BASE_WIDTH * scale || player.height != SizeApplier.BASE_HEIGHT * scale
            || props.appliedScale != scale) {
            SizeApplier.apply(player, scale);
            props.appliedScale = scale;
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ScaleManager.syncToAll(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ScaleManager.syncToAll(event.player);
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ScaleManager.syncToAll(event.player);
    }
}
