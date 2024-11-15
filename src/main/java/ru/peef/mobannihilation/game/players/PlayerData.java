package ru.peef.mobannihilation.game.players;

import ru.peef.mobannihilation.game.items.RarityItem;

import java.util.List;

public class PlayerData {
    public double level;
    public int gold;
    public int mobKilled;
    public int rebithCount;
    public List<RarityItem> rarity_items;

    public PlayerData(int gold, int mobKilled, double level, int rebithCount, List<RarityItem> rarity_items) {
        this.gold = gold;
        this.level = level;
        this.mobKilled = mobKilled;
        this.rebithCount = rebithCount;
        this.rarity_items = rarity_items;
    }

    public int getLevel() { return (int) Math.floor(level); }

    public static PlayerData create(GamePlayer gamePlayer) {
        return new PlayerData(gamePlayer.gold, gamePlayer.mobKilled, gamePlayer.level, gamePlayer.rebithCount, gamePlayer.getRarityItems());
    }
}
