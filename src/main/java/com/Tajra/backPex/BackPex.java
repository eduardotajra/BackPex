package com.Tajra.backPex;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public final class BackPex extends JavaPlugin implements CommandExecutor {

    public static NamespacedKey BACKPEX_LEVEL_KEY;
    public static NamespacedKey BACKPEX_UUID_KEY;
    public static NamespacedKey BACKPEX_CONTENT_KEY;
    public static NamespacedKey BACKPEX_OWNER_KEY;
    public static NamespacedKey BACKPEX_LOCKED_KEY;
    public static NamespacedKey UPGRADE_CONFIRM_BUTTON_KEY;
    public static NamespacedKey UPGRADE_CANCEL_BUTTON_KEY;
    public static NamespacedKey LEVEL_INDICATOR_MARKER_KEY;
    public static NamespacedKey PAGE_NEXT_BUTTON_KEY;
    public static NamespacedKey PAGE_PREV_BUTTON_KEY;
    public static NamespacedKey PAGE_INDICATOR_KEY;

    private final Map<String, UUID> currentlyViewingBackPex = new HashMap<>();
    private final Map<UUID, String> playerViewingWhat = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();

    public int getCurrentPage(Player player) {
        return playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
    }

    public void setCurrentPage(Player player, int page) {
        playerCurrentPage.put(player.getUniqueId(), page);
    }

    public void removeCurrentPage(Player player) {
        playerCurrentPage.remove(player.getUniqueId());
    }

    public boolean isBeingViewed(String backPexUuid) {
        return currentlyViewingBackPex.containsKey(backPexUuid);
    }

    public UUID getViewerOf(String backPexUuid) {
        return currentlyViewingBackPex.get(backPexUuid);
    }

    public void startViewing(String backPexUuid, UUID playerUuid) {
        if (playerViewingWhat.containsKey(playerUuid)) {
            String uuidAntigo = playerViewingWhat.get(playerUuid);
            currentlyViewingBackPex.remove(uuidAntigo);
            playerViewingWhat.remove(playerUuid);
        }
        currentlyViewingBackPex.put(backPexUuid, playerUuid);
        playerViewingWhat.put(playerUuid, backPexUuid);
    }

    public void stopViewingByPlayer(UUID playerUuid) {
        if (!playerViewingWhat.containsKey(playerUuid)) return;
        String backPexUuid = playerViewingWhat.get(playerUuid);
        playerViewingWhat.remove(playerUuid);
        currentlyViewingBackPex.remove(backPexUuid);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        BACKPEX_LEVEL_KEY = new NamespacedKey(this, "backpex_level");
        BACKPEX_UUID_KEY = new NamespacedKey(this, "backpex_uuid");
        BACKPEX_CONTENT_KEY = new NamespacedKey(this, "backpex_content");
        BACKPEX_OWNER_KEY = new NamespacedKey(this, "backpex_owner");
        BACKPEX_LOCKED_KEY = new NamespacedKey(this, "backpex_locked");
        UPGRADE_CONFIRM_BUTTON_KEY = new NamespacedKey(this, "upgrade_confirm_button");
        UPGRADE_CANCEL_BUTTON_KEY = new NamespacedKey(this, "upgrade_cancel_button");
        LEVEL_INDICATOR_MARKER_KEY = new NamespacedKey(this, "backpex_indicator_marker");
        PAGE_NEXT_BUTTON_KEY = new NamespacedKey(this, "page_next_button");
        PAGE_PREV_BUTTON_KEY = new NamespacedKey(this, "page_prev_button");
        PAGE_INDICATOR_KEY = new NamespacedKey(this, "page_indicator");

        getServer().getPluginManager().registerEvents(new CauldronCraftListener(this), this);
        getCommand("backpex").setExecutor(this);

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[BackPex] Plugin habilitado!");
    }

    public int getSlotsPorPagina() {
        return 45;
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BackPex] Plugin desabilitado.");
    }

    public ItemStack createNextPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Próxima Página ->");
            meta.getPersistentDataContainer().set(PAGE_NEXT_BUTTON_KEY, PersistentDataType.BYTE, (byte)1);
            button.setItemMeta(meta);
        }
        return button;
    }

    public ItemStack createPrevPageButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "<- Página Anterior");
            meta.getPersistentDataContainer().set(PAGE_PREV_BUTTON_KEY, PersistentDataType.BYTE, (byte)1);
            button.setItemMeta(meta);
        }
        return button;
    }

    public ItemStack createBackPexItem(Player creator) {
        ItemStack backpex = new ItemStack(Material.CHEST);
        ItemMeta meta = backpex.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "BackPex");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Uma mochila especial para seus itens.");
            lore.add(ChatColor.GRAY + "Nível: " + ChatColor.YELLOW + "0 (Inicial)");
            lore.add(ChatColor.DARK_GRAY + "Dono: " + creator.getName());
            meta.setLore(lore);

            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setCustomModelData(1);

            meta.getPersistentDataContainer().set(BACKPEX_LEVEL_KEY, PersistentDataType.INTEGER, 0);
            meta.getPersistentDataContainer().set(BACKPEX_UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.getPersistentDataContainer().set(BACKPEX_OWNER_KEY, PersistentDataType.STRING, creator.getUniqueId().toString());
            meta.getPersistentDataContainer().set(BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);

            backpex.setItemMeta(meta);
        }

        return backpex;
    }

    public String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            getLogger().severe("Erro ao serializar ItemStackArray para Base64: " + e.getMessage());
            return null;
        }
    }

    public ItemStack[] base64ToItemStackArray(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (ClassNotFoundException | IOException e) {
            getLogger().severe("Erro ao desserializar Base64 para ItemStackArray: " + e.getMessage());
            return null;
        }
    }

    public Material getMaterialForLevelIndicator(int level) {
        String materialName = getConfig().getString("upgrades.level-" + level + ".required-material");

        if (materialName == null) {
            return Material.BARRIER;
        }

        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Material inválido no config.yml para o nível " + level + ": " + materialName);
            return Material.BARRIER;
        }
    }

    public int getRequiredAmountForNextLevel(int currentLevel) {
        return getConfig().getInt("upgrades.level-" + currentLevel + ".required-amount", 0);
    }

    public int countPlayerItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public ItemStack createLevelIndicatorItem(int currentLevel) {
        Material indicatorMaterial = getMaterialForLevelIndicator(currentLevel);
        ItemStack indicatorItem = new ItemStack(indicatorMaterial);
        ItemMeta indicatorMeta = indicatorItem.getItemMeta();

        if (indicatorMeta != null) {
            indicatorMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Nível Atual: " + ChatColor.YELLOW + currentLevel);
            List<String> lore = new ArrayList<>();
            if (currentLevel < 12) {
                lore.add(ChatColor.GRAY + "Clique para ver opções de upgrade.");
            } else {
                lore.add(ChatColor.GREEN + "Nível Máximo Alcançado!");
            }
            indicatorMeta.getPersistentDataContainer()
                    .set(BackPex.LEVEL_INDICATOR_MARKER_KEY, PersistentDataType.BYTE, (byte)1);
            indicatorMeta.setLore(lore);
            indicatorItem.setItemMeta(indicatorMeta);
        }
        return indicatorItem;
    }

    public boolean removeItemsFromPlayer(Player player, Material material, int amountToRemove) {
        if (amountToRemove <= 0) return true;

        if (countPlayerItems(player, material) < amountToRemove) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        int removedCount = 0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int amountInSlot = item.getAmount();
                int amountToTakeFromSlot = Math.min(amountInSlot, amountToRemove - removedCount);

                item.setAmount(amountInSlot - amountToTakeFromSlot);
                removedCount += amountToTakeFromSlot;

                if (item.getAmount() == 0) {
                    inventory.setItem(i, null);
                } else {
                    inventory.setItem(i, item);
                }

                if (removedCount >= amountToRemove) {
                    break;
                }
            }
        }
        player.updateInventory();
        return removedCount >= amountToRemove;
    }

    public int getTotalPages(int totalSlots) {
        if (totalSlots <= 0) return 1;
        return (int) Math.ceil((double) totalSlots / getSlotsPorPagina());
    }

    public ItemStack createPageIndicatorItem(int currentPage, int totalPages) {
        ItemStack indicator = new ItemStack(Material.BOOK);
        ItemMeta meta = indicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Página " + (currentPage + 1) + " de " + totalPages);
            meta.getPersistentDataContainer().set(PAGE_INDICATOR_KEY, PersistentDataType.INTEGER, currentPage);
            indicator.setItemMeta(meta);
        }
        return indicator;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Uso: /backpex <reload|lock>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("backpex.reload")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão.");
                return true;
            }
            this.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "[BackPex] Configuração recarregada!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;

        if (subCommand.equals("lock")) {
            if (!player.hasPermission("backpex.lock")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão.");
                return true;
            }

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.CHEST && itemInHand.hasItemMeta() && itemInHand.getItemMeta().getPersistentDataContainer().has(BACKPEX_UUID_KEY, PersistentDataType.STRING)) {
                toggleLockOnItem(itemInHand, player);
                return true;
            }

            Block targetBlock = player.getTargetBlock(null, 5);
            if (isBackPexBlock(targetBlock)) {
                toggleLockOnBlock(targetBlock, player);
                return true;
            }

            player.sendMessage(ChatColor.RED + "Você precisa estar segurando ou olhando para um BackPex para usar este comando.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Comando desconhecido. Uso: /backpex <reload|lock>");
        return true;
    }


    private boolean isBackPexBlock(Block block) {
        if (block != null && block.getType() == Material.CHEST && block.getState() instanceof org.bukkit.block.Chest) {
            org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) block.getState();
            return chestState.getPersistentDataContainer().has(BACKPEX_UUID_KEY, PersistentDataType.STRING);
        }
        return false;
    }

    private void toggleLockOnItem(ItemStack backPexItem, Player player) {
        ItemMeta meta = backPexItem.getItemMeta();
        if (meta == null) return;

        String ownerUuid = meta.getPersistentDataContainer().get(BACKPEX_OWNER_KEY, PersistentDataType.STRING);
        if (ownerUuid == null || !player.getUniqueId().toString().equals(ownerUuid)) {
            player.sendMessage(ChatColor.RED + "Você não é o dono deste BackPex.");
            return;
        }

        byte currentLockState = meta.getPersistentDataContainer().getOrDefault(BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
        byte newLockState = (currentLockState == 0) ? (byte) 1 : (byte) 0;
        meta.getPersistentDataContainer().set(BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, newLockState);
        backPexItem.setItemMeta(meta);

        player.sendMessage(newLockState == 1 ? ChatColor.GREEN + "BackPex (item) trancado!" : ChatColor.YELLOW + "BackPex (item) destrancado!");
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, newLockState == 1 ? 0.8f : 1.2f);
    }

    private void toggleLockOnBlock(Block backPexBlock, Player player) {
        org.bukkit.block.Chest chestState = (org.bukkit.block.Chest) backPexBlock.getState();
        String ownerUuid = chestState.getPersistentDataContainer().get(BACKPEX_OWNER_KEY, PersistentDataType.STRING);

        if (ownerUuid == null || !player.getUniqueId().toString().equals(ownerUuid)) {
            player.sendMessage(ChatColor.RED + "Você não é o dono deste BackPex.");
            return;
        }

        byte currentLockState = chestState.getPersistentDataContainer().getOrDefault(BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, (byte) 0);
        byte newLockState = (currentLockState == 0) ? (byte) 1 : (byte) 0;
        chestState.getPersistentDataContainer().set(BACKPEX_LOCKED_KEY, PersistentDataType.BYTE, newLockState);
        chestState.update();

        player.sendMessage(newLockState == 1 ? ChatColor.GREEN + "BackPex (bloco) trancado!" : ChatColor.YELLOW + "BackPex (bloco) destrancado!");
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1.0f, newLockState == 1 ? 0.8f : 1.2f);
    }
}
