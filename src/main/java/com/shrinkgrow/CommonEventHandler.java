package com.shrinkgrow;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Handlers that live on MinecraftForge.EVENT_BUS. */
public class CommonEventHandler {

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(ScaleProps.ID) == null) {
            event.entity.registerExtendedProperties(ScaleProps.ID, new ScaleProps());
        }
    }

    /** IEEP is not carried across death/dimension change by default; copy it manually. */
    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        ScaleProps oldProps = ScaleProps.get(event.original);
        ScaleProps newProps = ScaleProps.get(event.entityPlayer);
        if (oldProps == null || newProps == null) return;

        NBTTagCompound tag = new NBTTagCompound();
        oldProps.saveNBTData(tag);
        newProps.loadNBTData(tag);
    }

    /** A client that starts seeing a player needs that player's scale. */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayer && event.entityPlayer instanceof EntityPlayerMP) {
            ScaleManager.syncTo((EntityPlayer) event.target, (EntityPlayerMP) event.entityPlayer);
        }
    }
}
