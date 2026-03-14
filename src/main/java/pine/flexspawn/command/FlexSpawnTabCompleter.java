package pine.flexspawn.command;

// 用法：为 FlexSpawn 命令提供组、权限组、坐标点和玩家名的动态补全。
import pine.flexspawn.FlexSpawnPlugin;
import pine.flexspawn.service.GroupConfigEditorService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class FlexSpawnTabCompleter implements TabCompleter {

    private final FlexSpawnPlugin plugin;
    private final GroupConfigEditorService groupConfigEditorService;

    public FlexSpawnTabCompleter(FlexSpawnPlugin plugin, GroupConfigEditorService groupConfigEditorService) {
        this.plugin = plugin;
        this.groupConfigEditorService = groupConfigEditorService;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(args[0], List.of("reload", "tp", "add", "del"));
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "tp", "add", "del" -> filterByPrefix(args[1], groupConfigEditorService.getGroupNames());
                default -> List.of();
            };
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "tp" -> completeTeleport(args);
            case "add" -> completeAdd(args);
            case "del" -> completeDelete(args);
            default -> List.of();
        };
    }

    private List<String> completeTeleport(String[] args) {
        if (args.length == 3) {
            Collection<String> suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            suggestions.addAll(groupConfigEditorService.getCoordinateNames(args[1]));
            suggestions.addAll(getOnlinePlayerNames());
            return filterByPrefix(args[2], suggestions);
        }
        if (args.length == 4) {
            return filterByPrefix(args[3], getOnlinePlayerNames());
        }
        return List.of();
    }

    private List<String> completeAdd(String[] args) {
        if (args.length == 3) {
            return filterByPrefix(args[2], groupConfigEditorService.getPermissionGroupNames(args[1]));
        }
        if (args.length == 5) {
            return filterByPrefix(args[4], List.of("1"));
        }
        return List.of();
    }

    private List<String> completeDelete(String[] args) {
        if (args.length == 3) {
            return filterByPrefix(args[2], groupConfigEditorService.getPermissionGroupNames(args[1]));
        }
        if (args.length == 4) {
            return filterByPrefix(args[3], groupConfigEditorService.getCoordinateNames(args[1]));
        }
        return List.of();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            playerNames.add(onlinePlayer.getName());
        }
        return playerNames;
    }

    private List<String> filterByPrefix(String input, Collection<String> suggestions) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(normalizedInput)) {
                result.add(suggestion);
            }
        }
        return result;
    }
}
