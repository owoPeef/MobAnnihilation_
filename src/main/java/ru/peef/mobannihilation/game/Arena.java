package ru.peef.mobannihilation.game;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitScheduler;
import ru.peef.mobannihilation.MobAnnihilation;
import ru.peef.mobannihilation.game.mobs.GameMob;
import ru.peef.mobannihilation.game.players.GamePlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Arena {
    public File file;
    public String arenaName;
    public int spawnX, spawnY, spawnZ;
    public int mobSpawnX, mobSpawnY, mobSpawnZ;
    public World world;
    public Operation operation;
    public List<GameMob> MOBS = new ArrayList<>();
    public List<GamePlayer> arenaPlayers = new ArrayList<>();
    public List<GamePlayer> spectatePlayers = new ArrayList<>();
    EditSession editSession;
    int arenaIndex = -1;
    int arenaOffset = 50;

    public boolean isStarted = false, startCountdown = false;

    public Arena(World world, String name, int spawnX, int spawnY, int spawnZ, int mobSpawnX, int mobSpawnY, int mobSpawnZ) {
        this.world = world;
        this.arenaName = name;
        file = new File(WorldEditPlugin.getPlugin(WorldEditPlugin.class).getDataFolder() + File.separator + "schematics" + File.separator + arenaName + ".schematic");

        int i = 1;
        for (Arena arena : GameManager.ARENA_LIST) {
            if (arena.arenaIndex != i) {
                arenaIndex = i;
                break;
            }
            i++;
        }

        if (arenaIndex == -1) arenaIndex = GameManager.ARENA_LIST.size()+1;

        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;

        this.mobSpawnX = mobSpawnX;
        this.mobSpawnY = mobSpawnY;
        this.mobSpawnZ = mobSpawnZ;

        GameManager.ARENA_LIST.add(this);
    }

    public void add(GamePlayer... players) { arenaPlayers.addAll(Arrays.asList(players)); }
    public Location getPlayerSpawn() {
        int spawnZ = this.spawnZ + Math.max(arenaOffset, arenaIndex * arenaOffset);
        return new Location(world, spawnX+.5f, spawnY, spawnZ+.5f, -90, 0);
    }

    public Location getMobSpawn() {
        int mobSpawnZ = this.mobSpawnZ + Math.max(arenaOffset, arenaIndex * arenaOffset);
        return new Location(world, mobSpawnX+.5f, mobSpawnY, mobSpawnZ+.5f);
    }

    public void load(int x, int y, int z) {
        if (!file.exists()) {
            MobAnnihilation.getInstance().getLogger().info("File (" + file.getAbsolutePath() + ") not found!");
            return;
        }

        z += Math.max(arenaOffset, arenaIndex * arenaOffset);

        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormat.findByFile(file);
        if (format != null) {
            try {
                com.sk89q.worldedit.world.World editWorld = BukkitUtil.getLocalWorld(world);
                WorldData worldData = editWorld.getWorldData();

                ClipboardReader reader = format.getReader(Files.newInputStream(file.toPath()));
                clipboard = reader.read(worldData);
                editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(editWorld, -1);
                operation = new ClipboardHolder(clipboard, worldData)
                        .createPaste(editSession.getSurvivalExtent(), worldData)
                        .to(Vector.toBlockPoint(x, y, z))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);

                MobAnnihilation.getInstance().getLogger().info(String.format("Set schematic on %s %s %s", x, y, z));

                arenaPlayers.forEach(gamePlayer -> {
                    int spawnZ = this.spawnZ + Math.max(arenaOffset, arenaIndex * arenaOffset);
                    gamePlayer.getPlayer().teleport(new Location(world, spawnX+.5f, spawnY, spawnZ+.5f, -90, 0));
                });
            } catch (WorldEditException | IOException e) {
                MobAnnihilation.getInstance().getLogger().info(e.getMessage());
            }
        }
    }

    // TODO: отгрузку арен
    public void unload() {
        if (arenaPlayers.isEmpty()) {
            GameManager.ARENA_LIST.remove(this);
            if (editSession != null) {
                editSession.undo(editSession);
            }

            MOBS.forEach(GameMob::kill);
        }
    }

    public void start() {
        startCountdown = true;

        BukkitScheduler scheduler = Bukkit.getScheduler();
        int WAIT_SECONDS = MobAnnihilation.getConfiguration().getInt("options.arena_start_seconds");

        for (int i = WAIT_SECONDS; i >= 0; i--) {
            int secondsLeft = i;
            scheduler.runTaskLater(MobAnnihilation.getInstance(), () -> {
                if (secondsLeft == 0) {
                    isStarted = true;
                    spawnMobs();
                } else {
                    playSound(Sound.UI_BUTTON_CLICK);
                    sendMessage("&bНачало через &6" + secondsLeft + " сек.");
                }
            }, (WAIT_SECONDS - secondsLeft) * 20L);
        }
    }

    public void spawnMobs() {
        for (int i = 0; i < 3 * getPlayers().size(); i++) {
            EntityType entityType = EntityType.ZOMBIE;
            double chance = Math.random() * 10;
            if (chance <= 4.2) entityType = EntityType.PIG_ZOMBIE;
            spawnMob(entityType);
        }
    }

    public void spawnMob(EntityType type) {
        Entity entity = GameManager.ARENA_WORLD.spawnEntity(getMobSpawn(), type);
        GameMob mob = new GameMob(entity, this);
        MOBS.add(mob);
        GameManager.SPAWNED_ENTITIES.add(mob.livingEntity);
    }

    public GameMob getMob(UUID uniqueId) {
        for (GameMob mob : MOBS) {
            if (mob.uniqueId.equals(uniqueId)) return mob;
        }
        return null;
    }

    public boolean hasAliveMobs() {
        for (GameMob mob : MOBS) {
            if (mob.livingEntity != null && !mob.livingEntity.isDead()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMob(UUID uniqueId) {
        for (GameMob mob : MOBS) {
            if (mob.livingEntity != null && !mob.livingEntity.isDead() && mob.uniqueId.equals(uniqueId)) return true;
        }
        return false;
    }

    public int getLevel() {
        AtomicInteger levels = new AtomicInteger();
        arenaPlayers.forEach(gamePlayer -> levels.addAndGet(gamePlayer.getLevel()));
        return levels.get() / arenaPlayers.size();
    }

    public boolean hasPlayer(String nick) {
        boolean s = false;

        for (GamePlayer gamePlayer : arenaPlayers) {
            if (gamePlayer.getName().equals(nick)) {
                s = true;
                break;
            }
        }

        return s;
    }
    public int getId() { return arenaIndex; }
    public String getState() { return isStarted ? ChatColor.GREEN + "играют" : ChatColor.RED + "ожидание"; }
    public List<GamePlayer> getAllPlayers() {
        List<GamePlayer> players = new ArrayList<>();

        players.addAll(spectatePlayers);
        players.addAll(arenaPlayers);

        return players;
    }
    public List<GamePlayer> getPlayers() { return arenaPlayers; }
    public List<GamePlayer> getSpectators() { return spectatePlayers; }
    public int getOwnerLevel() {
        if (arenaPlayers.get(0) == null) return 1;
        return arenaPlayers.get(0).getLevel();
    }

    public void playSound(Sound sound) { getAllPlayers().forEach(player -> player.getPlayer().playSound(player.getPlayer().getLocation(), sound, 1f, 1f));}
    public void sendMessage(String message) { getAllPlayers().forEach(player -> player.getPlayer().sendMessage(message.replace('&', ChatColor.COLOR_CHAR))); }

    public void checkAll() {
        boolean canStart = true;

        for (GamePlayer gamePlayer : getPlayers()) {
            if (gamePlayer.getPlayer().getLocation().getY() > 10) {
                canStart = false;
                break;
            }
        }

        if (canStart && !startCountdown) start();
    }
}
