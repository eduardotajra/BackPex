package com.Tajra.backPex;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BackPexUpgradeGuiHolder implements InventoryHolder {

    private final String backPexUuid;
    private final @Nullable ItemStack sourceItemStack;
    private final @Nullable Location sourceBlockLocation;

    public BackPexUpgradeGuiHolder(@NotNull String backPexUuid, @NotNull ItemStack sourceItemStack) {
        this.backPexUuid = backPexUuid;
        this.sourceItemStack = sourceItemStack.clone();
        this.sourceBlockLocation = null;
    }

    public BackPexUpgradeGuiHolder(@NotNull String backPexUuid, @NotNull Location sourceBlockLocation) {
        this.backPexUuid = backPexUuid;
        this.sourceItemStack = null;
        this.sourceBlockLocation = sourceBlockLocation;
    }

    @NotNull
    public String getBackPexUuid() {
        return backPexUuid;
    }

    @Nullable
    public ItemStack getSourceItemStack() {
        return sourceItemStack;
    }

    @Nullable
    public Location getSourceBlockLocation() {
        return sourceBlockLocation;
    }

    public boolean isBlockUpgrade() {
        return this.sourceBlockLocation != null;
    }

    public boolean isItemUpgrade() {
        return this.sourceItemStack != null;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}