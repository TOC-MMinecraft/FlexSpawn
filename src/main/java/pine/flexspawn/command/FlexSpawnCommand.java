package pine.flexspawn.command;

import pine.flexspawn.FlexSpawnPlugin;
import pine.flexspawn.model.LocationData;
import pine.flexspawn.service.CommandPermissionService;
import pine.flexspawn.service.GroupConfigEditorService;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FlexSpawnCommand implements CommandExecutor {

    private static final String MESSAGE_NO_PERMISSION_COMMAND = "\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u547d\u4ee4\u3002";
    private static final String MESSAGE_NO_PERMISSION_TELEPORT = "\u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u4f20\u9001\u3002";
    private static final String MESSAGE_TELEPORT_GROUP_NOT_FOUND = "\u5750\u6807\u7ec4\u4e0d\u5b58\u5728\u6216\u6ca1\u6709\u53ef\u7528\u5750\u6807\u70b9\u3002";
    private static final String MESSAGE_TELEPORT_POINT_NOT_FOUND = "\u672a\u627e\u5230\u6307\u5b9a\u5750\u6807\u70b9\u3002";
    private static final String MESSAGE_TELEPORT_FAILED = "\u4f20\u9001\u5931\u8d25\u3002";
    private static final String MESSAGE_TELEPORT_SUCCESS = "\u4f20\u9001\u6210\u529f\u3002";
    private static final String MESSAGE_TELEPORT_OTHER_SUCCESS = "\u5df2\u5c06\u73a9\u5bb6\u4f20\u9001\u5230\u76ee\u6807\u4f4d\u7f6e\u3002";
    private static final String MESSAGE_TELEPORT_OTHER_TARGET = "\u4f60\u5df2\u88ab\u4f20\u9001\u3002";
    private static final String MESSAGE_CONSOLE_TELEPORT_REQUIRES_PLAYER = "\u63a7\u5236\u53f0\u6267\u884c\u4f20\u9001\u65f6\u5fc5\u987b\u6307\u5b9a\u73a9\u5bb6\u540d\u3002";
    private static final String MESSAGE_TARGET_PLAYER_NOT_FOUND = "\u76ee\u6807\u73a9\u5bb6\u4e0d\u5b58\u5728\u6216\u4e0d\u5728\u7ebf\u3002";
    private static final String MESSAGE_PLAYER_ONLY_ADD = "\u53ea\u6709\u73a9\u5bb6\u624d\u80fd\u4f7f\u7528\u5f53\u524d\u4f4d\u7f6e\u521b\u5efa\u5750\u6807\u70b9\u3002";
    private static final String MESSAGE_COORDINATE_EXISTS = "\u5750\u6807\u70b9\u5df2\u5b58\u5728\u3002";
    private static final String MESSAGE_DELETE_GROUP_NOT_FOUND = "\u672a\u627e\u5230\u6307\u5b9a\u5750\u6807\u7ec4\u3002";
    private static final String MESSAGE_DELETE_PERMISSION_GROUP_NOT_FOUND = "\u672a\u627e\u5230\u6307\u5b9a\u6743\u9650\u7ec4\u3002";
    private static final String MESSAGE_DELETE_COORDINATE_NOT_FOUND = "\u672a\u627e\u5230\u6307\u5b9a\u5750\u6807\u70b9\u3002";
    private static final String MESSAGE_WEIGHT_MUST_BE_POSITIVE = "\u6743\u91cd\u5fc5\u987b\u5927\u4e8e 0\u3002";
    private static final String MESSAGE_WEIGHT_MUST_BE_INTEGER = "\u6743\u91cd\u5fc5\u987b\u662f\u6b63\u6574\u6570\u3002";

    private final FlexSpawnPlugin plugin;
    private final GroupConfigEditorService groupConfigEditorService;
    private final CommandPermissionService commandPermissionService;

    public FlexSpawnCommand(
            FlexSpawnPlugin plugin,
            GroupConfigEditorService groupConfigEditorService,
            CommandPermissionService commandPermissionService
    ) {
        this.plugin = plugin;
        this.groupConfigEditorService = groupConfigEditorService;
        this.commandPermissionService = commandPermissionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "tp" -> handleTeleport(sender, args);
            case "add" -> handleAdd(sender, args);
            case "del" -> handleDelete(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!commandPermissionService.hasAdminPermission(sender)) {
            sender.sendMessage(MESSAGE_NO_PERMISSION_COMMAND);
            return true;
        }

        try {
            plugin.reloadRuntime();
            sender.sendMessage("FlexSpawn \u914d\u7f6e\u5df2\u91cd\u8f7d\u3002");
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "FlexSpawn \u91cd\u8f7d\u5931\u8d25\u3002", exception);
            sender.sendMessage("FlexSpawn \u91cd\u8f7d\u5931\u8d25: " + exception.getMessage());
        }
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 4) {
            sender.sendMessage("\u7528\u6cd5: /fs tp <group> [coordinate] [playername]");
            return true;
        }

        String groupName = args[1];
        TeleportRequest teleportRequest;
        try {
            teleportRequest = parseTeleportRequest(sender, args);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }

        if (!commandPermissionService.hasTeleportPermission(
                sender,
                groupName,
                teleportRequest.coordinateName() != null,
                teleportRequest.otherTarget()
        )) {
            sender.sendMessage(MESSAGE_NO_PERMISSION_TELEPORT);
            return true;
        }

        Location targetLocation;
        try {
            Optional<LocationData> locationData = teleportRequest.coordinateName() == null
                    ? groupConfigEditorService.findGroupLocation(groupName, teleportRequest.targetPlayer())
                    : groupConfigEditorService.findCoordinateLocation(groupName, teleportRequest.coordinateName());
            if (locationData.isEmpty()) {
                sender.sendMessage(
                        teleportRequest.coordinateName() == null
                                ? MESSAGE_TELEPORT_GROUP_NOT_FOUND
                                : MESSAGE_TELEPORT_POINT_NOT_FOUND
                );
                return true;
            }
            targetLocation = locationData.get().toLocation(plugin.getServer());
        } catch (RuntimeException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }

        if (!teleportRequest.targetPlayer().teleport(targetLocation)) {
            sender.sendMessage(MESSAGE_TELEPORT_FAILED);
            return true;
        }

        if (teleportRequest.otherTarget()) {
            sender.sendMessage(MESSAGE_TELEPORT_OTHER_SUCCESS);
            teleportRequest.targetPlayer().sendMessage(MESSAGE_TELEPORT_OTHER_TARGET);
        } else {
            sender.sendMessage(MESSAGE_TELEPORT_SUCCESS);
        }
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!commandPermissionService.hasAdminPermission(sender)) {
            sender.sendMessage(MESSAGE_NO_PERMISSION_COMMAND);
            return true;
        }

        try {
            return switch (args.length) {
                case 4, 5 -> handleAddCoordinate(sender, args);
                default -> {
                    sender.sendMessage("\u7528\u6cd5: /fs add <group> <permission> <coordinate> [weight]");
                    yield true;
                }
            };
        } catch (RuntimeException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!commandPermissionService.hasAdminPermission(sender)) {
            sender.sendMessage(MESSAGE_NO_PERMISSION_COMMAND);
            return true;
        }

        try {
            return switch (args.length) {
                case 2 -> executeEditAction(
                        sender,
                        MESSAGE_DELETE_GROUP_NOT_FOUND,
                        "\u5df2\u5220\u9664\u5750\u6807\u7ec4: " + args[1],
                        () -> groupConfigEditorService.deleteGroup(args[1])
                );
                case 3 -> executeEditAction(
                        sender,
                        MESSAGE_DELETE_PERMISSION_GROUP_NOT_FOUND,
                        "\u5df2\u5220\u9664\u6743\u9650\u7ec4: " + args[2],
                        () -> groupConfigEditorService.deletePermissionGroup(args[1], args[2])
                );
                case 4 -> executeEditAction(
                        sender,
                        MESSAGE_DELETE_COORDINATE_NOT_FOUND,
                        "\u5df2\u5220\u9664\u5750\u6807\u70b9: " + args[3],
                        () -> groupConfigEditorService.deleteCoordinate(args[1], args[2], args[3])
                );
                default -> {
                    sender.sendMessage("\u7528\u6cd5: /fs del <group> [permission] [coordinate]");
                    yield true;
                }
            };
        } catch (RuntimeException exception) {
            sender.sendMessage(exception.getMessage());
            return true;
        }
    }

    private boolean handleAddCoordinate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MESSAGE_PLAYER_ONLY_ADD);
            return true;
        }

        int weight = args.length == 5 ? parseWeight(args[4]) : 1;
        Location currentLocation = player.getLocation();
        return executeEditAction(
                sender,
                MESSAGE_COORDINATE_EXISTS,
                "\u5df2\u521b\u5efa\u5750\u6807\u70b9: " + args[3],
                () -> groupConfigEditorService.addCoordinate(
                        args[1],
                        args[2],
                        args[3],
                        LocationData.fromLocation(currentLocation),
                        weight
                )
        );
    }

    private boolean executeEditAction(
            CommandSender sender,
            String failureMessage,
            String successMessage,
            BooleanSupplier action
    ) {
        if (!action.getAsBoolean()) {
            sender.sendMessage(failureMessage);
            return true;
        }

        if (!reloadRuntimeAfterEdit(sender)) {
            return true;
        }
        sender.sendMessage(successMessage);
        return true;
    }

    private TeleportRequest parseTeleportRequest(CommandSender sender, String[] args) {
        String groupName = args[1];
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                throw new IllegalArgumentException(MESSAGE_CONSOLE_TELEPORT_REQUIRES_PLAYER);
            }
            return new TeleportRequest(player, null, false);
        }

        if (args.length == 4) {
            Player targetPlayer = findOnlinePlayer(args[3]);
            return new TeleportRequest(targetPlayer, args[2], isOtherTarget(sender, targetPlayer));
        }

        String thirdArgument = args[2];
        if (!(sender instanceof Player player)) {
            Player targetPlayer = findOnlinePlayer(thirdArgument);
            return new TeleportRequest(targetPlayer, null, true);
        }

        if (groupConfigEditorService.containsCoordinate(groupName, thirdArgument)) {
            return new TeleportRequest(player, thirdArgument, false);
        }

        Player targetPlayer = plugin.getServer().getPlayerExact(thirdArgument);
        if (targetPlayer != null) {
            return new TeleportRequest(targetPlayer, null, isOtherTarget(sender, targetPlayer));
        }

        return new TeleportRequest(player, thirdArgument, false);
    }

    private Player findOnlinePlayer(String playerName) {
        Player targetPlayer = plugin.getServer().getPlayerExact(playerName);
        if (targetPlayer == null) {
            throw new IllegalArgumentException(MESSAGE_TARGET_PLAYER_NOT_FOUND);
        }
        return targetPlayer;
    }

    private boolean isOtherTarget(CommandSender sender, Player targetPlayer) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return !Objects.equals(player.getUniqueId(), targetPlayer.getUniqueId());
    }

    private int parseWeight(String value) {
        try {
            int weight = Integer.parseInt(value);
            if (weight <= 0) {
                throw new IllegalArgumentException(MESSAGE_WEIGHT_MUST_BE_POSITIVE);
            }
            return weight;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(MESSAGE_WEIGHT_MUST_BE_INTEGER);
        }
    }

    private boolean reloadRuntimeAfterEdit(CommandSender sender) {
        try {
            plugin.reloadRuntime();
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "FlexSpawn \u5199\u5165\u540e\u70ed\u91cd\u8f7d\u5931\u8d25\u3002", exception);
            sender.sendMessage("\u914d\u7f6e\u5df2\u5199\u5165\uff0c\u4f46\u70ed\u91cd\u8f7d\u5931\u8d25: " + exception.getMessage());
            return false;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("/fs reload");
        sender.sendMessage("/fs tp <group> [coordinate] [playername]");
        sender.sendMessage("/fs add <group> <permission> <coordinate> [weight]");
        sender.sendMessage("/fs del <group> [permission] [coordinate]");
    }

    private record TeleportRequest(
            Player targetPlayer,
            String coordinateName,
            boolean otherTarget
    ) {
    }
}