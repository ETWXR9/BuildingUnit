package org.etwxr9.buildingunit.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.etwxr9.buildingunit.BuildingUnitAPI;

import java.util.List;

public class BUCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        if (args.length == 1)
            return false;
        switch (args[0]) {
            case "save":
                return SaveCmd(sender, command, label, args);
            case "paste":
                return PasteCmd(sender, command, label, args);
            case "delete":
                return DeleteCmd(sender, command, label, args);
        }
        return false;
    }

    boolean SaveCmd(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        // 参数为x y z（都是大小）name confirm 不加confirm则出现粒子框架提示。 以玩家为min位置保存建筑
        int sizeX;
        int sizeY;
        int sizeZ;
        try {
            sizeX = Integer.parseInt(args[1]);
            sizeY = Integer.parseInt(args[2]);
            sizeZ = Integer.parseInt(args[3]);
        } catch (Exception e) {
            player.sendMessage("[BuildingUnit]参数错误");
            e.printStackTrace();
            return true;
        }
        Location locMin = player.getLocation();
        Location locMax = new Location(locMin.getWorld(),
                locMin.getBlockX() + sizeX - 1,
                locMin.getBlockY() + sizeY - 1,
                locMin.getBlockZ() + sizeZ - 1);

        if (args[5].equals("confirm")) {
            BuildingUnitAPI.saveSchematic(locMin, locMax, args[4], locMin.clone().add(3, 3, 3));
        }
        // 没有confirm，展示粒子
        else {
            BuildingUnitAPI.showCubeParticle(locMin, locMax, List.of(player), 255, 0, 0, 20, 10);
        }
        return true;
    }

    boolean PasteCmd(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        // bu paste name rotate confirm
        if (args[3].equals("confirm")) {
            BuildingUnitAPI.pasteUnit(player.getLocation(), args[1], Integer.parseInt(args[2]));
        } else {
            BuildingUnitAPI.showSchematicParticle(player.getLocation(), List.of(player),
                    args[1], 255, 0, 0, 20, 10, Integer.parseInt(args[2]));
        }
        return true;
    }

    boolean DeleteCmd(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        // bu delete
        var u = BuildingUnitAPI.getUnit(player.getLocation());
        if (u != null) {
            BuildingUnitAPI.deleteUnit(u, true);
            player.sendMessage("[BuildingUnit]删除成功");
        } else {
            player.sendMessage("[BuildingUnit]未找到Unit");
        }
        return true;
    }

}
