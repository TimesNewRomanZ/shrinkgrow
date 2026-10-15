package com.shrinkgrow.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.shrinkgrow.ShrinkGrow;

/** Opens the size screen. All the actual work happens server-side via ScaleManager. */
public class ItemSizeRemote extends Item {

    public ItemSizeRemote() {
        setMaxStackSize(1);
        setUnlocalizedName("shrinkgrow.size_remote");
        setTextureName("shrinkgrow:size_remote");
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            ShrinkGrow.proxy.openSizeGui();
        }
        return stack;
    }
}
