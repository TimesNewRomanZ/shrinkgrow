package com.shrinkgrow;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.shrinkgrow.network.MsgSyncScale;
import com.shrinkgrow.network.PacketHandler;

public final class ScaleManager {

    private ScaleManager() {}

    public static float getScale(EntityPlayer player) {
        ScaleProps props = ScaleProps.get(player);
        return props == null ? 1.0F : props.getScale();
    }

    /** Server-authoritative change. Clamps, stores, then tells everyone. */
    public static void setScale(EntityPlayer player, float scale) {
        ScaleProps props = ScaleProps.get(player);
        if (props == null) return;
        props.setScale(scale);
        syncToAll(player);
    }

    /** Broadcast one player's scale to every client. */
    public static void syncToAll(EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        PacketHandler.INSTANCE.sendToAll(new MsgSyncScale(player.getEntityId(), getScale(player)));
    }

    /** Send one player's scale to a single viewer (used when tracking starts). */
    public static void syncTo(EntityPlayer subject, EntityPlayerMP viewer) {
        if (subject.worldObj.isRemote) return;
        PacketHandler.INSTANCE.sendTo(new MsgSyncScale(subject.getEntityId(), getScale(subject)), viewer);
    }
}
