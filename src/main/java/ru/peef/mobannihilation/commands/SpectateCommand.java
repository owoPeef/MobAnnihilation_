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

// TODO: /s {nick or id} (auto get)
public class SpectateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        Player player = (Player) sender;
        if (player.hasPermission("game.spectate")) {
            if (args.length == 0) {
                GamePlayer gamePlayer = PlayerManager.get(player);

                if (gamePlayer != null) player.openInventory(Menu.find("spectate").getInventory(gamePlayer));
            } else if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "Для наблюдения за игроками:");
                player.sendMessage(ChatColor.GOLD + "/" + cmd.getName() + " id [ARENA_ID] " + ChatColor.AQUA + "- наблюдение по арене");
                player.sendMessage(ChatColor.GOLD + "/" + cmd.getName() + " nick [PLAYER_NICK] " + ChatColor.AQUA + "- наблюдение по игроку");
            }

            if (args.length == 2) {
                GamePlayer gamePlayer = PlayerManager.get(player);

                if (gamePlayer != null) {
                    if (args[0].equals("id")) {
                        String strId = args[1];
                        try {
                            int id = Integer.parseInt(strId);

                            Arena arena = null;
                            for (Arena checkArena : GameManager.ARENA_LIST) {
                                if (checkArena.getId() == id) {
                                    arena = checkArena;
                                    break;
                                }
                            }

                            if (arena != null) {
                                gamePlayer.startSpectate(arena);
                            } else {
                                player.sendMessage(ChatColor.RED + "Такой арены нет!");
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Номер арены может быть только числом!");
                        }
                    } else if (args[0].equals("nick")) {
                        String nick = args[1];

                        Arena arena = null;
                        for (Arena checkArena : GameManager.ARENA_LIST) {
                            if (checkArena.hasPlayer(nick)) {
                                arena = checkArena;
                                break;
                            }
                        }

                        if (arena != null) {
                            gamePlayer.startSpectate(arena);
                        } else {
                            player.sendMessage(ChatColor.RED + "Игрок не на арене!");
                        }
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Команда не найдена!");
        }

        return true;
    }
}
