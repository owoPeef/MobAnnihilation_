package ru.peef.mobannihilation.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.peef.mobannihilation.game.players.GamePlayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Menu {
    public String name;
    private final String title;
    public List<MenuItem> executeItems;
    public int size;

    public Menu(String name, String title, int size, List<MenuItem> executeItems) {
        this.name = name;
        this.title = title;
        this.size = size;
        this.executeItems = executeItems;
    }

    public String getName() { return name.toLowerCase(); }
    public String getTitle() { return title.replace('&', ChatColor.COLOR_CHAR); }
    public Inventory getInventory(GamePlayer gamePlayer) {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (int i = 0; i < size; i++) {
            for (MenuItem item : executeItems) {
                if (item.slot == i) {
                    ItemStack itemStack = new ItemStack(item.material, item.count);
                    ItemMeta itemMeta = itemStack.getItemMeta();

                    if (itemMeta != null) {
                        itemMeta.setDisplayName(item.getTitle());
                        itemMeta.setLore(item.getLore());
                    }

                    itemStack.setItemMeta(itemMeta);
                    item.itemStack = itemStack;

                    inventory.setItem(i, itemStack);
                }
            }
        }

        return inventory;
    }

    public void addItem(MenuItem item) { executeItems.add(item); }
    public void addItems(List<MenuItem> items) { executeItems.addAll(items); }

    public static Menu find(String openMenu) {
        AtomicReference<Menu> findMenu = new AtomicReference<>();
        MenuManager.LOADED_MENUS.forEach(menu -> {
            if (menu.getName().equals(openMenu)) findMenu.set(menu);
        });
        return findMenu.get();
    }
}
