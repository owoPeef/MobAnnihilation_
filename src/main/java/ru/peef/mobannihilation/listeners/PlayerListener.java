package ru.peef.mobannihilation.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.scheduler.BukkitScheduler;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.Utils;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.items.RarityItem;
import ru.peef.mobannihilation.game.mobs.GameMob;
import ru.peef.mobannihilation.game.npcs.NPC;
import ru.peef.mobannihilation.game.npcs.NPCManager;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerDataHandler;
import ru.peef.mobannihilation.game.players.PlayerManager;

public class PlayerListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage("");

        Player player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(GameManager.BASIC_SPAWN);
        player.setHealth(player.getMaxHealth());
        player.getInventory().clear();

        String textures_url = MobAnnihilation.getConfiguration().getString("options.resource_pack_url");
        if (!textures_url.isEmpty()) player.setResourcePack(textures_url);

        // Когда игрок только зашел, у него появляются все доступные команды
        if (!PlayerDataHandler.hasPlayer(player)) {
            player.sendMessage(
                    ChatColor.GREEN + "Привет! Я для тебя подготовил небольшой гайд по командам... Вкратце:\n" +
                    ChatColor.GOLD + "/game info" + ChatColor.AQUA + " - вывести это сообщение\n" +
                    ChatColor.GOLD + "/game stats" + ChatColor.AQUA + " - получить свою статистику\n" +
                    ((player.isOp() || player.hasPermission("game.stats.other")) ? ChatColor.GOLD + "/game stats <ник>" + ChatColor.AQUA + " - получить статистику игрока\n" : "") +
                    ChatColor.GOLD + "/game join" + ChatColor.AQUA + " - войти на арену\n" +
                    ChatColor.GOLD + "/game leave" + ChatColor.AQUA + " - выйти с арены\n" +
                    ((player.isOp() || player.hasPermission("game.add")) ? ChatColor.GOLD + "/game add_level <число>" + ChatColor.AQUA + " - добавить <число> уровней\n" + ChatColor.GOLD + "/game add_progress <число>" + ChatColor.AQUA + " - добавить <число>% прогресса (от 1 до 100)" : "")
            );
        }

        GamePlayer gamePlayer = GamePlayer.fromFile(player);

        gamePlayer.joinServer();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage("");

        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            gamePlayer.save();

            for (GameMob mob : gamePlayer.mobs) {
                if (mob != null && mob.livingEntity != null && !mob.livingEntity.isDead() && mob.spawnedFor.getName().equals(gamePlayer.getName())) {
                    mob.livingEntity.remove();
                }
            }

            PlayerManager.PLAYERS.removeIf(checkPlayer -> checkPlayer.getPlayer().getName().equals(gamePlayer.getPlayer().getName()));
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getRightClicked().getType().equals(EntityType.VILLAGER)) {
            NPC npc = NPCManager.get(event.getRightClicked().getUniqueId());

            if (npc != null) {
                player.performCommand(npc.executeCommand);
            }
        } else if (event.getRightClicked().getType().equals(EntityType.PLAYER)) {
            Player interactPlayer = (Player) event.getRightClicked();

            GamePlayer interactGamePlayer = PlayerManager.get(interactPlayer);

            if (interactGamePlayer != null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW + "У " + interactGamePlayer.getName() + " имеет " + ChatColor.GOLD + interactGamePlayer.getLevel() + " уровень"));
            }
        } else {
            LivingEntity entity = (LivingEntity) event.getRightClicked();
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + (Utils.roundTo(entity.getHealth(), 1) + "❤/") + (Utils.roundTo(entity.getMaxHealth(), 1) + "❤")));
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);
        ItemStack item = event.getItemDrop().getItemStack();

        if (gamePlayer != null) {
            for (RarityItem rarityItem : gamePlayer.getRarityItems()) {
                if (item.isSimilar(rarityItem.getItemStack())) {
                    gamePlayer.removeItem(rarityItem);
                    event.getItemDrop().remove();

                    event.setCancelled(false);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            event.setFormat(PlaceholderAPI.setPlaceholders(player, MobAnnihilation.getConfiguration().getString("options.chat.gamePlayer_format")).replace('&', ChatColor.COLOR_CHAR));
        } else {
            event.setFormat(PlaceholderAPI.setPlaceholders(player, MobAnnihilation.getConfiguration().getString("options.chat.player_format")).replace('&', ChatColor.COLOR_CHAR));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage("");
        Player player = event.getEntity();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            gamePlayer.mobs.forEach(mob -> {
                if (mob != null && mob.livingEntity != null) {
                    mob.livingEntity.remove();
                }
            });

            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskLater(MobAnnihilation.getInstance(), gamePlayer::kill, 1L);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            event.setCancelled(!gamePlayer.editMode);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            event.setCancelled(!gamePlayer.editMode);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Player player = (Player) event.getInventory().getViewers().get(0);
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            event.setCancelled(!gamePlayer.editMode);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = PlayerManager.get(player);

        if ((event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
        && player.isSneaking()) {
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
        } else {
            if (event.getClickedInventory() != null && event.getAction() != null
                    && !event.getClickedInventory().getType().equals(InventoryType.ANVIL)
                    && !event.getAction().equals(InventoryAction.DROP_ALL_SLOT)
                    && !event.getAction().equals(InventoryAction.DROP_ONE_SLOT)
                    && !event.getAction().equals(InventoryAction.DROP_ALL_CURSOR)
                    && !event.getAction().equals(InventoryAction.DROP_ONE_CURSOR)
            ) event.setCancelled(true);
        }
    }
    @EventHandler public void onFoodChange(FoodLevelChangeEvent event) { event.setCancelled(true); }
    @EventHandler public void onPlayerInteractEntity(PlayerInteractEntityEvent event) { if (event.getRightClicked().getType().equals(EntityType.VILLAGER)) event.setCancelled(true); }
}
