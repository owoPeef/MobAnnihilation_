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

public class SpectateMenu extends Menu {
    public SpectateMenu() {
        super("spectate", "Наблюдение", 9, new ArrayList<>());
    }

    @Override
    public Inventory getInventory(GamePlayer gamePlayer) {
        Inventory inventory = Bukkit.createInventory(null, Math.max(9, ((GameManager.getArenaPlayers().size() + 8) / 9) * 9), this.getTitle());

        for (int i = 0; i < GameManager.ARENA_LIST.size(); i++) {
            Arena arena = GameManager.ARENA_LIST.get(i);

            MenuItem menuItem = new MenuItem(
                    i,
                    "&aАрена #" + (i+1),
                    "spectate id " + arena.getId(),
                    Arrays.asList(
                            "&eСтатус: " + arena.getState(),
                            "&bИгроков: &6" + arena.arenaPlayers.size(),
                            "&bСр. уровень: &6" + arena.getLevel(),
                            "",
                            "&aНажмите, чтобы наблюдать"
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

            inventory.setItem(i, itemStack);
            executeItems.add(menuItem);
        }

        MenuManager.INSTANCE_MENUS.put(gamePlayer, this);
        return inventory;
    }
}
