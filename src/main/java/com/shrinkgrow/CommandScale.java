package com.shrinkgrow;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class CommandScale extends CommandBase {

    @Override
    public String getCommandName() {
        return "scale";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/scale <number|reset|info> [player]";
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
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        EntityPlayer target;
        if (args.length >= 2) {
            if (Config.requireOpToScaleOthers && !sender.canCommandSenderUseCommand(2, getCommandName())) {
                sender.addChatMessage(new ChatComponentText("You may only scale yourself."));
                return;
            }
            target = getPlayer(sender, args[1]);
        } else {
            target = getCommandSenderAsPlayer(sender);
        }

        if ("info".equalsIgnoreCase(args[0])) {
            printInfo(sender, target);
            return;
        }

        float scale;
        if ("reset".equalsIgnoreCase(args[0])) {
            scale = 1.0F;
        } else {
            try {
                scale = Float.parseFloat(args[0]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
        }

        float clamped = Config.clamp(scale);
        ScaleManager.setScale(target, clamped);
        sender.addChatMessage(
            new ChatComponentText(String.format("Set %s's scale to %.2f", target.getCommandSenderName(), clamped)));
    }

    /** Dumps the raw geometry so size bugs can be read off numbers, not screenshots. */
    private void printInfo(ICommandSender sender, EntityPlayer p) {
        float scale = ScaleManager.getScale(p);
        double feet = p.boundingBox.minY;
        double expectedFeet = p.posY - p.yOffset + p.ySize;

        sender.addChatMessage(
            new ChatComponentText(
                String.format(
                    "scale=%.3f  w=%.3f (want %.3f)  h=%.3f (want %.3f)",
                    scale,
                    p.width,
                    SizeApplier.BASE_WIDTH * scale,
                    p.height,
                    SizeApplier.BASE_HEIGHT * scale)));
        sender.addChatMessage(
            new ChatComponentText(
                String.format(
                    "posY=%.3f  yOffset=%.3f  ySize=%.3f  eyeHeight=%.3f (want %.3f)",
                    p.posY,
                    p.yOffset,
                    p.ySize,
                    p.eyeHeight,
                    SizeApplier.eyeHeightField(p, scale))));
        sender.addChatMessage(
            new ChatComponentText(
                String.format(
                    "box Y: %.3f -> %.3f   feet=%.3f  derived=%.3f  drift=%.3f",
                    p.boundingBox.minY,
                    p.boundingBox.maxY,
                    feet,
                    expectedFeet,
                    feet - expectedFeet)));
        sender.addChatMessage(
            new ChatComponentText(
                String.format(
                    "box XZ: %.3f wide, %.3f deep",
                    p.boundingBox.maxX - p.boundingBox.minX,
                    p.boundingBox.maxZ - p.boundingBox.minZ)));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<String>();
            out.add("reset");
            out.add("info");
            out.add("0.25");
            out.add("0.5");
            out.add("1.0");
            out.add("2.0");
            return out;
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, MinecraftServerHelper.onlinePlayerNames());
        }
        return null;
    }

    /** Small indirection so the command class stays readable. */
    static final class MinecraftServerHelper {

        static String[] onlinePlayerNames() {
            return net.minecraft.server.MinecraftServer.getServer()
                .getAllUsernames();
        }
    }
}
