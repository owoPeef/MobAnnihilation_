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
        if (event.getDamager().getType().equals(EntityType.PLAYER) && (event.getEntityType().equals(EntityType.VILLAGER) || event.getEntityType().equals(EntityType.PLAYER))) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType().equals(EntityType.PLAYER) && !event.getEntityType().equals(EntityType.VILLAGER)) {
            Player damager = (Player) event.getDamager();
            GamePlayer gDamager = PlayerManager.get(damager);

            if (gDamager != null) {
                if (!gDamager.getRarityItems().isEmpty()) {
                    double itemBaseDamage = 0;
                    double damage = event.getFinalDamage();
                    double damagePercent = damage / 100f;

                    List<RarityItem> damageItems = gDamager.getRarityItems(RarityItem.Boost.DAMAGE);

                    double result = 100.0;
                    for (RarityItem rarityItem : damageItems) {
                        result *= (1 + rarityItem.boostPercent / 100);
                        itemBaseDamage += rarityItem.baseValue;
                    }
                    result -= 100;

                    double total = Math.abs(itemBaseDamage + damage) + (damagePercent * result) * (Math.pow(gDamager.rebithCount, 1.8f) * gDamager.X_damageBoost);
                    event.setDamage(total);
                }

                LivingEntity entity = (LivingEntity) event.getEntity();
                if (entity.getHealth() - event.getFinalDamage() <= 0) gDamager.killMob(entity);
            }
        }
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByEntityEvent event) {
        if (event.getEntityType().equals(EntityType.PLAYER)) {
            GamePlayer gamePlayer = PlayerManager.get((Player) event.getEntity());

            if (gamePlayer != null) {
                double scalingFactor = MobAnnihilation.getConfiguration().getDouble("options.mobs.damage_scaling_factor");

                double entityMaxHealth = ((LivingEntity) event.getDamager()).getMaxHealth();
                double entityHealthPercent = entityMaxHealth / 100.0;

                double baseDamage = event.getDamage();
                double damage = Utils.roundTo(baseDamage * Math.pow(scalingFactor, entityHealthPercent * 10) + baseDamage * 0.1 * (entityHealthPercent * 10), 2);

                if (!gamePlayer.getRarityItems(RarityItem.Boost.PROTECTION).isEmpty()) {
                    double damagePercent = damage / 100f;

                    List<RarityItem> protectionItems = gamePlayer.getRarityItems(RarityItem.Boost.PROTECTION);

                    double result = 100.0;
                    for (RarityItem rarityItem : protectionItems) result *= (1 + rarityItem.boostPercent / 100);
                    result -= 100;
                    result /= (gamePlayer.getLevel() / 3.3f);

                    damage -= (damagePercent * result);
                }

                event.setDamage(damage);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType().equals(EntityType.PLAYER) && event.getCause().equals(EntityDamageEvent.DamageCause.FALL))
            event.setCancelled(true);
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
}
