package com.shrinkgrow;

import net.minecraft.item.Item;

import com.shrinkgrow.item.ItemSizeRemote;
import com.shrinkgrow.network.PacketHandler;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = ShrinkGrow.MODID,
    name = ShrinkGrow.MODNAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]")
public class ShrinkGrow {

    public static final String MODID = "shrinkgrow";
    public static final String MODNAME = "Shrink & Grow";

    @Mod.Instance(MODID)
    public static ShrinkGrow instance;

    @SidedProxy(clientSide = "com.shrinkgrow.client.ClientProxy", serverSide = "com.shrinkgrow.CommonProxy")
    public static CommonProxy proxy;

    public static Item sizeRemote;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        PacketHandler.init();

        sizeRemote = new ItemSizeRemote();
        GameRegistry.registerItem(sizeRemote, "size_remote");
        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandScale());
    }
}
