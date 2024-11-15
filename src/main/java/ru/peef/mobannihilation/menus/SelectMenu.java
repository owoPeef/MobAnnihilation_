package ru.peef.mobannihilation.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.peef.mobannihilation.game.Arena;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.players.GamePlayer;

import java.util.ArrayList;
import java.util.Arrays;

public class SelectMenu extends Menu {
    public SelectMenu() {
        super("select", "Выбор", 9, new ArrayList<>());
    }

    @Override
    public Inventory getInventory(GamePlayer gamePlayer) {
        Inventory inventory = Bukkit.createInventory(null, Math.max(9, ((GameManager.getArenaPlayers().size() + 8) / 9) * 9), this.getTitle());

        int localCount = 0;
        for (int i = 0; i < GameManager.ARENA_LIST.size(); i++) {
            Arena arena = GameManager.ARENA_LIST.get(i);
            if (Math.abs(arena.getOwnerLevel()-gamePlayer.getLevel()) <= 5 && !arena.isStarted && !arena.startCountdown) {
                MenuItem menuItem = new MenuItem(
                        localCount,
                        "&aАрена #" + (localCount+1),
                        "game join " + arena.getId(),
                        Arrays.asList(
                                "&eСтатус: " + arena.getState(),
                                " ",
                                "&bИгроков: &6" + arena.arenaPlayers.size(),
                                "&bСр. уровень: &6" + arena.getLevel(),
                                "",
                                "&aНажмите, чтобы присоединиться"
                        ),
                        Material.STONE,
                        Math.max(1, arena.spectatePlayers.size()));

                ItemStack itemStack = new ItemStack(menuItem.material, menuItem.count);
                ItemMeta itemMeta = itemStack.getItemMeta();

                if (itemMeta != null) {
                    itemMeta.setDisplayName(menuItem.getTitle());
                    itemMeta.setLore(menuItem.getLore());
                }

                itemStack.setItemMeta(itemMeta);
                menuItem.itemStack = itemStack;

                inventory.setItem(localCount, itemStack);
                executeItems.add(menuItem);
                localCount++;
            }
        }

        MenuManager.INSTANCE_MENUS.put(gamePlayer, this);
        return inventory;
    }
}
