package com.shrinkgrow.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.shrinkgrow.ScaleManager;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client -> server: "I pressed a key; please set my scale to X". Server clamps. */
public class MsgSetScale implements IMessage {

    public float scale;

    public MsgSetScale() {}

    public MsgSetScale(float scale) {
        this.scale = scale;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        scale = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(scale);
    }

    public static class Handler implements IMessageHandler<MsgSetScale, IMessage> {

        @Override
        public IMessage onMessage(MsgSetScale msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null) {
                ScaleManager.setScale(player, msg.scale);
            }
            return null;
        }
    }
}
