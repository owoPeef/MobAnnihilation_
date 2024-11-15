package ru.peef.mobannihilation;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.peef.mobannihilation.commands.*;
import ru.peef.mobannihilation.game.AnvilGUI;
import ru.peef.mobannihilation.game.GameExpansion;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.npcs.NPC;
import ru.peef.mobannihilation.game.npcs.NPCManager;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.handlers.PlayerDataHandler;
import ru.peef.mobannihilation.listeners.EntityListener;
import ru.peef.mobannihilation.listeners.InventoryListener;
import ru.peef.mobannihilation.listeners.PlayerListener;
import ru.peef.mobannihilation.game.players.PlayerManager;
import ru.peef.mobannihilation.holograms.Hologram;
import ru.peef.mobannihilation.listeners.WorldListener;
import ru.peef.mobannihilation.menus.MenuManager;

import java.io.File;

public final class MobAnnihilation extends JavaPlugin {
    public static File configFile;
    private static FileConfiguration config;

    @Override
    public void onEnable() {
        createConfig();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("Could not find PlaceholderAPI! This plugin is required.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            new GameExpansion().register();
        }

        GameManager.init();
        MenuManager.init();
        PlayerDataHandler.init();

        NPCManager.init();

        new Hologram("top", GameManager.BASIC_WORLD, 5.5, 20.4, 0.5, ChatColor.YELLOW + (ChatColor.BOLD + "Топ игроков по уровню:"));
        for (int i = 0; i < GameManager.SHOW_TOP_PLAYERS_COUNT; i++) {
            new Hologram("top" + (i+1), GameManager.BASIC_WORLD, 5.5, 20 - (i * 0.3), 0.5, "%mobannihilation_top" + (i+1) + "%");
        }

        getCommand("spectate").setExecutor(new SpectateCommand());
        getCommand("game").setExecutor(new GameCommand());
        getCommand("menu").setExecutor(new MenuCommand());
        getCommand("npc").setExecutor(new NPCCommand());
        getCommand("world").setExecutor(new WorldCommand());
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);
        getServer().getPluginManager().registerEvents(new AnvilGUI(), this);
    }

    @Override
    public void onDisable() {
        PlayerManager.PLAYERS.forEach(GamePlayer::save);
        NPCManager.CHARACTERS.forEach(NPC::despawn);

        for (World world : Bukkit.getWorlds()) world.getEntities().clear();
        GameManager.SPAWNED_ENTITIES.forEach(LivingEntity::remove);
    }

    private void createConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static FileConfiguration getConfiguration() { return config; }
    public static Plugin getInstance() { return getProvidingPlugin(MobAnnihilation.class); }
}
