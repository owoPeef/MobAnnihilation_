package ru.peef.mobannihilation.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.game.GameManager;

public class WorldListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        MobAnnihilation.getInstance().getLogger().info("[WORLD_UNLOAD] Try to deleted entities: " + GameManager.SPAWNED_ENTITIES.size());
        for (LivingEntity entity : GameManager.SPAWNED_ENTITIES) {
            entity.remove();
        }
    }
}
