package ru.peef.mobannihilation.game;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.ScoreboardUtils;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerData;
import ru.peef.mobannihilation.handlers.PlayerDataHandler;
import ru.peef.mobannihilation.game.players.PlayerManager;

import java.util.*;

public class GameManager {
    public static World BASIC_WORLD, ARENA_WORLD;
    public static Location BASIC_SPAWN, ARENA_SPAWN;
    public static int SHOW_TOP_PLAYERS_COUNT;
    public static int scoreboardUpdateSeconds;

    public static List<LivingEntity> SPAWNED_ENTITIES = new ArrayList<>();

    public static List<Arena> ARENA_LIST = new ArrayList<>();

    public static void init() {
        BASIC_WORLD = Bukkit.getWorld(MobAnnihilation.getConfiguration().getString("worlds.lobby.world_name"));
        ARENA_WORLD = Bukkit.createWorld(WorldCreator.name(MobAnnihilation.getConfiguration().getString("worlds.arena.world_name")));

        BASIC_SPAWN = new Location(BASIC_WORLD, MobAnnihilation.getConfiguration().getDouble("worlds.lobby.spawnX"), MobAnnihilation.getConfiguration().getDouble("worlds.lobby.spawnY"), MobAnnihilation.getConfiguration().getDouble("worlds.lobby.spawnZ"));
        ARENA_SPAWN = new Location(ARENA_WORLD, MobAnnihilation.getConfiguration().getDouble("worlds.arena.spawnX"), MobAnnihilation.getConfiguration().getDouble("worlds.arena.spawnY"), MobAnnihilation.getConfiguration().getDouble("worlds.arena.spawnZ"), -90, 0);

        scoreboardUpdateSeconds = MobAnnihilation.getConfiguration().getInt("options.scoreboard_update_seconds");
        SHOW_TOP_PLAYERS_COUNT = MobAnnihilation.getConfiguration().getInt("options.players_top_count");

        Bukkit.getScheduler().runTaskTimer(MobAnnihilation.getInstance(), () -> PlayerManager.PLAYERS.forEach(ScoreboardUtils::updateScoreboard), 0, scoreboardUpdateSeconds * 20L);
    }

    public static Map<String, PlayerData> getTopByLevel() {
        Map<String, PlayerData> players = PlayerDataHandler.loadPlayers();
        if (!players.isEmpty()) {
            List<Map.Entry<String, PlayerData>> entries = new ArrayList<>(players.entrySet());
            entries.sort((entry1, entry2) -> Double.compare(entry2.getValue().level, entry1.getValue().level));
            Map<String, PlayerData> sortedPlayers = new LinkedHashMap<>();
            for (Map.Entry<String, PlayerData> entry : entries) {
                sortedPlayers.put(entry.getKey(), entry.getValue());
            }

            return sortedPlayers;
        } else {
            return new HashMap<>();
        }
    }

    public static List<GamePlayer> getArenaPlayers() {
        List<GamePlayer> players = new ArrayList<>();
        ARENA_LIST.forEach(arena -> players.addAll(arena.arenaPlayers));
        return players;
    }

    public static void playerChat(Player player, String message) {
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            if (gamePlayer.arena != null && !gamePlayer.isSpectate) {
                gamePlayer.arena.sendMessage(PlaceholderAPI.setPlaceholders(
                        player,
                        MobAnnihilation.getConfiguration().getString("options.chat.game.arena_format")
                ).replace("%player_name%", player.getName()).replace("%message%", message).replace('&', ChatColor.COLOR_CHAR));
            } else {
                Bukkit.broadcastMessage(PlaceholderAPI.setPlaceholders(player,
                        MobAnnihilation.getConfiguration().getString("options.chat.game.global_format")).replace("%player_name%", player.getName()).replace("%message%", message).replace('&', ChatColor.COLOR_CHAR));
            }
        } else {
            Bukkit.broadcastMessage(PlaceholderAPI.setPlaceholders(player,
                    MobAnnihilation.getConfiguration().getString("options.chat.player_format")).replace("%player_name%", player.getName()).replace("%message%", message).replace('&', ChatColor.COLOR_CHAR));
        }
    }
}
