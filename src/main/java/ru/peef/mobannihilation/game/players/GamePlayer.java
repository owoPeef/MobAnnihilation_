package ru.peef.mobannihilation.game.players;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.ScoreboardUtils;
import ru.peef.mobannihilation.Utils;
import ru.peef.mobannihilation.game.Arena;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.items.RarityItem;
import ru.peef.mobannihilation.game.mobs.GameMob;
import ru.peef.mobannihilation.handlers.PlayerDataHandler;

import java.util.ArrayList;
import java.util.List;

public class GamePlayer {
    private final Player player;
    private final String name;
    public double level;
    public int gold;
    public int rebithCount;
    public int mobKilled;
    // Donate
    public int X_damageBoost = 2;
    private final List<RarityItem> items = new ArrayList<>();
    public boolean onArena = false, isSpectate = false;
    public String lastProg = "";
    public boolean editMode = false;

    public int maxItemsCount;

    public GamePlayer(Player player, Double level, Integer mobKilled, Integer gold, Integer rebithCount) {
        this.player = player;
        this.name = player.getName();
        this.level = level;
        this.mobKilled = mobKilled;
        this.gold = gold;
        this.rebithCount = rebithCount;

        updateProgress();
    }

    public GamePlayer(String name, Double level, Integer mobKilled, Integer gold, Integer rebithCount) {
        this.name = name;
        this.player = Bukkit.getPlayer(this.name);
        this.level = level;
        this.mobKilled = mobKilled;
        this.gold = gold;
        this.rebithCount = rebithCount;

        updateProgress();
    }

    public GamePlayer(String name, Double level, Integer mobKilled, Integer gold, Integer rebithCount, List<RarityItem> items) {
        this.name = name;
        this.player = Bukkit.getPlayer(this.name);
        this.level = level;
        this.mobKilled = mobKilled;
        this.gold = gold;
        this.rebithCount = rebithCount;

        items.forEach(item -> addItem(item, false));
        updateProgress();
    }

    public String getName() { return player == null ? name : player.getName(); }
    public Player getPlayer() { return player; }
    public int getLevel() { return (int) Math.floor(level); }
    public int getRebithLevel() { return getLevel() * rebithCount; }
    public int getProgress() { return (int) Math.floor((level - Math.floor(level)) * 100); }

    public void save() { PlayerDataHandler.savePlayer(this); }
    public void addLevel(int level) { addProgress(level * 100); }
    public void addProgress(double prog) {
        int lvl = getLevel();
        level += prog / 100;

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.format("%sПрогресс: %s%s %s(+%s)", ChatColor.AQUA + (ChatColor.BOLD).toString(), ChatColor.GOLD+ (ChatColor.BOLD).toString(), getProgress() + "%", ChatColor.GREEN+ (ChatColor.BOLD).toString(), Utils.roundTo(prog, 1) + "%")));
        if (getLevel() > lvl) {
            player.sendTitle(ChatColor.GOLD + (ChatColor.BOLD + "НОВЫЙ УРОВЕНЬ!"), ChatColor.AQUA + "Вы достигли " + ChatColor.GOLD + getLevel() + " уровня", 5, 20, 5);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            setSword();
        }

        updateProgress();

        lastProg = ChatColor.GREEN + "(+" + (int)prog + "%)";
    }

    public void setLevel(int level) { setProgress(level * 100); }
    public void setProgress(double prog) {
        level -= prog / 100;
        updateProgress();
        lastProg = ChatColor.GREEN + "(+" + (int)prog + "%)";
    }

    public void reduceProgress(double prog) {
        level -= prog / 100;
        level = Math.max(level, 1.0);
        updateProgress();
        lastProg = ChatColor.RED + "(-" + (int)prog + "%)";
    }

    public void updateProgress() {
        player.setLevel(getLevel());

        float experience = getProgress() / 100.0f;
        player.setExp(experience);

        setSword();
        save();
    }

    public void setSword() {
        int currentLevel = getLevel();
        ItemStack sword = new ItemStack((currentLevel < 5 ? Material.WOOD_SWORD : currentLevel < 25 ? Material.STONE_SWORD : currentLevel < 60 ? Material.IRON_SWORD : Material.DIAMOND_SWORD), 1);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            swordMeta.setDisplayName(ChatColor.GOLD + "Перчатка для Битья");
            swordMeta.setUnbreakable(true);

            if (sword.getType().equals(Material.DIAMOND_SWORD)) {
                swordMeta.addEnchant(Enchantment.DAMAGE_ALL, Math.min((currentLevel - 55) / 5, 10), true);
            }

            sword.setItemMeta(swordMeta);
        }
        player.getInventory().setItem(0, sword);
    }

    public void setPotions() {
        player.removePotionEffect(PotionEffectType.SPEED);
        if (!getRarityItems(RarityItem.Boost.SPEED).isEmpty()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, ((int) getRarityPercent(RarityItem.Boost.SPEED))-1, false));
        }
    }

    public void joinServer() {
        updateProgress();
        PlayerManager.PLAYERS.add(this);
        getPlayer().getInventory().setHeldItemSlot(0);

        setPotions();
        ScoreboardUtils.updateScoreboard(this);
    }

    public Arena arena = null;

    public void joinArena() {
        if (isSpectate) stopSpectate();

        if (!onArena) {
            onArena = true;

            player.sendMessage(ChatColor.GREEN + "Вы были телепортированы на арену!");
            player.sendMessage(ChatColor.YELLOW + (ChatColor.BOLD + "ПОМНИТЕ!") + ChatColor.AQUA + " Для выхода из арены: " + ChatColor.GOLD + "/game leave");

            arena = new Arena(GameManager.ARENA_WORLD,
                    "mainArena",
                    5, 18, 40,
                    21, 7, 40
            );
            arena.add(this);
            arena.load(0, 25, 50);

            player.teleport(arena.getPlayerSpawn());
        } else {
            player.sendMessage(ChatColor.RED + "Ты уже на арене!");
        }
    }

    public void joinArena(Arena arena) {
        if (isSpectate) stopSpectate();

        if (!onArena) {
            onArena = true;

            player.teleport(arena.getPlayerSpawn());

            player.sendMessage(ChatColor.GREEN + "Вы были телепортированы на арену!");
            player.sendMessage(ChatColor.YELLOW + (ChatColor.BOLD + "ПОМНИТЕ!") + ChatColor.AQUA + " Для выхода из арены: " + ChatColor.GOLD + "/game leave");

            this.arena = arena;
            this.arena.add(this);
        } else {
            player.sendMessage(ChatColor.RED + "Ты уже на арене!");
        }
    }

    public void leaveArena(boolean sendMessages) {
        if (onArena) {
            onArena = false;

            player.teleport(GameManager.BASIC_SPAWN);
            player.setHealth(player.getMaxHealth());

            if (sendMessages) player.sendMessage(ChatColor.AQUA + "Вы вышли с арены!");

            setPotions();
            updateProgress();

            arena.getPlayers().remove(this);
            arena.unload();
            arena = null;
        } else if (sendMessages) {
            player.sendMessage(ChatColor.RED + "Ты не находишься на арене!");
        }
    }

    public void startSpectate(Arena arena) {
        if (onArena) leaveArena(false);
        if (isSpectate) stopSpectate();

        this.arena = arena;
        this.arena.getSpectators().add(this);
        this.arena.getPlayers().forEach(arenaPlayer -> arenaPlayer.getPlayer().hidePlayer(MobAnnihilation.getInstance(), player));

        player.sendMessage(ChatColor.AQUA + "Тсс... Вы начали наблюдать за Ареной #" + arena.getId());
        player.teleport(arena.getPlayerSpawn());
        player.setGameMode(GameMode.SPECTATOR);

        isSpectate = true;
    }

    public void stopSpectate() {
        if (isSpectate) {
            player.teleport(GameManager.BASIC_SPAWN);
            player.setGameMode(GameMode.ADVENTURE);

            this.arena.getSpectators().remove(this);
            this.arena.getAllPlayers().forEach(arenaPlayer -> arenaPlayer.getPlayer().showPlayer(MobAnnihilation.getInstance(), player));
            this.arena = null;

            isSpectate = false;
        }
    }

    public void addItem(RarityItem item, boolean announce) {
        maxItemsCount = MobAnnihilation.getConfiguration().getInt("options.players_rarity_items_count");
        if (items.size() < maxItemsCount) {
            ItemStack itemStack = new ItemStack(Material.EMERALD, 1);
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                List<String> lore = new ArrayList<>();
                itemMeta.setDisplayName(ChatColor.RESET + (ChatColor.GOLD + item.getTitle()));

                lore.add(ChatColor.GREEN + "Редкость: " + item.getRarityString() + ChatColor.GRAY + " (" + item.boostPercent + (item.boostIsPercent ? "%" : "") + ")");
                if (item.getChance() != 0) lore.add(ChatColor.GREEN + "Шанс выпадения: " + ChatColor.GOLD + Utils.roundTo(item.getChance(), 2) + "%");
                lore.add(ChatColor.GRAY + item.getDescription());

                itemMeta.setLore(lore);
            }

            itemStack.setItemMeta(itemMeta);
            item.setItemStack(itemStack);

            if (announce) {
                TextComponent message = new TextComponent(ChatColor.AQUA + "Получен предмет: ");
                TextComponent hoverMessage = new TextComponent(ChatColor.GOLD + "[" + item.getTitle() + "]");
                hoverMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GOLD + item.getTitle() + "\n" +
                        ChatColor.GREEN + "Редкость: " + item.getRarityString() + ChatColor.GRAY + " (" + item.boostPercent + (item.boostIsPercent ? "%" : "") + ")" + "\n" +
                        ChatColor.AQUA + item.getDescription()
                ).create()));
                message.addExtra(hoverMessage);
                player.spigot().sendMessage(message);
            }

            items.add(item);
            setPotions();
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Инвентарь заполнен!"));
        }
    }

    public void removeItem(RarityItem rarityItem) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.AQUA + "Вы выбросили " + ChatColor.GOLD + "[" + rarityItem.getTitle() + "]"));
        items.remove(rarityItem);
        player.closeInventory();
        openRuneInventory();
        setPotions();
    }

    public List<RarityItem> getRarityItems() { return items; }
    public List<RarityItem> getRarityItems(RarityItem.Boost boostType) {
        List<RarityItem> rarityItems = new ArrayList<>();
        items.forEach(item -> { if (item.boost.equals(boostType)) rarityItems.add(item); });
        return rarityItems;
    }

    public float getRarityPercent(RarityItem.Boost type) {
        float value = 0;
        for (RarityItem protectionItem : getRarityItems(type)) {
            value += protectionItem.boostPercent;
        }
        return value;
    }

    public void openRuneInventory() {
        Inventory inventory = Bukkit.createInventory(null, 9, ChatColor.GOLD + (ChatColor.BOLD + "Ваши Руны"));
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i).getItemStack());
        }
        this.player.openInventory(inventory);
    }

    public void kill() {
        player.spigot().respawn();
        player.teleport(GameManager.BASIC_SPAWN);
        player.setGameMode(GameMode.ADVENTURE);

        int minProgressReduce = MobAnnihilation.getConfiguration().getInt("options.game_process.death_min_reduce_progress");
        int maxProgressReduce = MobAnnihilation.getConfiguration().getInt("options.game_process.death_max_reduce_progress");

        int progress = (int) Math.round(minProgressReduce + Math.random() * (maxProgressReduce - minProgressReduce));
        reduceProgress(progress);
        leaveArena(false);

        player.setMaxHealth(20 + getHealth());
        player.setHealth(player.getMaxHealth());

        player.sendMessage(String.format(ChatColor.RED + "Вы умерли! Из-за этого вы потеряли: %s%s", ChatColor.GOLD, progress + " опыта"));
    }

    public void killMob(LivingEntity entity) {
        float baseProgress = 22;
        addProgress((baseProgress / getLevel()) * (rebithCount * 1.3f));
        player.setMaxHealth(20 + getHealth());
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 5), true);

        RarityItem dropItem = RarityItem.getRandom(this);
        if (dropItem.getChance() <= RarityItem.getRandomPercent(0f, 100f)) {
            addItem(dropItem, true);
        }

        double GOLD_MIN = 33 * (rebithCount / 1.5f);
        double GOLD_MAX = 69 * (rebithCount / 1.6f);
        gold += (int) (GOLD_MIN + Math.random() * (GOLD_MAX - GOLD_MIN));

        GameMob killedMob = arena.getMob(entity.getUniqueId());
        if (killedMob != null) {
            arena.MOBS.removeIf(mob -> mob.uniqueId.equals(killedMob.uniqueId));
            if (!arena.hasAliveMobs()) {
                Bukkit.getScheduler().runTaskLater(MobAnnihilation.getInstance(), arena::spawnMobs, 10L);
            }
        }

        mobKilled++;
    }

    public static GamePlayer fromFile(String name) {
        PlayerData playerData = PlayerDataHandler.getPlayerData(name);

        if (playerData != null) return new GamePlayer(name, Math.max(1.0, playerData.level), playerData.mobKilled, Math.max(0, playerData.gold), playerData.rebithCount, playerData.rarity_items);
        else return new GamePlayer(name, 1.0, 0, 0, 1);
    }
    public static GamePlayer fromFile(Player player) { return fromFile(player.getName()); }

    public double getHealth() {
        if (getLevel() >= 20) {
            return (getLevel()-20.0) / 5;
        }
        return 0;
    }

    public int getLevelForRebith() {
        return (int) (20 * Math.pow(rebithCount, 1.1f));
    }

    public void rebith() {
        if (getLevel() >= getLevelForRebith()) {
            rebithCount++;
            player.sendMessage(ChatColor.AQUA + "Готово! Для следующего ребитха необходим: " + ChatColor.GOLD + getLevelForRebith() + " ур.");

            gold = 0;
            level = 1.0;
        } else {
            player.sendMessage(ChatColor.RED + "Ваш уровень слишком мал! Для следующего ребитха нужен: " + ChatColor.GOLD + getLevelForRebith() + " ур.");
        }

        updateProgress();
    }
}
