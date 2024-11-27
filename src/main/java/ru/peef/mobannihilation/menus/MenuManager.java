package ru.peef.mobannihilation.menus;

import org.bukkit.ChatColor;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.handlers.MenuDataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MenuManager {
    public static List<Menu> LOADED_MENUS = new ArrayList<>();
    public static HashMap<GamePlayer, Menu> INSTANCE_MENUS = new HashMap<>();

    public static void init() {
        MenuDataHandler.init();

        LOADED_MENUS.add(new SpectateMenu());
        LOADED_MENUS.add(new SelectMenu());

        MobAnnihilation.getInstance().getLogger().info(("&aLoaded &6" + LOADED_MENUS.size() + "&a menus!").replace('&', ChatColor.COLOR_CHAR));
    }

    public static Menu getMenuByTitle(String title) {
        return LOADED_MENUS.stream()
                .filter(menu -> menu.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }
}
