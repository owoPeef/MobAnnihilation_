package ru.peef.mobannihilation.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.peef.mobannihilation.game.Arena;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;
import ru.peef.mobannihilation.menus.Menu;

public class SpectateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("game.moderation.spectate")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
            return false;
        }

        if (args.length == 0) {
            openSpectateMenu(player);
        } else if (args.length == 2) {
            handleSpectateByArgs(player, args);
        } else {
            showHelp(player, cmd.getName());
        }

        return true;
    }

    private void openSpectateMenu(Player player) {
        GamePlayer gamePlayer = PlayerManager.get(player);
        if (gamePlayer != null) {
            player.openInventory(Menu.find("spectate").getInventory(gamePlayer));
        }
    }

    private void handleSpectateByArgs(Player player, String[] args) {
        String type = args[0];
        String identifier = args[1];

        switch (type.toLowerCase()) {
            case "id":
                handleSpectateByArenaId(player, identifier);
                break;
            case "nick":
                handleSpectateByPlayerNick(player, identifier);
                break;
            default:
                showHelp(player, "spectate");
        }
    }

    private void handleSpectateByArenaId(Player player, String arenaIdStr) {
        try {
            int arenaId = Integer.parseInt(arenaIdStr);
            Arena arena = findArenaById(arenaId);

            if (arena != null) {
                GamePlayer gamePlayer = PlayerManager.get(player);
                if (gamePlayer != null) {
                    gamePlayer.startSpectate(arena);
                }
            } else {
                player.sendMessage(ChatColor.RED + "Арена с таким ID не найдена!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Номер арены должен быть числом!");
        }
    }

    private void handleSpectateByPlayerNick(Player player, String playerNick) {
        Arena arena = findArenaByPlayerNick(playerNick);

        if (arena != null) {
            GamePlayer gamePlayer = PlayerManager.get(player);
            if (gamePlayer != null) {
                gamePlayer.startSpectate(arena);
            }
        } else {
            player.sendMessage(ChatColor.RED + "Игрок с ником " + playerNick + " не на арене.");
        }
    }

    private Arena findArenaById(int id) {
        for (Arena arena : GameManager.ARENA_LIST) {
            if (arena.getId() == id) {
                return arena;
            }
        }
        return null;
    }

    private Arena findArenaByPlayerNick(String playerNick) {
        for (Arena arena : GameManager.ARENA_LIST) {
            if (arena.hasPlayer(playerNick)) {
                return arena;
            }
        }
        return null;
    }

    private void showHelp(Player player, String commandName) {
        player.sendMessage(ChatColor.YELLOW + "Для наблюдения за игроками:");
        player.sendMessage(ChatColor.GOLD + "/" + commandName + " id [ARENA_ID] " + ChatColor.AQUA + "- наблюдение по арене");
        player.sendMessage(ChatColor.GOLD + "/" + commandName + " nick [PLAYER_NICK] " + ChatColor.AQUA + "- наблюдение по игроку");
    }
}
