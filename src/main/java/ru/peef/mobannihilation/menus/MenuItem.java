package ru.peef.mobannihilation.menus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    public int slot;
    public int count;
    public String title;
    public String executeCommand;
    public List<String> lore;
    public Material material;
    public ItemStack itemStack;

    public MenuItem(int slot, String title, String executeCommand, List<String> lore, Material material) {
        this.slot = slot;
        this.title = title;
        this.executeCommand = executeCommand;
        this.lore = lore;
        this.material = material;
        this.count = 1;
    }

    public MenuItem(int slot, String title, String executeCommand, List<String> lore, Material material, int count) {
        this.slot = slot;
        this.title = title;
        this.executeCommand = executeCommand;
        this.lore = lore;
        this.material = material;
        this.count = count;
    }

    public String getTitle() { return title.replace('&', ChatColor.COLOR_CHAR); }
    public List<String> getLore() {
        List<String> l = new ArrayList<>();
        lore.forEach(loreS -> l.add(loreS.replace('&', ChatColor.COLOR_CHAR)));
        return l;
    }
}
