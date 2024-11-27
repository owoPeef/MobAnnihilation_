package ru.peef.mobannihilation.game;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Arena {
    private static final int ARENA_OFFSET = 50;

    public File file;
    public String arenaName;
    public int spawnX, spawnY, spawnZ;
    public int mobSpawnX, mobSpawnY, mobSpawnZ;
    public World world;
    public Operation operation;
    public List<GameMob> MOBS = new ArrayList<>();
    private final List<GamePlayer> arenaPlayers = new ArrayList<>();
    private final List<GamePlayer> spectatePlayers = new ArrayList<>();
    EditSession editSession;
    int arenaIndex = -1;

    public boolean isStarted = false, startCountdown = false;

    public Arena(World world, String name, int spawnX, int spawnY, int spawnZ, int mobSpawnX, int mobSpawnY, int mobSpawnZ) {
        this.world = world;
        this.arenaName = name;
        file = new File(WorldEditPlugin.getPlugin(WorldEditPlugin.class).getDataFolder() + File.separator + "schematics" + File.separator + arenaName + ".schematic");

        this.arenaIndex = calculateArenaIndex();

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
        int spawnZ = this.spawnZ + Math.max(ARENA_OFFSET, arenaIndex * ARENA_OFFSET);
        return new Location(world, spawnX + 0.5f, spawnY, spawnZ + 0.5f, -90, 0);
    }

    public Location getMobSpawn() {
        int mobSpawnZ = this.mobSpawnZ + Math.max(ARENA_OFFSET, arenaIndex * ARENA_OFFSET);
        return new Location(world, mobSpawnX + 0.5f, mobSpawnY, mobSpawnZ + 0.5f);
    }

    public void load(int x, int y, int z) {
        if (!file.exists()) {
            MobAnnihilation.getInstance().getLogger().info("File (" + file.getAbsolutePath() + ") not found!");
            return;
        }

        z += Math.max(ARENA_OFFSET, arenaIndex * ARENA_OFFSET);

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
                    int spawnZ = this.spawnZ + Math.max(ARENA_OFFSET, arenaIndex * ARENA_OFFSET);
                    gamePlayer.getPlayer().teleport(new Location(world, spawnX + 0.5f, spawnY, spawnZ + 0.5f, -90, 0));
                });
            } catch (WorldEditException | IOException e) {
                MobAnnihilation.getInstance().getLogger().severe("Error while loading schematic: " + e.getMessage());
            }
        }
    }

    public void unload() {
        if (arenaPlayers.isEmpty()) {
            GameManager.ARENA_LIST.remove(this);

            if (editSession != null) {
                editSession.undo(editSession);
            }

            MOBS.forEach(GameMob::kill);
            MOBS.clear();
        }
    }

    public void start() {
        startCountdown = true;

        BukkitScheduler scheduler = Bukkit.getScheduler();
        int WAIT_SECONDS = MobAnnihilation.getConfiguration().getInt("options.arena_start_seconds");

        AtomicInteger secondsLeft = new AtomicInteger(WAIT_SECONDS);

        scheduler.runTaskTimer(MobAnnihilation.getInstance(), () -> {
            if (secondsLeft.get() == 0) {
                isStarted = true;
                spawnMobs();
            } else {
                playSound(Sound.UI_BUTTON_CLICK);
                sendMessage("&bНачало через &6" + secondsLeft.getAndDecrement() + " сек.");
            }
        }, 0L, 20L);
    }

    public void spawnMobs() {
        for (int i = 0; i < 3 * getPlayers().size(); i++) {
            EntityType entityType = Math.random() * 10 <= 4.2 ? EntityType.PIG_ZOMBIE : EntityType.ZOMBIE;
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
        return MOBS.stream().filter(mob -> mob.uniqueId.equals(uniqueId)).findFirst().orElse(null);
    }

    public boolean hasAliveMobs() {
        return MOBS.stream().anyMatch(mob -> mob.livingEntity != null && !mob.livingEntity.isDead());
    }

    public int getLevel() {
        AtomicInteger levels = new AtomicInteger();
        arenaPlayers.forEach(gamePlayer -> levels.addAndGet(gamePlayer.getLevel()));
        return levels.get() / arenaPlayers.size();
    }

    public boolean hasPlayer(String nick) {
        return arenaPlayers.stream().anyMatch(player -> player.getName().equals(nick));
    }

    public int getId() {
        return arenaIndex;
    }

    public String getState() {
        return isStarted ? ChatColor.GREEN + "играют" : ChatColor.RED + "ожидание";
    }

    public List<GamePlayer> getAllPlayers() {
        return Stream.concat(spectatePlayers.stream(), arenaPlayers.stream()).collect(Collectors.toList());
    }

    public List<GamePlayer> getPlayers() {
        return arenaPlayers;
    }

    public List<GamePlayer> getSpectators() {
        return spectatePlayers;
    }

    public int getOwnerLevel() {
        if (arenaPlayers.get(0) == null) return 1;
        return arenaPlayers.get(0).getLevel();
    }

    public void playSound(Sound sound) {
        getAllPlayers().forEach(player -> player.getPlayer().playSound(player.getPlayer().getLocation(), sound, 1f, 1f));
    }

    public void sendMessage(String message) {
        getAllPlayers().forEach(player -> player.getPlayer().sendMessage(message.replace('&', ChatColor.COLOR_CHAR)));
    }

    public void checkAll() {
        boolean canStart = getPlayers().stream().allMatch(gamePlayer -> gamePlayer.getPlayer().getLocation().getY() <= 10);

        if (canStart && !startCountdown) start();
    }

    private int calculateArenaIndex() {
        int index = 1;
        for (Arena arena : GameManager.ARENA_LIST) {
            if (arena.arenaIndex != index) return index;
            index++;
        }
        return GameManager.ARENA_LIST.size() + 1;
    }
}
