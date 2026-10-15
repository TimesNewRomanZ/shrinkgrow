package com.shrinkgrow.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import com.shrinkgrow.Config;
import com.shrinkgrow.ScaleManager;
import com.shrinkgrow.network.MsgSetScale;
import com.shrinkgrow.network.PacketHandler;

public class GuiSizeRemote extends GuiScreen {

    private static final float FINE = 0.1F;
    private static final float COARSE = 0.5F;

    private static final int ID_MINUS_COARSE = 0;
    private static final int ID_MINUS_FINE = 1;
    private static final int ID_PLUS_FINE = 2;
    private static final int ID_PLUS_COARSE = 3;
    private static final int ID_RESET = 4;
    private static final int ID_DONE = 5;

    /**
     * Tracked locally so rapid clicks each build on the previous one rather than
     * on a server value that has not round-tripped yet. The server clamps with
     * the same rule, so the two agree.
     */
    private float target = 1.0F;

    @Override
    public void initGui() {
        buttonList.clear();

        Minecraft mc = Minecraft.getMinecraft();
        target = mc.thePlayer == null ? 1.0F : ScaleManager.getScale(mc.thePlayer);

        int cx = width / 2;
        int top = height / 2 - 30;

        buttonList.add(new GuiButton(ID_MINUS_COARSE, cx - 110, top, 40, 20, "-" + trim(COARSE)));
        buttonList.add(new GuiButton(ID_MINUS_FINE, cx - 68, top, 40, 20, "-" + trim(FINE)));
        buttonList.add(new GuiButton(ID_PLUS_FINE, cx + 28, top, 40, 20, "+" + trim(FINE)));
        buttonList.add(new GuiButton(ID_PLUS_COARSE, cx + 70, top, 40, 20, "+" + trim(COARSE)));

        buttonList.add(new GuiButton(ID_RESET, cx - 61, top + 30, 60, 20, "Reset"));
        buttonList.add(new GuiButton(ID_DONE, cx + 1, top + 30, 60, 20, "Done"));

        updateButtonStates();
    }

    private static String trim(float value) {
        String s = String.format("%.2f", value);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Grey out a direction once it would do nothing. */
    private void updateButtonStates() {
        for (GuiButton b : buttonList) {
            if (b.id == ID_MINUS_COARSE || b.id == ID_MINUS_FINE) {
                b.enabled = target > Config.minScale;
            } else if (b.id == ID_PLUS_FINE || b.id == ID_PLUS_COARSE) {
                b.enabled = target < Config.maxScale;
            } else if (b.id == ID_RESET) {
                b.enabled = target != 1.0F;
            }
        }
    }

    private void nudge(float delta) {
        float next = Config.clamp(target + delta);
        if (next == target) return;
        target = next;
        PacketHandler.INSTANCE.sendToServer(new MsgSetScale(target));
        updateButtonStates();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;

        switch (button.id) {
            case ID_MINUS_COARSE:
                nudge(-COARSE);
                break;
            case ID_MINUS_FINE:
                nudge(-FINE);
                break;
            case ID_PLUS_FINE:
                nudge(FINE);
                break;
            case ID_PLUS_COARSE:
                nudge(COARSE);
                break;
            case ID_RESET:
                target = 1.0F;
                PacketHandler.INSTANCE.sendToServer(new MsgSetScale(1.0F));
                updateButtonStates();
                break;
            case ID_DONE:
                mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int cx = width / 2;
        int top = height / 2 - 30;

        drawCenteredString(fontRendererObj, "Size Remote", cx, top - 26, 0xFFFFFF);
        drawCenteredString(fontRendererObj, String.format("%.2fx", target), cx, top + 6, 0x55FF55);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
