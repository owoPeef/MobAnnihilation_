package ru.peef.mobannihilation.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;
import ru.peef.mobannihilation.menus.Menu;
import ru.peef.mobannihilation.menus.MenuItem;
import ru.peef.mobannihilation.menus.MenuManager;

public class InventoryListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        boolean isFind = false;
        for (Menu menu : MenuManager.LOADED_MENUS) {
            if (inventory.getTitle().equals(menu.getTitle())) {
                for (MenuItem item : menu.executeItems) {
                    if (item.itemStack.isSimilar(clickedItem)) {
                        player.sendMessage("Try to execute command: " + ChatColor.GOLD + "/" + item.executeCommand);
                        player.performCommand(item.executeCommand);
                        isFind = true;
                    }
                }
            }
        }

        if (!isFind) {
            GamePlayer gamePlayer = PlayerManager.get(player);

            if (gamePlayer != null) {
                if (MenuManager.INSTANCE_MENUS.containsKey(gamePlayer)) {
                    Menu menu = MenuManager.INSTANCE_MENUS.get(gamePlayer);

                    if (inventory.getTitle().equals(menu.getTitle())) {
                        for (MenuItem item : menu.executeItems) {
                            if (item.itemStack.isSimilar(clickedItem)) {
                                player.performCommand(item.executeCommand);
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            if (MenuManager.INSTANCE_MENUS.containsKey(gamePlayer)) {
                Menu menu = MenuManager.INSTANCE_MENUS.get(gamePlayer);

                if (menu.getTitle().equals(inventory.getTitle())) {
                    MenuManager.INSTANCE_MENUS.remove(gamePlayer, menu);
                }
            }
        }
    }
}
