package ru.peef.mobannihilation.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.items.RarityItem;
import ru.peef.mobannihilation.game.npcs.NPC;
import ru.peef.mobannihilation.game.npcs.NPCManager;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("");
        Player player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(GameManager.BASIC_SPAWN);
        player.getInventory().clear();

        String resourcePackUrl = MobAnnihilation.getConfiguration().getString("options.resource_pack_url");
        if (resourcePackUrl != null && !resourcePackUrl.isEmpty()) {
            player.setResourcePack(resourcePackUrl);
        }

        GamePlayer gamePlayer = GamePlayer.fromFile(player);
        gamePlayer.joinServer();
        player.setMaxHealth(20 + gamePlayer.getHealth());
        player.setHealth(player.getMaxHealth());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage("");
        Player player = event.getPlayer();

        GamePlayer gamePlayer = PlayerManager.get(player);
        if (gamePlayer == null) return;

        gamePlayer.save();
        if (gamePlayer.onArena) gamePlayer.leaveArena(false);
        if (gamePlayer.isSpectate) gamePlayer.stopSpectate();

        PlayerManager.PLAYERS.removeIf(gp -> gp.getPlayer().getName().equals(gamePlayer.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage("");
        GamePlayer gamePlayer = PlayerManager.get(event.getEntity());
        if (gamePlayer != null) {
            Bukkit.getScheduler().runTaskLater(MobAnnihilation.getInstance(), gamePlayer::kill, 1L);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (clickedEntity.getType() == EntityType.VILLAGER) {
            Villager villager = (Villager) clickedEntity;
            NPC npc = NPCManager.get(villager.getUniqueId());
            if (npc != null) player.performCommand(npc.executeCommand);

        } else if (clickedEntity.getType() == EntityType.PLAYER) {
            Player targetPlayer = (Player) clickedEntity;
            GamePlayer interactGamePlayer = PlayerManager.get(targetPlayer);
            if (interactGamePlayer != null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.YELLOW + "У " + interactGamePlayer.getName() + " " + ChatColor.GOLD + interactGamePlayer.getLevel() + " уровень"));
            }
        }
    }


    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItemDrop().getItemStack();
        for (RarityItem rarityItem : gamePlayer.getRarityItems()) {
            if (item.isSimilar(rarityItem.getItemStack())) {
                gamePlayer.removeItem(rarityItem);
                event.getItemDrop().remove();
                event.setCancelled(false);
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        GameManager.playerChat(event.getPlayer(), event.getMessage());
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        GamePlayer gamePlayer = PlayerManager.get(event.getPlayer());
        if (gamePlayer != null && gamePlayer.arena != null) {
            gamePlayer.arena.checkAll();
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        GamePlayer gamePlayer = PlayerManager.get(event.getPlayer());
        event.setCancelled(gamePlayer == null || !gamePlayer.editMode);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        GamePlayer gamePlayer = PlayerManager.get(event.getPlayer());
        event.setCancelled(gamePlayer == null || !gamePlayer.editMode);
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Player player = (Player) event.getInventory().getViewers().get(0);
        GamePlayer gamePlayer = PlayerManager.get(player);
        event.setCancelled(gamePlayer == null || !gamePlayer.editMode);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && player.isSneaking()) {
            GamePlayer gamePlayer = PlayerManager.get(player);
            if (gamePlayer != null) {
                event.setCancelled(true);
                gamePlayer.openRuneInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null && gamePlayer.editMode) {
            event.setCancelled(false);
            return;
        }

        if (event.getClickedInventory() != null && event.getAction() != null &&
                event.getClickedInventory().getType() != InventoryType.ANVIL &&
                event.getAction() != InventoryAction.DROP_ALL_SLOT &&
                event.getAction() != InventoryAction.DROP_ONE_SLOT &&
                event.getAction() != InventoryAction.DROP_ALL_CURSOR &&
                event.getAction() != InventoryAction.DROP_ONE_CURSOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Villager) {
            event.setCancelled(true);
        }
    }
}
