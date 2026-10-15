package com.shrinkgrow;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**
 * Per-player persistent scale. 1.7.10 has no capability system, so we use
 * Forge's IExtendedEntityProperties instead.
 */
public class ScaleProps implements IExtendedEntityProperties {

    public static final String ID = "shrinkgrow_scale";

    /** Authoritative on the server; mirrored on clients via MsgSyncScale. */
    private float scale = 1.0F;

    /** Last scale we actually pushed onto the entity, so we only touch it on change. */
    public float appliedScale = Float.NaN;

    public static ScaleProps get(EntityPlayer player) {
        return (ScaleProps) player.getExtendedProperties(ID);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float value) {
        this.scale = Config.clamp(value);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("scale", scale);
        compound.setTag(ID, tag);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        NBTTagCompound tag = compound.getCompoundTag(ID);
        if (tag.hasKey("scale")) {
            scale = Config.clamp(tag.getFloat("scale"));
        }
    }

    @Override
    public void init(Entity entity, World world) {
        // nothing to do
    }
}
