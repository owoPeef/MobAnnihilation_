package ru.peef.mobannihilation.game.players;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    public static List<GamePlayer> PLAYERS = new ArrayList<>();

    public static GamePlayer get(Player player) {
        return PLAYERS.stream()
                .filter((pl) -> pl.getPlayer().getName().equals(player.getName()))
                .findAny()
                .orElse(null);
    }
}
