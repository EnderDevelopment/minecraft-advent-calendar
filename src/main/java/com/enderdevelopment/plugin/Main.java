package com.enderdevelopment.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin implements Listener {

    private final Map<Integer, ItemStack> gifts = new HashMap<>();
    private final NamespacedKey giftKey = new NamespacedKey(this, "gift");
    private final NamespacedKey claimedKey = new NamespacedKey(this, "claimed");
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadGifts();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadGifts() {
        for (int i = 1; i <= 24; i++) {
            String path = "gifts.day" + i;
            Material material = Material.valueOf(getConfig().getString(path + ".material", "DIAMOND"));
            int amount = getConfig().getInt(path + ".amount", 1);
            ItemStack gift = new ItemStack(material, amount);
            ItemMeta meta = gift.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getConfig().getString(path + ".name", "Geschenk"));
                meta.getPersistentDataContainer().set(giftKey, PersistentDataType.INTEGER, i);
                gift.setItemMeta(meta);
            }
            gifts.put(i, gift);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("calender")) {
                if (player.hasPermission("calender.admin") || LocalDate.now().isAfter(LocalDate.of(2026, 12, 21))) {
                    openCalendar(player);
                } else {
                    player.sendMessage(Component.text("Der Adventskalender kann erst ab dem 21.12.2026 geöffnet werden.").color(NamedTextColor.RED));
                }
                return true;
            }
        }
        return false;
    }

    private void openCalendar(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Adventskalender").color(NamedTextColor.GOLD));
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        
        for (int i = 1; i <= 24; i++) {
            ItemStack item;
            if (i <= day && today.getMonthValue() == 12 && today.getYear() == 2026) {
                ItemStack claimedGift = new ItemStack(Material.COAL);
                ItemMeta meta = claimedGift.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("Abgeholt");
                    meta.getPersistentDataContainer().set(claimedKey, PersistentDataType.INTEGER, i);
                    claimedGift.setItemMeta(meta);
                }
                item = claimedGift;
            } else {
                item = gifts.getOrDefault(i, new ItemStack(Material.CHEST));
            }
            inventory.setItem(i - 1, item);
        }

        // Close Button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("Schließen");
            closeButton.setItemMeta(closeMeta);
        }
        inventory.setItem(26, closeButton);

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(Component.text("Adventskalender").color(NamedTextColor.GOLD))) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            if (slot == 26) {
                player.closeInventory();
            } else if (slot >= 0 && slot < 24) {
                int day = slot + 1;
                LocalDate today = LocalDate.now();
                if (day <= today.getDayOfMonth() && today.getMonthValue() == 12 && today.getYear() == 2026) {
                    ItemStack item = event.getCurrentItem();
                    if (item != null && item.getType() != Material.COAL) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            PersistentDataContainer container = meta.getPersistentDataContainer();
                            if (container.has(giftKey, PersistentDataType.INTEGER)) {
                                int giftDay = container.get(giftKey, PersistentDataType.INTEGER);
                                if (giftDay == day) {
                                    player.getInventory().addItem(gifts.get(day));
                                    ItemStack claimedGift = new ItemStack(Material.COAL);
                                    ItemMeta claimedMeta = claimedGift.getItemMeta();
                                    if (claimedMeta != null) {
                                        claimedMeta.setDisplayName("Abgeholt");
                                        claimedMeta.getPersistentDataContainer().set(claimedKey, PersistentDataType.INTEGER, day);
                                        claimedGift.setItemMeta(claimedMeta);
                                    }
                                    event.getInventory().setItem(slot, claimedGift);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}