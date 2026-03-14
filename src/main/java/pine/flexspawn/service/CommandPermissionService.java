package pine.flexspawn.service;

// 用法：集中处理命令权限节点，避免命令执行器内散落重复判断。
import java.util.Locale;
import org.bukkit.command.CommandSender;

public final class CommandPermissionService {

    private static final String ADMIN_PERMISSION = "flexspawn.admin";
    private static final String TELEPORT_ALL_PERMISSION = "flexspawn.tp";

    public boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission(ADMIN_PERMISSION);
    }

    public boolean hasTeleportPermission(
            CommandSender sender,
            String groupName,
            boolean exactCoordinate,
            boolean otherTarget
    ) {
        if (sender.hasPermission(TELEPORT_ALL_PERMISSION)) {
            return true;
        }

        String normalizedGroupName = groupName.toLowerCase(Locale.ROOT);
        String permissionNode;
        if (otherTarget) {
            permissionNode = exactCoordinate
                    ? "flexspawn.tp.other.acc." + normalizedGroupName
                    : "flexspawn.tp.other." + normalizedGroupName;
        } else {
            permissionNode = exactCoordinate
                    ? "flexspawn.tp.acc." + normalizedGroupName
                    : "flexspawn.tp." + normalizedGroupName;
        }
        return sender.hasPermission(permissionNode);
    }
}
