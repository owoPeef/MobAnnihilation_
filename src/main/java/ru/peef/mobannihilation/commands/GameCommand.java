package ru.peef.mobannihilation.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.peef.mobannihilation.game.AnvilGUI;
import ru.peef.mobannihilation.game.Arena;
import ru.peef.mobannihilation.game.GameManager;
import ru.peef.mobannihilation.game.items.RarityItem;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.handlers.PlayerDataHandler;
import ru.peef.mobannihilation.game.players.PlayerManager;

public class GameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        Player player = (Player) sender;
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer != null) {
            if (args.length == 1) {
                switch (args[0]) {
                    case "join":
                        gamePlayer.joinArena();
                        break;
                    case "leave":
                        gamePlayer.leaveArena(true);
                        break;
                    case "rebith":
                        gamePlayer.rebith();
                        break;
                    // TODO: Combine runes
                    case "combine":
                        AnvilGUI.openAnvil(player);
                        break;
                    case "runes":
                        gamePlayer.openRuneInventory();
                        break;
                    case "item":
                        gamePlayer.addItem(RarityItem.getRandom(gamePlayer), true);
                        break;
                    case "edit":
                        gamePlayer.editMode = !gamePlayer.editMode;
                        player.sendMessage(ChatColor.AQUA + "Режим редактирования: " + (gamePlayer.editMode ? ChatColor.GREEN + "вкл" : ChatColor.RED + "выкл"));
                }
            } else if (args.length == 2) {
                if (args[0].equals("join")) {
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
                            gamePlayer.joinArena(arena);
                        } else {
                            player.sendMessage(ChatColor.RED + "Такой арены нет!");
                        }
                    } catch (NumberFormatException ignored) {
                        player.sendMessage(ChatColor.RED + "Арена не найдена!");
                    }
                }

                if (args[0].equals("add_progress") || args[0].equals("add_level") || args[0].equals("add_lvl")) {
                    try {
                        // ADD
                        int count = Integer.parseInt(args[1]);

                        if (args[0].equals("add_progress")) {
                            if (count > 0 && count <= 100) {
                                gamePlayer.addProgress(count);
                                player.sendMessage(ChatColor.AQUA + "Добавлено прогресса: " + ChatColor.GOLD + count + "%");
                                gamePlayer.save();
                            } else {
                                player.sendMessage(ChatColor.RED + "Прогресс должен иметь диапозон от 1 до 100!");
                            }
                        } else {
                            gamePlayer.addLevel(count);
                            player.sendMessage(ChatColor.AQUA + "Добавлено уровней: " + ChatColor.GOLD + count);
                            gamePlayer.save();
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }

        return true;
    }
}
