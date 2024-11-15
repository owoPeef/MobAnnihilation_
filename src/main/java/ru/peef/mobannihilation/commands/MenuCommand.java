package ru.peef.mobannihilation.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.peef.mobannihilation.game.players.GamePlayer;
import ru.peef.mobannihilation.game.players.PlayerManager;
import ru.peef.mobannihilation.menus.Menu;

public class MenuCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        Player player = (Player) sender;
        if (args.length == 2) {
            if (args[0].equals("open")) {
                String openMenu = args[1];
                Menu menu = Menu.find(openMenu);

                if (menu != null) {
                    GamePlayer gamePlayer = PlayerManager.get(player);

                    if (gamePlayer != null) player.openInventory(menu.getInventory(gamePlayer));
                }
            }
        }

        return true;
    }
}
