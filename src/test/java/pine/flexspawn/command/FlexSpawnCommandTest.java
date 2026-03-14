package pine.flexspawn.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import pine.flexspawn.FlexSpawnPlugin;
import pine.flexspawn.model.LocationData;
import pine.flexspawn.service.CommandPermissionService;
import pine.flexspawn.service.GroupConfigEditorService;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class FlexSpawnCommandTest {

    @Test
    void shouldReportMissingWorldWhenTeleportTargetWorldDoesNotExist() {
        FlexSpawnPlugin plugin = mock(FlexSpawnPlugin.class);
        GroupConfigEditorService groupConfigEditorService = mock(GroupConfigEditorService.class);
        CommandPermissionService commandPermissionService = mock(CommandPermissionService.class);
        Server server = mock(Server.class);
        Player player = mock(Player.class);
        Command command = mock(Command.class);

        when(plugin.getServer()).thenReturn(server);
        when(commandPermissionService.hasTeleportPermission(player, "arena", false, false)).thenReturn(true);
        when(groupConfigEditorService.findGroupLocation("arena", player))
                .thenReturn(Optional.of(new LocationData("missing-world", 1.0D, 2.0D, 3.0D, 0.0F, 0.0F)));

        FlexSpawnCommand flexSpawnCommand = new FlexSpawnCommand(
                plugin,
                groupConfigEditorService,
                commandPermissionService
        );

        flexSpawnCommand.onCommand(player, command, "fs", new String[]{"tp", "arena"});

        verify(player).sendMessage(contains("missing-world"));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    void shouldReportFailureWhenTeleportReturnsFalse() {
        FlexSpawnPlugin plugin = mock(FlexSpawnPlugin.class);
        GroupConfigEditorService groupConfigEditorService = mock(GroupConfigEditorService.class);
        CommandPermissionService commandPermissionService = mock(CommandPermissionService.class);
        Server server = mock(Server.class);
        World world = mock(World.class);
        Player player = mock(Player.class);
        Command command = mock(Command.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getWorld("world")).thenReturn(world);
        when(commandPermissionService.hasTeleportPermission(player, "arena", false, false)).thenReturn(true);
        when(groupConfigEditorService.findGroupLocation("arena", player))
                .thenReturn(Optional.of(new LocationData("world", 1.0D, 2.0D, 3.0D, 0.0F, 0.0F)));
        when(player.teleport(any(Location.class))).thenReturn(false);

        FlexSpawnCommand flexSpawnCommand = new FlexSpawnCommand(
                plugin,
                groupConfigEditorService,
                commandPermissionService
        );

        flexSpawnCommand.onCommand(player, command, "fs", new String[]{"tp", "arena"});

        verify(player).sendMessage("\u4f20\u9001\u5931\u8d25\u3002");
        verify(player, never()).sendMessage("\u4f20\u9001\u6210\u529f\u3002");
        verify(player, never()).sendMessage("\u4f60\u5df2\u88ab\u4f20\u9001\u3002");
        verify(groupConfigEditorService).findGroupLocation(eq("arena"), eq(player));
    }

    @Test
    void shouldNotSendSuccessMessageWhenReloadFailsAfterEdit() {
        FlexSpawnPlugin plugin = mock(FlexSpawnPlugin.class);
        GroupConfigEditorService groupConfigEditorService = mock(GroupConfigEditorService.class);
        CommandPermissionService commandPermissionService = mock(CommandPermissionService.class);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        when(commandPermissionService.hasAdminPermission(sender)).thenReturn(true);
        when(groupConfigEditorService.deleteGroup("arena")).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("FlexSpawnCommandTest"));
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(plugin).reloadRuntime();

        FlexSpawnCommand flexSpawnCommand = new FlexSpawnCommand(
                plugin,
                groupConfigEditorService,
                commandPermissionService
        );

        flexSpawnCommand.onCommand(sender, command, "fs", new String[]{"del", "arena"});

        verify(sender).sendMessage("\u914d\u7f6e\u5df2\u5199\u5165\uff0c\u4f46\u70ed\u91cd\u8f7d\u5931\u8d25: boom");
        verify(sender, never()).sendMessage("\u5df2\u5220\u9664\u5750\u6807\u7ec4: arena");
    }
}