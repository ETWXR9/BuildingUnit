package org.etwxr9.buildingunit.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.etwxr9.buildingunit.UnitInfo;
import org.etwxr9.buildingunit.service.BuildingUnitService;

import java.util.List;

public class BUCommand implements CommandExecutor {

    private final BuildingUnitService service;

    public BUCommand(BuildingUnitService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("buildingunit.admin")) {
            sender.sendMessage("[BuildingUnit] 你没有权限执行此命令。");
            return true;
        }
        if (args.length == 0) {
            return sendUsage(sender);
        }
        return switch (args[0].toLowerCase()) {
            case "save" -> handleSave(sender, args);
            case "paste" -> handlePaste(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "delete" -> handleDelete(sender);
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            default -> sendUsage(sender);
        };
    }

    private boolean handleSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[BuildingUnit] 该命令只能由玩家执行。");
            return true;
        }
        if (args.length != 5 && args.length != 8 && args.length != 6 && args.length != 9) {
            return sendUsage(sender);
        }
        String name = args[1];
        int sizeX;
        int sizeY;
        int sizeZ;
        try {
            sizeX = Integer.parseInt(args[2]);
            sizeY = Integer.parseInt(args[3]);
            sizeZ = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("[BuildingUnit] 尺寸参数必须是整数。");
            return true;
        }
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            sender.sendMessage("[BuildingUnit] 尺寸参数必须大于 0。");
            return true;
        }
        Location min = player.getLocation().toBlockLocation();
        Location max = min.clone().add(sizeX - 1, sizeY - 1, sizeZ - 1);
        int cursor = 5;
        Location origin = min.clone();
        if (args.length == 8 || args.length == 9) {
            try {
                int offsetX = Integer.parseInt(args[5]);
                int offsetY = Integer.parseInt(args[6]);
                int offsetZ = Integer.parseInt(args[7]);
                origin = min.clone().add(offsetX, offsetY, offsetZ);
                cursor = 8;
            } catch (NumberFormatException e) {
                sender.sendMessage("[BuildingUnit] 原点偏移参数必须是整数。");
                return true;
            }
        }
        boolean confirm = args.length > cursor && "confirm".equalsIgnoreCase(args[cursor]);
        if (!confirm) {
            service.showCubeParticle(min, max, List.of(player), 255, 0, 0, 20, 10);
            sender.sendMessage("[BuildingUnit] 已显示保存范围预览。追加 confirm 才会真正保存。");
            return true;
        }
        boolean saved = service.saveSchematic(min, max, name, origin);
        sender.sendMessage(saved ? "[BuildingUnit] 已保存 schematic。" : "[BuildingUnit] 保存 schematic 失败，请检查日志。");
        return true;
    }

    private boolean handlePaste(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[BuildingUnit] 该命令只能由玩家执行。");
            return true;
        }
        if (args.length < 3 || args.length > 5) {
            return sendUsage(sender);
        }
        int rotate;
        try {
            rotate = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("[BuildingUnit] rotate 必须是整数。");
            return true;
        }
        boolean ignoreAir = args.length >= 4 && Boolean.parseBoolean(args[3]);
        boolean confirm = args.length == 5 && "confirm".equalsIgnoreCase(args[4]);
        if (!confirm) {
            service.showSchematicParticle(player.getLocation().toBlockLocation(), List.of(player), args[1], 255, 0, 0,
                    20, 10, rotate);
            sender.sendMessage("[BuildingUnit] 已显示粘贴预览。追加 confirm 才会真正粘贴。");
            return true;
        }
        UnitInfo unitInfo = service.pasteUnit(player.getLocation().toBlockLocation(), args[1], rotate, ignoreAir);
        sender.sendMessage(unitInfo == null ? "[BuildingUnit] 粘贴失败，可能是模板不存在、区域重叠或被事件取消。"
                : "[BuildingUnit] 粘贴成功，UUID: " + unitInfo.getUuid());
        return true;
    }

    private boolean handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[BuildingUnit] 该命令只能由玩家执行。");
            return true;
        }
        if (args.length != 3) {
            return sendUsage(sender);
        }
        try {
            int rotate = Integer.parseInt(args[2]);
            service.showSchematicParticle(player.getLocation().toBlockLocation(), List.of(player), args[1], 255, 0, 0,
                    20, 10, rotate);
            sender.sendMessage("[BuildingUnit] 已显示粘贴预览。");
        } catch (NumberFormatException e) {
            sender.sendMessage("[BuildingUnit] rotate 必须是整数。");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[BuildingUnit] 该命令只能由玩家执行。");
            return true;
        }
        UnitInfo unitInfo = service.getUnit(player.getLocation());
        if (unitInfo == null) {
            sender.sendMessage("[BuildingUnit] 当前位置没有 Unit。");
            return true;
        }
        boolean deleted = service.deleteUnit(unitInfo, true);
        sender.sendMessage(deleted ? "[BuildingUnit] 删除成功。" : "[BuildingUnit] 删除失败或被事件取消。");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[BuildingUnit] 该命令只能由玩家执行。");
            return true;
        }
        UnitInfo unitInfo = service.getUnit(player.getLocation());
        if (unitInfo == null) {
            sender.sendMessage("[BuildingUnit] 当前位置没有 Unit。");
            return true;
        }
        unitInfo.showInfoMsg(player);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        service.reload();
        sender.sendMessage("[BuildingUnit] 已重新加载 schematic、unit 数据和索引。");
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        BuildingUnitService.Stats stats = service.getStats();
        sender.sendMessage("[BuildingUnit] schematics=" + stats.schematicCount()
                + ", units=" + stats.unitCount()
                + ", worlds=" + stats.worldBucketCount()
                + ", chunkBuckets=" + stats.chunkBucketCount());
        return true;
    }

    private boolean sendUsage(CommandSender sender) {
        sender.sendMessage(
                "/bu save <name> <sizeX> <sizeY> <sizeZ> [originOffsetX originOffsetY originOffsetZ] [confirm]");
        sender.sendMessage("/bu paste <name> <rotate> [ignoreAir] [confirm]");
        sender.sendMessage("/bu preview <name> <rotate>");
        sender.sendMessage("/bu delete");
        sender.sendMessage("/bu info");
        sender.sendMessage("/bu reload");
        sender.sendMessage("/bu stats");
        return true;
    }
}
