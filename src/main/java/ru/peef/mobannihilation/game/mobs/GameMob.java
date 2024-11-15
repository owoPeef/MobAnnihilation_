package ru.peef.mobannihilation.game.mobs;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.peef.mobannihilation.game.Arena;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;

import java.util.UUID;

public class GameMob {
    public Entity entity;
    public LivingEntity livingEntity;
    public UUID uniqueId;
    public Arena arena;

    public GameMob(Entity entity, Arena arena) {
        this.entity = entity;
        this.livingEntity = (LivingEntity) entity;

        uniqueId = livingEntity.getUniqueId();

        this.arena = arena;

        if (livingEntity != null) {
            double mobHealth = livingEntity.getMaxHealth() * (this.arena.getLevel() / 2.1);
            livingEntity.setMaxHealth(Math.min(mobHealth, 2048.0));
            livingEntity.setHealth(Math.min(mobHealth, 2048.0));
        }
    }

    public void kill() {
        livingEntity.remove();
        entity.remove();
    }
}
