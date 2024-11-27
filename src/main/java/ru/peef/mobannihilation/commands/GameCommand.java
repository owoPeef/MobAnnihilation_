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
import ru.peef.mobannihilation.game.players.PlayerManager;

public class GameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        GamePlayer gamePlayer = PlayerManager.get(player);

        if (gamePlayer == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: Игрок не найден!");
            return false;
        }

        if (args.length == 1) {
            handleSingleArgumentCommand(player, gamePlayer, args[0]);
        } else if (args.length == 2) {
            handleDoubleArgumentCommand(player, gamePlayer, args[0], args[1]);
        }

        return true;
    }

    private void handleSingleArgumentCommand(Player player, GamePlayer gamePlayer, String arg) {
        switch (arg.toLowerCase()) {
            case "join":
                gamePlayer.joinArena();
                break;
            case "leave":
                gamePlayer.leaveArena(true);
                break;
            case "rebith":
                gamePlayer.rebith();
                break;
            case "combine":
                AnvilGUI.openAnvil(player);
                break;
            case "runes":
                gamePlayer.openRuneInventory();
                break;
            case "item":
                if (player.hasPermission("game.give_item")) gamePlayer.addItem(RarityItem.getRandom(gamePlayer), true);
                break;
            case "edit":
                player.sendMessage(toggleEditMode(player, gamePlayer));
                break;
            default:
                player.sendMessage(ChatColor.RED + "Неизвестная команда!");
        }
    }

    private void handleDoubleArgumentCommand(Player player, GamePlayer gamePlayer, String command, String value) {
        try {
            int count = Integer.parseInt(value);

            switch (command.toLowerCase()) {
                case "join":
                    joinArenaById(player, gamePlayer, count);
                    break;
                case "set_progress":
                    if (player.hasPermission("game.admin.set_progress")) gamePlayer.setProgress(count);
                    break;
                case "set_level":
                    if (player.hasPermission("game.admin.set_level")) {
                        gamePlayer.setLevel(count);
                        player.sendMessage(ChatColor.AQUA + "Уровень установлен на " + ChatColor.GOLD + count);
                    }
                    break;
                case "add_progress":
                    if (player.hasPermission("game.admin.add_progress")) gamePlayer.addProgress(count);
                    break;
                case "add_level":
                    if (player.hasPermission("game.admin.add_level")) gamePlayer.addLevel(count);
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Неизвестная команда!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Неверный формат числа!");
        }
    }

    private String toggleEditMode(Player player, GamePlayer gamePlayer) {
        if (!player.hasPermission("game.edit_mode")) return ChatColor.RED + "Недостаточно прав!";
        gamePlayer.editMode = !gamePlayer.editMode;
        return ChatColor.AQUA + "Режим редактирования: " +
                (gamePlayer.editMode ? ChatColor.GREEN + "вкл" : ChatColor.RED + "выкл");
    }

    private void joinArenaById(Player player, GamePlayer gamePlayer, int id) {
        Arena arena = GameManager.ARENA_LIST.stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);

        if (arena != null) {
            gamePlayer.joinArena(arena);
        } else {
            player.sendMessage(ChatColor.RED + "Арена с таким ID не найдена!");
        }
    }
}
