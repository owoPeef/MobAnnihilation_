package ru.peef.mobannihilation.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

public class WorldListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldSave(WorldSaveEvent event) {
        event.getWorld().getEntities().forEach(Entity::remove);
    }
}
