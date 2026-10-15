package com.shrinkgrow.network;

import com.shrinkgrow.ShrinkGrow;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class PacketHandler {

    public static SimpleNetworkWrapper INSTANCE;

    private PacketHandler() {}

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ShrinkGrow.MODID);
        INSTANCE.registerMessage(MsgSyncScale.Handler.class, MsgSyncScale.class, 0, Side.CLIENT);
        INSTANCE.registerMessage(MsgSetScale.Handler.class, MsgSetScale.class, 1, Side.SERVER);
    }
}
