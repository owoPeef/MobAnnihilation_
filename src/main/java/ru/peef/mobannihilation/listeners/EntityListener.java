package ru.peef.mobannihilation.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.Utils;
import ru.peef.mobannihilation.game.items.RarityItem;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;

import java.util.List;

public class EntityListener implements Listener {

    @EventHandler
    public void onPlayerDamagePlayerOrVillager(EntityDamageByEntityEvent event) {
        if (isPlayer(event.getDamager()) && isVillagerOrPlayer(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!isPlayer(event.getDamager()) || isVillager(event.getEntity())) {
            return;
        }

        Player damager = (Player) event.getDamager();
        GamePlayer gDamager = PlayerManager.get(damager);
        if (gDamager == null) {
            return;
        }

        double totalDamage = calculateDamageWithItems(event.getFinalDamage(), gDamager);
        event.setDamage(totalDamage);

        handleEntityDeath((LivingEntity) event.getEntity(), gDamager, event.getFinalDamage());
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByEntityEvent event) {
        if (!isPlayer(event.getEntity())) {
            return;
        }

        GamePlayer gamePlayer = PlayerManager.get((Player) event.getEntity());
        if (gamePlayer == null) {
            return;
        }

        double damage = calculateMobDamage(event, gamePlayer);
        event.setDamage(damage);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isPlayer(event.getEntity()) && event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();

        if (entity.getType() == EntityType.ZOMBIE ||
                entity.getType() == EntityType.SKELETON ||
                entity.getType() == EntityType.HUSK ||
                entity.getType() == EntityType.STRAY) {

            event.setCancelled(true);
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity.getType().equals(EntityType.PLAYER);
    }

    private boolean isVillager(Entity entity) {
        return entity.getType().equals(EntityType.VILLAGER);
    }

    private boolean isVillagerOrPlayer(Entity entity) {
        return isPlayer(entity) || isVillager(entity);
    }

    private double calculateDamageWithItems(double baseDamage, GamePlayer gDamager) {
        List<RarityItem> damageItems = gDamager.getRarityItems(RarityItem.Boost.DAMAGE);
        if (damageItems.isEmpty()) {
            return baseDamage;
        }

        double itemBaseDamage = damageItems.stream()
                .mapToDouble(item -> item.baseValue)
                .sum();

        double boostMultiplier = damageItems.stream()
                .mapToDouble(item -> 1 + item.boostPercent / 100)
                .reduce(1, (a, b) -> a * b);

        double damagePercent = baseDamage / 100f;
        double rebirthBoost = Math.pow(gDamager.rebithCount, 1.8) * gDamager.X_damageBoost;

        return Math.abs(itemBaseDamage + baseDamage) + (damagePercent * (boostMultiplier - 1) * rebirthBoost);
    }

    private void handleEntityDeath(LivingEntity entity, GamePlayer gDamager, double finalDamage) {
        if (entity.getHealth() - finalDamage <= 0) {
            gDamager.killMob(entity);
        }
    }

    private double calculateMobDamage(EntityDamageByEntityEvent event, GamePlayer gamePlayer) {
        double scalingFactor = MobAnnihilation.getConfiguration().getDouble("options.mobs.damage_scaling_factor");
        LivingEntity damager = (LivingEntity) event.getDamager();

        double entityHealthPercent = damager.getMaxHealth() / 100.0;
        double baseDamage = event.getDamage();
        double damage = Utils.roundTo(
                baseDamage * Math.pow(scalingFactor, entityHealthPercent * 10) +
                        baseDamage * 0.1 * (entityHealthPercent * 10), 2
        );

        List<RarityItem> protectionItems = gamePlayer.getRarityItems(RarityItem.Boost.PROTECTION);
        if (!protectionItems.isEmpty()) {
            double damagePercent = damage / 100f;

            double protectionBoost = protectionItems.stream()
                    .mapToDouble(item -> 1 + item.boostPercent / 100)
                    .reduce(1, (a, b) -> a * b) - 1;

            protectionBoost /= (gamePlayer.getLevel() / 3.3f);
            damage -= (damagePercent * protectionBoost);
        }

        return damage;
    }
}
