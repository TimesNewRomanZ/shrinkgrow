package com.shrinkgrow.client;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.shrinkgrow.CommonProxy;
import com.shrinkgrow.client.gui.GuiSizeRemote;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;

public class ClientProxy extends CommonProxy {

    public static KeyBinding keyShrink;
    public static KeyBinding keyGrow;
    public static KeyBinding keyReset;

    /**
     * Packets arrive on a Netty thread. Rather than touch the world from there,
     * queue the update and drain it on the client tick.
     */
    public static final ConcurrentLinkedQueue<int[]> PENDING_IDS = new ConcurrentLinkedQueue<int[]>();
    public static final ConcurrentLinkedQueue<Float> PENDING_SCALES = new ConcurrentLinkedQueue<Float>();

    @Override
    public void preInit() {
        super.preInit();

        keyShrink = new KeyBinding("key.shrinkgrow.shrink", Keyboard.KEY_LBRACKET, "key.categories.shrinkgrow");
        keyGrow = new KeyBinding("key.shrinkgrow.grow", Keyboard.KEY_RBRACKET, "key.categories.shrinkgrow");
        keyReset = new KeyBinding("key.shrinkgrow.reset", Keyboard.KEY_BACKSLASH, "key.categories.shrinkgrow");
        ClientRegistry.registerKeyBinding(keyShrink);
        ClientRegistry.registerKeyBinding(keyGrow);
        ClientRegistry.registerKeyBinding(keyReset);

        ClientEventHandler handler = new ClientEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);

        FMLCommonHandler.instance()
            .bus()
            .register(new CameraHandler());
    }

    @Override
    public void handleScaleSync(int entityId, float scale) {
        PENDING_IDS.add(new int[] { entityId });
        PENDING_SCALES.add(Float.valueOf(scale));
    }

    @Override
    public void openSizeGui() {
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiSizeRemote());
    }
}
