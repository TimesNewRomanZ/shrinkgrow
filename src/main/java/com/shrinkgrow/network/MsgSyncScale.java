package com.shrinkgrow.network;

import com.shrinkgrow.ShrinkGrow;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server -> client: "this player's scale is now X". */
public class MsgSyncScale implements IMessage {

    public int entityId;
    public float scale;

    public MsgSyncScale() {}

    public MsgSyncScale(int entityId, float scale) {
        this.entityId = entityId;
        this.scale = scale;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        scale = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(scale);
    }

    public static class Handler implements IMessageHandler<MsgSyncScale, IMessage> {

        @Override
        public IMessage onMessage(MsgSyncScale msg, MessageContext ctx) {
            // Routed through the proxy so a dedicated server never touches client classes.
            // Only writes a float; the per-tick handler does the entity mutation on the main thread.
            ShrinkGrow.proxy.handleScaleSync(msg.entityId, msg.scale);
            return null;
        }
    }
}
