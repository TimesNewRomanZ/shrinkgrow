package com.shrinkgrow.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;

import com.shrinkgrow.ScaleManager;
import com.shrinkgrow.SizeApplier;

/**
 * Client-side twin of "/scale info".
 *
 * The server and client disagree about yOffset (EntityPlayerMP zeroes it), and
 * different mods build their ray origin from different combinations of posY,
 * eyeHeight and getDefaultEyeHeight(). This prints every variant side by side
 * so the correct one can be read off rather than guessed at.
 *
 * Registered through Forge's ClientCommandHandler, so it never reaches the
 * server and works in singleplayer and multiplayer alike.
 */
public class CommandScaleClient extends CommandBase {

    @Override
    public String getCommandName() {
        return "scaleclient";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/scaleclient";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return new ArrayList<String>();
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null) return;

        float scale = ScaleManager.getScale(p);
        float g = p.getDefaultEyeHeight();
        float e = p.eyeHeight;
        float y = p.yOffset;
        double feet = p.boundingBox.minY;

        say(sender, String.format("CLIENT scale=%.3f  yOffset=%.3f  defaultEyeHeight=%.3f", scale, y, g));
        say(sender, String.format("  posY=%.3f  eyeHeight=%.3f  feet(box.minY)=%.3f", p.posY, e, feet));
        say(
            sender,
            String.format("  camShift: shifted=%s posYShift=%.3f", CameraHandler.shifted, CameraHandler.posYShift));

        // Where the eye SHOULD be, relative to the feet, at this scale.
        double want = feet + SizeApplier.VANILLA_CAMERA_HEIGHT * scale;

        // orientCamera: posY - (yOffset - 1.62F). This command runs OUTSIDE the
        // render tick, where CameraHandler has not shifted posY yet, so subtract
        // the pending shift to get the camera height that rendering will use.
        double cameraRaw = p.posY - (y - SizeApplier.VANILLA_CAMERA_HEIGHT);
        double camera = cameraRaw - (CameraHandler.shifted ? 0.0F : CameraHandler.posYShift);
        // vanilla Item.getMovingObjectPositionFromPlayer, client branch
        double vanillaRay = p.posY + (e - g);
        // MMUtils.getHitResult (both sides) and the server branch of getLookingAtLocation
        double mmHit = p.posY + e;

        say(sender, String.format("  want eye      = %.3f  (feet + 1.62 * scale)", want));
        say(sender, String.format("  camera        = %.3f  (off by %+.3f, shift applied)", camera, camera - want));
        say(
            sender,
            String.format(
                "    raw posY-based = %.3f  (before the pending %.3f shift)",
                cameraRaw,
                CameraHandler.posYShift));
        say(sender, String.format("  vanilla ray   = %.3f  (off by %+.3f)", vanillaRay, vanillaRay - want));
        say(sender, String.format("  MM getHitResult = %.3f  (off by %+.3f)", mmHit, mmHit - want));

        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            say(
                sender,
                String.format(
                    "  crosshair block: %d, %d, %d (side %d)",
                    hit.blockX,
                    hit.blockY,
                    hit.blockZ,
                    hit.sideHit));
        } else {
            say(sender, "  crosshair block: none (look at a block)");
        }
    }
}
