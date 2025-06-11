package com.Tajra.backPex;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BackPexInventoryHolder implements InventoryHolder {

    private final String backPexUuid;
    private final @Nullable ItemStack backPexItemStack;
    private final @Nullable Location blockLocation;


    public BackPexInventoryHolder(String backPexUuid, ItemStack backPexItemStack) {
        this.backPexUuid = backPexUuid;
        this.backPexItemStack = backPexItemStack;
        this.blockLocation = null;
    }

    public BackPexInventoryHolder(String backPexUuid, Location blockLocation) {
        this.backPexUuid = backPexUuid;
        this.backPexItemStack = null;
        this.blockLocation = blockLocation;
    }

    public String getBackPexUuid() {
        return backPexUuid;
    }

    @Nullable
    public ItemStack getBackPexItemStack() {
        return backPexItemStack;
    }

    @Nullable
    public Location getBlockLocation() {
        return blockLocation;
    }

    public boolean isItemSource() {
        return this.backPexItemStack != null;
    }

    public boolean isBlockSource() {
        return this.blockLocation != null;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}