package com.Tajra.backPex;

import org.bukkit.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.Chest;
import org.bukkit.block.BlockFace;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.block.TileState;
import org.bukkit.block.Container;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.block.BlockState;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class CauldronCraftListener implements Listener {
    private final Set<UUID> recentlyPickedUp = new HashSet<>();
    private final BackPex plugin;

    public CauldronCraftListener(BackPex plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCauldronInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (clickedBlock == null) {
            return;
        }

        if (clickedBlock.getType() != Material.WATER_CAULDRON) {
            return;
        }

        if (!(clickedBlock.getBlockData() instanceof Levelled)) {
            return;
        }

        Levelled cauldronData = (Levelled) clickedBlock.getBlockData();
        if (cauldronData.getLevel() == 0) {
            player.sendMessage(ChatColor.RED + "O caldeirão precisa de água para o ritual!");
            return;
        }

        Location cauldronLocation = clickedBlock.getLocation();
        World world = cauldronLocation.getWorld();
        if (world == null) {
            return;
        }

        List<Item> nearbyItems = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(cauldronLocation.clone().add(0.5, 0.5, 0.5), 1.2, 1.2, 1.2)) {
            if (entity instanceof Item) {
                nearbyItems.add((Item) entity);
            }
        }

        Item chestItemEntity = null;
        Item craftingTableItemEntity = null;
        Item purpleDyeItemEntity = null;

        for (Item itemEntity : nearbyItems) {
            ItemStack itemStack = itemEntity.getItemStack();
            if (itemStack.getType() == Material.CHEST && chestItemEntity == null) {
                if (itemStack.hasItemMeta()) {
                    ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                        continue;
                    }
                }
                chestItemEntity = itemEntity;

            } else if (itemStack.getType() == Material.CRAFTING_TABLE && craftingTableItemEntity == null) {
                craftingTableItemEntity = itemEntity;
            } else if (itemStack.getType() == Material.PURPLE_DYE && purpleDyeItemEntity == null) {
                purpleDyeItemEntity = itemEntity;
            }
        }

        if (chestItemEntity != null && craftingTableItemEntity != null && purpleDyeItemEntity != null) {
            if (chestItemEntity.getItemStack().getAmount() > 1) {
                chestItemEntity.getItemStack().setAmount(chestItemEntity.getItemStack().getAmount() - 1);
            } else {
                chestItemEntity.remove();
            }

            if (craftingTableItemEntity.getItemStack().getAmount() > 1) {
                craftingTableItemEntity.getItemStack().setAmount(craftingTableItemEntity.getItemStack().getAmount() - 1);
            } else {
                craftingTableItemEntity.remove();
            }

            if (purpleDyeItemEntity.getItemStack().getAmount() > 1) {
                purpleDyeItemEntity.getItemStack().setAmount(purpleDyeItemEntity.getItemStack().getAmount() - 1);
            } else {
                purpleDyeItemEntity.remove();
            }

            int currentLevel = cauldronData.getLevel();
            if (currentLevel - 1 == 0) {
                clickedBlock.setType(Material.CAULDRON);
            } else {
                cauldronData.setLevel(currentLevel - 1);
                clickedBlock.setBlockData(cauldronData);
            }

            ItemStack backpexItem = plugin.createBackPexItem(player);
            Location dropLocation = cauldronLocation.clone().add(0, 1, 0);
            world.dropItemNaturally(cauldronLocation.add(0, 1, 0), backpexItem);

            player.sendMessage(ChatColor.GOLD + "Um BackPex foi criado misticamente!");
            world.playSound(dropLocation, org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            world.spawnParticle(org.bukkit.Particle.PORTAL, dropLocation, 50, 0.5, 0.5, 0.5, 0.1);

        } else {
            player.sendMessage(ChatColor.YELLOW + "O ritual requer um Baú, uma Mesa de Trabalho e Corante Roxo no caldeirão.");
        }
    }

    @EventHandler
    public void onBackPexOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() != Material.CHEST || !itemInHand.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
            return;
        }

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            openBackPexAsBackpack(player, itemInHand, meta);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);

        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            if (recentlyPickedUp.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) {
                event.setCancelled(true);
                openBackPexAsBackpack(player, itemInHand, meta);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                return;
            }

            Material clickedType = clickedBlock.getType();
            if (clickedType == Material.CAULDRON || clickedType == Material.WATER_CAULDRON) {
                event.setCancelled(true);
                openBackPexAsBackpack(player, itemInHand, meta);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                return;
            }

            if (clickedBlock.getType() == Material.CHEST
                    && clickedBlock.getState() instanceof org.bukkit.block.Chest) {
                org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) clickedBlock.getState();
                if (chestState.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)
                        && player.isSneaking()) {
                    return;
                }
            }

            boolean canPlace = true;
            if (clickedType.isInteractable() && !player.isSneaking()) {
                if (clickedType == Material.CHEST ||
                        clickedType == Material.TRAPPED_CHEST ||
                        clickedType == Material.ENDER_CHEST ||
                        clickedType.name().contains("SHULKER_BOX") ||
                        clickedType == Material.FURNACE ||
                        clickedType == Material.BLAST_FURNACE ||
                        clickedType == Material.SMOKER ||
                        clickedType == Material.CRAFTING_TABLE ||
                        clickedType == Material.ANVIL ||
                        clickedType == Material.CHIPPED_ANVIL ||
                        clickedType == Material.DAMAGED_ANVIL ||
                        clickedType == Material.ENCHANTING_TABLE ||
                        clickedType == Material.BARREL ||
                        clickedType == Material.LOOM ||
                        clickedType == Material.GRINDSTONE ||
                        clickedType == Material.STONECUTTER ||
                        clickedType == Material.CARTOGRAPHY_TABLE ||
                        clickedType == Material.SMITHING_TABLE ||
                        clickedType == Material.BREWING_STAND ||
                        clickedType.toString().contains("DOOR") ||
                        clickedType.toString().contains("GATE") ||
                        clickedType.toString().contains("BUTTON") ||
                        clickedType == Material.LEVER ||
                        clickedType == Material.COMPARATOR ||
                        clickedType == Material.REPEATER ||
                        clickedType == Material.DAYLIGHT_DETECTOR ||
                        clickedType.toString().contains("SIGN") ||
                        clickedType == Material.CAULDRON ||
                        clickedType == Material.WATER_CAULDRON
                ) {
                    canPlace = false;
                }
            }

            if (canPlace) {
                event.setCancelled(true);
                Location placeLocation = clickedBlock.getRelative(event.getBlockFace()).getLocation();
                Material typeAtPlaceLocation = placeLocation.getBlock().getType();

                if (typeAtPlaceLocation.isAir() || typeAtPlaceLocation == Material.SHORT_GRASS || typeAtPlaceLocation == Material.SNOW) {
                    placeBackPexBlock(player, itemInHand, placeLocation, meta);
                } else {
                    player.sendMessage(ChatColor.RED + "Você não pode colocar o BackPex aqui!");
                    openBackPexAsBackpack(player, itemInHand, meta);
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                }
            }
        }
    }

    private void openBackPexAsBackpack(Player player, ItemStack backPexItem, ItemMeta backPexMeta) {
        // CORRIGIDO: A lógica de trava foi movida para este método.
        String ownerUuid = backPexMeta.getPersistentDataContainer().get(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING);
        byte lockState = backPexMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);

        if (lockState == 1) {
            if (ownerUuid != null && !player.getUniqueId().toString().equals(ownerUuid)) {
                player.sendMessage(ChatColor.RED + "Este BackPex está trancado.");
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }
        }

        String backPexUuid = backPexMeta.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
        if (backPexUuid == null) return;

        int level = backPexMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
        String serializedContents = backPexMeta.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);

        BackPexInventoryHolder holder = new BackPexInventoryHolder(backPexUuid, backPexItem.clone());
        plugin.setCurrentPage(player, 0);
        // CORRIGIDO: Passa o 'lockState' como parâmetro.
        openBackPexGUI(player, backPexUuid, level, serializedContents, lockState, holder);
    }

    private int getInventorySizeForLevel(int level) {
        switch (level) {
            case 0: return 18;
            case 1: return 27;
            case 2: return 36;
            case 3: return 45;
            case 4: return 54;
            case 5: return 54 + 18;
            case 6: return 72 + 27;
            case 7: return 99 + 27;
            case 8: return 126 + 27;
            case 9: return 153 + 27;
            case 10: return 180 + 36;
            case 11: return 216 + 45;
            case 12: return 261 + 54;
            default: return 18;
        }
    }

    @EventHandler
    public void onBackPexClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BackPexInventoryHolder) {
            BackPexInventoryHolder backPexHolder = (BackPexInventoryHolder) event.getInventory().getHolder();
            Player player = (Player) event.getPlayer();

            final ItemStack[] contentsToSave = new ItemStack[plugin.getSlotsPorPagina()];
            for (int i = 0; i < plugin.getSlotsPorPagina(); i++) {
                contentsToSave[i] = event.getInventory().getItem(i);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory nextOpenInv = player.getOpenInventory().getTopInventory();
                if (!(nextOpenInv.getHolder() instanceof BackPexInventoryHolder) &&
                        !(nextOpenInv.getHolder() instanceof BackPexUpgradeGuiHolder))
                {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                    savePageContents(player, backPexHolder, contentsToSave);
                    plugin.stopViewingByPlayer(player.getUniqueId());
                    plugin.removeCurrentPage(player);
                    player.updateInventory();
                }
            });
        }
    }

    private boolean isSameBackPex(ItemStack itemToCheck, String targetUuid) {
        if (itemToCheck != null && itemToCheck.hasItemMeta()) {
            ItemMeta meta = itemToCheck.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                String uuid = meta.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
                return targetUuid.equals(uuid);
            }
        }
        return false;
    }

    private void placeBackPexBlock(Player player, ItemStack backPexItemToPlace, Location location, ItemMeta itemMeta) {
        Block newBlock = location.getBlock();
        newBlock.setType(Material.CHEST);

        BlockState blockState = newBlock.getState();
        if (blockState.getBlockData() instanceof org.bukkit.block.data.Directional) {
            org.bukkit.block.data.Directional directionalData = (org.bukkit.block.data.Directional) blockState.getBlockData();
            BlockFace playerFacing = player.getFacing().getOppositeFace();

            if (playerFacing == BlockFace.UP || playerFacing == BlockFace.DOWN) {
                float yaw = player.getLocation().getYaw();
                if (yaw < 0) yaw += 360;
                yaw %= 360;
                int i = (int)((yaw + 45.0D) / 90.0D) & 3;
                BlockFace[] cardinalFaces = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
                directionalData.setFacing(cardinalFaces[i]);
            } else {
                directionalData.setFacing(player.getFacing().getOppositeFace());
            }
            newBlock.setBlockData(directionalData);
            blockState = newBlock.getState();
        }

        if (blockState instanceof org.bukkit.block.TileState && blockState instanceof org.bukkit.block.Container) {
            org.bukkit.block.TileState tileState = (org.bukkit.block.TileState) blockState;

            String uuid = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
            Integer level = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER);
            String content = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
            String owner = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING);
            Byte locked = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE);

            if (uuid != null) {
                tileState.getPersistentDataContainer().set(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING, uuid);
            }
            if (level != null) {
                tileState.getPersistentDataContainer().set(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, level);
            }
            if (content != null) {
                tileState.getPersistentDataContainer().set(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING, content);
            }
            if (owner != null) {
                tileState.getPersistentDataContainer().set(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING, owner);
            }
            if (locked != null) {
                tileState.getPersistentDataContainer().set(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, locked);
            }

            blockState.update(true);

            if (backPexItemToPlace.getAmount() > 1) {
                backPexItemToPlace.setAmount(backPexItemToPlace.getAmount() - 1);
            } else {
                if (player.getInventory().getItemInMainHand().isSimilar(backPexItemToPlace)) {
                    player.getInventory().setItemInMainHand(null);
                } else if (player.getInventory().getItemInOffHand().isSimilar(backPexItemToPlace)) {
                    player.getInventory().setItemInOffHand(null);
                }
            }
            player.updateInventory();
            player.sendMessage(ChatColor.GREEN + "BackPex colocado no chão!");
        } else {
            player.sendMessage(ChatColor.RED + "Erro: Bloco colocado não é um TileState ou Container (não é um baú válido para BackPex).");
            newBlock.setType(Material.AIR);
        }
    }

    @EventHandler
    public void onPlacedBackPexInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) return;
        if (!(clickedBlock.getState() instanceof Chest)) return;

        Chest chestState = (Chest) clickedBlock.getState();
        if (!chestState.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) return;

        event.setCancelled(true);

        String ownerUuid = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING);
        byte lockState = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
        boolean isOwner = ownerUuid != null && ownerUuid.equals(player.getUniqueId().toString());

        if (player.isSneaking()) {
            if (!isOwner) {
                player.sendMessage(ChatColor.RED + "Apenas o dono pode pegar este BackPex.");
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }

            String backPexUuid = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
            int level = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            String serializedContents = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);

            ItemStack backPexItemToGive = plugin.createBackPexItem(player);
            ItemMeta meta = backPexItemToGive.getItemMeta();

            if (meta != null) {
                meta.getPersistentDataContainer().set(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING, backPexUuid);
                meta.getPersistentDataContainer().set(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, level);
                meta.getPersistentDataContainer().set(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING, ownerUuid);
                meta.getPersistentDataContainer().set(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, lockState);
                if (serializedContents != null) {
                    meta.getPersistentDataContainer().set(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING, serializedContents);
                }
                String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(ownerUuid)).getName();
                List<String> lore = new ArrayList<>(Arrays.asList("§7Uma mochila especial para seus itens.", "§7Nível: §e" + level, "§8Dono: " + ownerName));
                meta.setLore(lore);

                backPexItemToGive.setItemMeta(meta);
            }

            clickedBlock.setType(Material.AIR);
            recentlyPickedUp.add(player.getUniqueId());
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> recentlyPickedUp.remove(player.getUniqueId()), 2L);

            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(backPexItemToGive);
            } else {
                clickedBlock.getWorld().dropItemNaturally(clickedBlock.getLocation().add(0.5, 0.5, 0.5), backPexItemToGive);
            }
            player.sendMessage(ChatColor.GREEN + "BackPex pego de volta!");
        } else {
            if (lockState == 1 && !isOwner) {
                player.sendMessage(ChatColor.RED + "Este BackPex está trancado.");
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                return;
            }

            String backPexUuid = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
            int level = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            String serializedContents = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);

            BackPexInventoryHolder holder = new BackPexInventoryHolder(backPexUuid, clickedBlock.getLocation());
            plugin.setCurrentPage(player, 0);
            openBackPexGUI(player, backPexUuid, level, serializedContents, lockState, holder);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        Player player = (Player) event.getWhoClicked();

        if (topInventory.getHolder() instanceof BackPexInventoryHolder) {
            BackPexInventoryHolder currentBackPexHolder = (BackPexInventoryHolder) topInventory.getHolder();
            int inventorySize = topInventory.getSize();
            int indicatorSlot = inventorySize - 1;

            if (event.getClickedInventory() == topInventory) {
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem != null && (
                        clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                                clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE
                )) {
                    event.setCancelled(true);
                    return;
                }

                ItemMeta clickedMeta = (clickedItem != null) ? clickedItem.getItemMeta() : null;

                if (clickedMeta != null) {
                    if (clickedMeta.getPersistentDataContainer().has(BackPex.BACKPEX_LOCK_TOGGLE_KEY, PersistentDataType.BYTE)) {
                        event.setCancelled(true);
                        toggleLockFromGui(player, currentBackPexHolder); // Chama o novo método
                        return;
                    }

                    if (clickedMeta.getPersistentDataContainer().has(BackPex.PAGE_NEXT_BUTTON_KEY, PersistentDataType.BYTE)) {
                        event.setCancelled(true);
                        changeBackPexPage(player, currentBackPexHolder, 1);
                        return;
                    }
                    if (clickedMeta.getPersistentDataContainer().has(BackPex.PAGE_PREV_BUTTON_KEY, PersistentDataType.BYTE)) {
                        event.setCancelled(true);
                        changeBackPexPage(player, currentBackPexHolder, -1);
                        return;
                    }

                    if (clickedMeta.getPersistentDataContainer().has(BackPex.PAGE_INDICATOR_KEY, PersistentDataType.INTEGER)) {
                        event.setCancelled(true);
                        return;
                    }
                }
                if (event.getSlot() == indicatorSlot) {
                    event.setCancelled(true);

                    String backPexUuid = currentBackPexHolder.getBackPexUuid();
                    BackPexUpgradeGuiHolder upgradeHolder;
                    String upgradeGuiTitle;

                    if (currentBackPexHolder.isItemSource()) {
                        upgradeHolder = new BackPexUpgradeGuiHolder(backPexUuid, currentBackPexHolder.getBackPexItemStack());
                        upgradeGuiTitle = ChatColor.DARK_RED + "BackPex Upgrade (Item)";
                    } else if (currentBackPexHolder.isBlockSource()) {
                        upgradeHolder = new BackPexUpgradeGuiHolder(backPexUuid, currentBackPexHolder.getBlockLocation());
                        upgradeGuiTitle = ChatColor.DARK_RED + "BackPex Upgrade (Bloco)";
                    } else {
                        player.sendMessage(ChatColor.RED + "Erro: Não foi possível determinar a origem do BackPex para upgrade.");
                        return;
                    }

                    Inventory upgradeGui = Bukkit.createInventory(upgradeHolder, 27, upgradeGuiTitle);
                    populateUpgradeGui(player, upgradeGui);

                    player.openInventory(upgradeGui);
                    return;
                }
            }

            ItemStack itemToPlace = null;
            boolean isPotentiallyMovingToBackPex = false;

            if (event.getClick().isLeftClick() || event.getClick().isRightClick()) {
                if (event.getClickedInventory() == topInventory) {
                    itemToPlace = event.getCursor();
                    isPotentiallyMovingToBackPex = true;
                }
            } else if (event.getClick().isShiftClick()) {
                if (event.getClickedInventory() != null && !event.getClickedInventory().equals(topInventory)) {
                    itemToPlace = event.getCurrentItem();
                    isPotentiallyMovingToBackPex = true;
                }
            } else if (event.getClick() == ClickType.NUMBER_KEY) {
                itemToPlace = player.getInventory().getItem(event.getHotbarButton());
                isPotentiallyMovingToBackPex = true;
            }

            if (isPotentiallyMovingToBackPex && itemToPlace != null && itemToPlace.getType() == Material.CHEST) {
                if (itemToPlace.hasItemMeta()) {
                    ItemMeta metaToCheck = itemToPlace.getItemMeta();
                    if (metaToCheck != null && metaToCheck.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Você não pode colocar uma BackPex dentro de outra!");
                    }
                }
            }
        } else if (topInventory.getHolder() instanceof BackPexUpgradeGuiHolder) {
            event.setCancelled(true);

            BackPexUpgradeGuiHolder upgradeHolder = (BackPexUpgradeGuiHolder) topInventory.getHolder();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            ItemMeta clickedMeta = clickedItem.getItemMeta();
            if (clickedMeta == null) return;

            if (clickedMeta.getPersistentDataContainer().has(BackPex.UPGRADE_CANCEL_BUTTON_KEY, PersistentDataType.BYTE)) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Upgrade cancelado.");
            }
            else if (clickedMeta.getPersistentDataContainer().has(BackPex.UPGRADE_CONFIRM_BUTTON_KEY, PersistentDataType.BYTE)) {
                handleUpgradeConfirmation(player, upgradeHolder);
            }
        }
    }

    @EventHandler
    public void onBackPexBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        if (brokenBlock.getType() != Material.CHEST) return;

        if (brokenBlock.getState() instanceof Chest) {
            Chest chestState = (Chest) brokenBlock.getState();

            if (chestState.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                byte lockState = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
                if (lockState == 1) {
                    String ownerUuid = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING);
                    if (ownerUuid != null && !player.getUniqueId().toString().equals(ownerUuid)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Você não pode quebrar um BackPex que está trancado por outro jogador.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                }

                event.setDropItems(false);

                String backPexUuid = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING);
                int level = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
                String serializedContents = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
                String ownerUuidOnBlock = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING);

                ItemStack backPexItemToDrop = plugin.createBackPexItem(player);
                ItemMeta meta = backPexItemToDrop.getItemMeta();

                if (meta != null) {
                    meta.getPersistentDataContainer().set(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING, backPexUuid);
                    meta.getPersistentDataContainer().set(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, level);

                    if (ownerUuidOnBlock != null) {
                        meta.getPersistentDataContainer().set(BackPex.BACKPEX_OWNER_KEY, PersistentDataType.STRING, ownerUuidOnBlock);
                    }
                    meta.getPersistentDataContainer().set(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, lockState);

                    OfflinePlayer owner = ownerUuidOnBlock != null ? Bukkit.getOfflinePlayer(UUID.fromString(ownerUuidOnBlock)) : player;
                    List<String> lore = new ArrayList<>(Arrays.asList("§7Uma mochila especial para seus itens.", "§7Nível: §e" + level, "§8Dono: " + owner.getName()));
                    meta.setLore(lore);

                    if (serializedContents != null && !serializedContents.isEmpty()) {
                        meta.getPersistentDataContainer().set(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING, serializedContents);
                    }
                    backPexItemToDrop.setItemMeta(meta);
                }
                brokenBlock.getWorld().dropItemNaturally(brokenBlock.getLocation().add(0.5, 0.5, 0.5), backPexItemToDrop);
            }
        }
    }

    private void populateUpgradeGui(Player player, Inventory upgradeGui) {
        if (!(upgradeGui.getHolder() instanceof BackPexUpgradeGuiHolder)) {
            return;
        }
        BackPexUpgradeGuiHolder upgradeHolder = (BackPexUpgradeGuiHolder) upgradeGui.getHolder();

        int currentLevel = -1;
        if (upgradeHolder.isItemUpgrade()) {
            ItemMeta itemMeta = upgradeHolder.getSourceItemStack().getItemMeta();
            if (itemMeta != null) {
                currentLevel = itemMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            }
        } else if (upgradeHolder.isBlockUpgrade()) {
            Block block = upgradeHolder.getSourceBlockLocation().getBlock();
            if (block.getState() instanceof org.bukkit.block.Chest) {
                org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
                if (chestState.getPersistentDataContainer().has(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                    currentLevel = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
                }
            }
        }

        if (currentLevel == -1) {
            player.sendMessage(ChatColor.RED + "Erro ao obter informações do BackPex para upgrade.");
            player.closeInventory();
            return;
        }

        ItemStack fillerPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = fillerPane.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            fillerPane.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < upgradeGui.getSize(); i++) {
            upgradeGui.setItem(i, fillerPane);
        }

        if (currentLevel < 12) {
            Material requiredMaterial = plugin.getMaterialForLevelIndicator(currentLevel);
            int requiredAmount = plugin.getRequiredAmountForNextLevel(currentLevel);
            int playerHasAmount = plugin.countPlayerItems(player, requiredMaterial);

            ItemStack nextLevelItem = new ItemStack(requiredMaterial);
            ItemMeta nextLevelMeta = nextLevelItem.getItemMeta();
            if (nextLevelMeta != null) {
                nextLevelMeta.setDisplayName(ChatColor.YELLOW + "Próximo Nível: " + (currentLevel + 1));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custo: " + ChatColor.AQUA + requiredAmount + " " + requiredMaterial.toString().replace("_", " ").toLowerCase());
                lore.add(ChatColor.GRAY + "Você possui: " + (playerHasAmount >= requiredAmount ? ChatColor.GREEN : ChatColor.RED) + playerHasAmount);
                if (playerHasAmount < requiredAmount) {
                    lore.add(ChatColor.RED + "Você não tem materiais suficientes!");
                }
                nextLevelMeta.setLore(lore);
                nextLevelItem.setItemMeta(nextLevelMeta);
            }
            upgradeGui.setItem(11, nextLevelItem);

            ItemStack confirmButton = new ItemStack(Material.GREEN_WOOL);
            ItemMeta confirmMeta = confirmButton.getItemMeta();
            if (confirmMeta != null) {
                confirmMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Confirmar Upgrade");
                List<String> confirmLore = new ArrayList<>();
                if (playerHasAmount >= requiredAmount) {
                    confirmLore.add(ChatColor.GRAY + "Clique para evoluir seu BackPex!");
                } else {
                    confirmLore.add(ChatColor.RED + "Materiais insuficientes.");
                }
                confirmMeta.setLore(confirmLore);
                confirmMeta.getPersistentDataContainer().set(BackPex.UPGRADE_CONFIRM_BUTTON_KEY, PersistentDataType.BYTE, (byte) 1);
                confirmButton.setItemMeta(confirmMeta);
            }
            upgradeGui.setItem(13, confirmButton);
        } else {
            ItemStack maxLevelItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta maxLevelMeta = maxLevelItem.getItemMeta();
            if (maxLevelMeta != null) {
                maxLevelMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Nível Máximo Alcançado!");
                maxLevelMeta.setLore(List.of(ChatColor.GREEN + "Seu BackPex está no poder total!"));
                maxLevelItem.setItemMeta(maxLevelMeta);
            }
            upgradeGui.setItem(13, maxLevelItem);
        }

        ItemStack cancelButton = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Cancelar");
            cancelMeta.setLore(List.of(ChatColor.GRAY + "Clique para fechar."));
            cancelMeta.getPersistentDataContainer().set(BackPex.UPGRADE_CANCEL_BUTTON_KEY, PersistentDataType.BYTE, (byte) 1);
            cancelButton.setItemMeta(cancelMeta);
        }
        upgradeGui.setItem(15, cancelButton);
    }

    private void handleUpgradeConfirmation(Player player, BackPexUpgradeGuiHolder upgradeHolder) {
        String backPexUuid = upgradeHolder.getBackPexUuid();
        int currentLevel = -1;

        if (upgradeHolder.isItemUpgrade()) {
            ItemMeta sourceMeta = upgradeHolder.getSourceItemStack().getItemMeta();
            if (sourceMeta != null) {
                currentLevel = sourceMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            }
        } else if (upgradeHolder.isBlockUpgrade()) {
            Block block = upgradeHolder.getSourceBlockLocation().getBlock();
            if (block.getState() instanceof org.bukkit.block.Chest) {
                org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
                if (backPexUuid.equals(chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING))) {
                    currentLevel = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
                }
            }
        }

        if (currentLevel == -1) { player.sendMessage(ChatColor.RED + "Erro: Não foi possível determinar o BackPex para o upgrade."); player.closeInventory(); return; }
        if (currentLevel >= 12) { player.sendMessage(ChatColor.YELLOW + "Seu BackPex já está no nível máximo!"); player.closeInventory(); return; }

        Material requiredMaterial = plugin.getMaterialForLevelIndicator(currentLevel);
        int requiredAmount = plugin.getRequiredAmountForNextLevel(currentLevel);

        if (requiredMaterial == null || requiredAmount == 0) { player.sendMessage(ChatColor.RED + "Erro: Não foi possível obter informações de upgrade para o próximo nível."); player.closeInventory(); return; }

        if (plugin.countPlayerItems(player, requiredMaterial) >= requiredAmount) {
            if (plugin.removeItemsFromPlayer(player, requiredMaterial, requiredAmount)) {
                int newLevel = currentLevel + 1;

                if (upgradeHolder.isItemUpgrade()) {
                    ItemStack actualItemInInventory = null;
                    PlayerInventory playerInv = player.getInventory();
                    for (int i = 0; i < playerInv.getSize(); i++) {
                        if (isSameBackPex(playerInv.getItem(i), backPexUuid)) {
                            actualItemInInventory = playerInv.getItem(i);
                            break;
                        }
                    }

                    if (actualItemInInventory != null) {
                        ItemMeta actualMeta = actualItemInInventory.getItemMeta();
                        actualMeta.getPersistentDataContainer().set(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, newLevel);
                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.GRAY + "Uma mochila especial para seus itens.");
                        lore.add(ChatColor.GRAY + "Nível: " + ChatColor.YELLOW + newLevel);
                        actualMeta.setLore(lore);
                        actualItemInInventory.setItemMeta(actualMeta);
                    } else {
                        player.sendMessage(ChatColor.RED + "Erro: Não foi possível encontrar o item BackPex original para atualizar."); player.closeInventory(); return;
                    }
                } else if (upgradeHolder.isBlockUpgrade()) {
                    Block block = upgradeHolder.getSourceBlockLocation().getBlock();
                    if (block.getState() instanceof org.bukkit.block.Chest) {
                        org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
                        if(backPexUuid.equals(chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING))) {
                            chestState.getPersistentDataContainer().set(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, newLevel);
                            chestState.update(true);
                        } else {
                            player.sendMessage(ChatColor.RED + "Erro: O bloco BackPex foi alterado ou removido."); player.closeInventory(); return;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Erro: O bloco BackPex não é mais um baú."); player.closeInventory(); return;
                    }
                }

                player.sendMessage(ChatColor.GREEN + "BackPex evoluído para o Nível " + newLevel + "!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
                player.closeInventory();

            } else {
                player.sendMessage(ChatColor.RED + "Erro ao remover os itens do seu inventário.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Você não tem materiais suficientes para este upgrade!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUuid = event.getPlayer().getUniqueId();
        plugin.stopViewingByPlayer(playerUuid);
        plugin.removeCurrentPage(event.getPlayer());
    }

    private void openBackPexGUI(Player player, String backPexUuid, int level, String serializedContents, byte lockState, BackPexInventoryHolder holder) {
        UUID jogadorAtual = player.getUniqueId();
        if (plugin.isBeingViewed(backPexUuid) && !jogadorAtual.equals(plugin.getViewerOf(backPexUuid))) {
            player.sendMessage(ChatColor.RED + "Este BackPex já está em uso por outro jogador!");
            return;
        }
        plugin.startViewing(backPexUuid, jogadorAtual);

        int totalSlots = getInventorySizeForLevel(level);
        int totalPages = plugin.getTotalPages(totalSlots);
        int currentPage = plugin.getCurrentPage(player);

        String inventoryTitle = String.format("%sBackPex (Nível %d) - Pág %d/%d",
                ChatColor.DARK_PURPLE, level, currentPage + 1, totalPages);
        Inventory backPexInventory = Bukkit.createInventory(holder, 54, inventoryTitle);

        ItemStack[] allItems = plugin.base64ToItemStackArray(serializedContents);
        if (allItems == null) {
            allItems = new ItemStack[totalSlots];
        } else if (allItems.length < totalSlots) {
            allItems = Arrays.copyOf(allItems, totalSlots);
        }

        int slotsPorPagina = plugin.getSlotsPorPagina();
        int startIndex = currentPage * slotsPorPagina;

        for (int i = 0; i < slotsPorPagina; i++) {
            int contentIndex = startIndex + i;
            if (contentIndex < allItems.length) {
                backPexInventory.setItem(i, allItems[contentIndex]);
            }
        }

        ItemStack controlFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = controlFiller.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            controlFiller.setItemMeta(meta);
        }
        for (int i = 45; i < 54; i++) {
            backPexInventory.setItem(i, controlFiller);
        }

        backPexInventory.setItem(45, plugin.createLockToggleButton(lockState));

        // Botão Página Anterior à esquerda do livro (Slot 48)
        if (currentPage > 0) {
            backPexInventory.setItem(48, plugin.createPrevPageButton());
        }

        backPexInventory.setItem(49, plugin.createPageIndicatorItem(currentPage, totalPages));

        if (currentPage < totalPages - 1) {
            backPexInventory.setItem(50, plugin.createNextPageButton());
        }
        backPexInventory.setItem(53, plugin.createLevelIndicatorItem(level));

        player.openInventory(backPexInventory);
    }

    // Em CauldronCraftListener.java
    private void changeBackPexPage(Player player, BackPexInventoryHolder holder, int direction) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

        saveCurrentPage(player, player.getOpenInventory().getTopInventory(), holder);

        int currentPage = plugin.getCurrentPage(player);
        plugin.setCurrentPage(player, currentPage + direction);

        String backPexUuid = holder.getBackPexUuid();
        int level;
        String serializedContents;
        byte lockState; // CORRIGIDO: Adicionada variável para o estado de trava

        if (holder.isItemSource()) {
            ItemStack updatedItem = findBackPexItemInPlayerInventory(player, backPexUuid);
            if (updatedItem == null) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Erro: O item BackPex não foi encontrado para mudar de página.");
                return;
            }
            ItemMeta itemMeta = updatedItem.getItemMeta();
            level = itemMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            serializedContents = itemMeta.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
            lockState = itemMeta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0); // CORRIGIDO
        } else if (holder.isBlockSource()) {
            Block block = holder.getBlockLocation().getBlock();
            if (!(block.getState() instanceof Chest)) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Erro: O bloco BackPex foi removido.");
                return;
            }
            Chest chestState = (Chest) block.getState();
            level = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            serializedContents = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
            lockState = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0); // CORRIGIDO
        } else {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            // CORRIGIDO: Passa o 'lockState' para a chamada do método
            openBackPexGUI(player, backPexUuid, level, serializedContents, lockState, holder);
        });
    }

    private void saveCurrentPage(Player player, Inventory currentInventory, BackPexInventoryHolder holder) {
        ItemStack[] pageContents = new ItemStack[plugin.getSlotsPorPagina()];
        for (int i = 0; i < plugin.getSlotsPorPagina(); i++) {
            pageContents[i] = currentInventory.getItem(i);
        }
        savePageContents(player, holder, pageContents);
    }

    private ItemStack findBackPexItemInPlayerInventory(Player player, String uuid) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSameBackPex(item, uuid)) {
                return item;
            }
        }
        if (isSameBackPex(player.getInventory().getItemInOffHand(), uuid)) {
            return player.getInventory().getItemInOffHand();
        }
        return null;
    }

    private void savePageContents(Player player, BackPexInventoryHolder holder, ItemStack[] pageContents) {

        if (holder == null || (!holder.isItemSource() && !holder.isBlockSource())) {
            return;
        }

        String backPexUuid = holder.getBackPexUuid();
        String serializedContents;
        int totalSlots;

        if (holder.isItemSource()) {
            ItemStack sourceItem = findBackPexItemInPlayerInventory(player, backPexUuid);
            if (sourceItem == null || sourceItem.getItemMeta() == null) {
                return;
            }
            ItemMeta meta = sourceItem.getItemMeta();
            serializedContents = meta.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
            totalSlots = getInventorySizeForLevel(meta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0));
        } else {
            Block block = holder.getBlockLocation().getBlock();
            if (!(block.getState() instanceof Chest)) {
                return;
            }
            Chest chestState = (Chest) block.getState();
            serializedContents = chestState.getPersistentDataContainer().get(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING);
            totalSlots = getInventorySizeForLevel(chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0));
        }

        ItemStack[] allItems = plugin.base64ToItemStackArray(serializedContents);
        if (allItems == null || allItems.length < totalSlots) {
            allItems = new ItemStack[totalSlots];
        }

        int currentPage = plugin.getCurrentPage(player);
        int startIndex = currentPage * plugin.getSlotsPorPagina();
        int remainingSlots = allItems.length - startIndex;
        int copyLength = Math.min(pageContents.length, remainingSlots);

        if (copyLength > 0) {
            System.arraycopy(pageContents, 0, allItems, startIndex, copyLength);
        }

        String newSerializedContents = plugin.itemStackArrayToBase64(allItems);

        if (holder.isItemSource()) {
            int itemSlot = -1;
            ItemStack[] inventoryContents = player.getInventory().getContents();

            for (int i = 0; i < inventoryContents.length; i++) {
                if (isSameBackPex(inventoryContents[i], backPexUuid)) {
                    itemSlot = i;
                    break;
                }
            }

            if (itemSlot == -1 && isSameBackPex(player.getInventory().getItemInOffHand(), backPexUuid)) {
                itemSlot = 40;
            }

            if (itemSlot != -1) {
                ItemStack itemToUpdate = player.getInventory().getItem(itemSlot);
                if (itemToUpdate != null) {
                    ItemMeta meta = itemToUpdate.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING, newSerializedContents);
                        itemToUpdate.setItemMeta(meta);
                        player.getInventory().setItem(itemSlot, itemToUpdate);
                    }
                }
            }
        } else {
            Block block = holder.getBlockLocation().getBlock();
            if (block.getState() instanceof Chest) {
                Chest chestState = (Chest) block.getState();
                if (backPexUuid.equals(chestState.getPersistentDataContainer().get(BackPex.BACKPEX_UUID_KEY, PersistentDataType.STRING))) {
                    chestState.getPersistentDataContainer().set(BackPex.BACKPEX_CONTENT_KEY, PersistentDataType.STRING, newSerializedContents);
                    chestState.update();
                }
            }
        }
    }

    private void toggleLockFromGui(Player player, BackPexInventoryHolder holder) {
        String backPexUuid = holder.getBackPexUuid();
        byte newLockState;

        if (holder.isItemSource()) {
            ItemStack backPexItem = findBackPexItemInPlayerInventory(player, backPexUuid);
            if (backPexItem == null) {
                player.sendMessage(ChatColor.RED + "Erro: Não foi possível encontrar o BackPex no seu inventário.");
                return;
            }
            ItemMeta meta = backPexItem.getItemMeta();
            if (meta == null) return;

            byte currentLockState = meta.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
            newLockState = (currentLockState == 0) ? (byte) 1 : (byte) 0;
            meta.getPersistentDataContainer().set(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, newLockState);
            backPexItem.setItemMeta(meta);

        } else if (holder.isBlockSource()) {
            Block block = holder.getBlockLocation().getBlock();
            if (!(block.getState() instanceof Chest)) {
                player.sendMessage(ChatColor.RED + "Erro: O bloco BackPex foi removido ou alterado.");
                return;
            }
            Chest chestState = (Chest) block.getState();
            byte currentLockState = chestState.getPersistentDataContainer().getOrDefault(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
            newLockState = (currentLockState == 0) ? (byte) 1 : (byte) 0;
            chestState.getPersistentDataContainer().set(BackPex.BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, newLockState);
            chestState.update();

        } else {
            return; // Fonte desconhecida
        }

        // Atualiza o botão na GUI em tempo real
        player.getOpenInventory().getTopInventory().setItem(45, plugin.createLockToggleButton(newLockState));
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, newLockState == 1 ? 0.8f : 1.2f);
    }
}
