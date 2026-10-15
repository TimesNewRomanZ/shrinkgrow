package com.shrinkgrow;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;

public class CommonProxy {

    public void preInit() {
        CommonEventHandler forgeHandler = new CommonEventHandler();
        MinecraftForge.EVENT_BUS.register(forgeHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(new FmlEventHandler());
    }

    /** Client-only; no-op on a dedicated server. */
    public void handleScaleSync(int entityId, float scale) {}

    /** Client-only; no-op on a dedicated server. */
    public void openSizeGui() {}
}
