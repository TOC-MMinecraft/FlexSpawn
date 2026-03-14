package pine.flexspawn.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class CommandPermissionServiceTest {

    private final CommandPermissionService permissionService = new CommandPermissionService();

    @Test
    void shouldAllowAllTeleportWhenSenderHasGlobalPermission() {
        CommandSender sender = senderWithPermissions("flexspawn.tp");

        boolean allowed = permissionService.hasTeleportPermission(sender, "arena", true, true);

        assertTrue(allowed);
    }

    @Test
    void shouldRequireGroupPermissionForSelfGroupTeleport() {
        CommandSender sender = senderWithPermissions("flexspawn.tp.arena");

        assertTrue(permissionService.hasTeleportPermission(sender, "arena", false, false));
        assertFalse(permissionService.hasTeleportPermission(sender, "arena", true, false));
    }

    @Test
    void shouldRequireOtherCoordinatePermissionForTeleportingOtherPlayerToExactPoint() {
        CommandSender sender = senderWithPermissions("flexspawn.tp.other.acc.arena");

        assertTrue(permissionService.hasTeleportPermission(sender, "arena", true, true));
        assertFalse(permissionService.hasTeleportPermission(sender, "arena", false, true));
    }

    private CommandSender senderWithPermissions(String... permissions) {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("flexspawn.admin")).thenReturn(false);
        when(sender.hasPermission("flexspawn.tp")).thenReturn(false);
        when(sender.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            String permission = invocation.getArgument(0, String.class);
            for (String grantedPermission : permissions) {
                if (grantedPermission.equals(permission)) {
                    return true;
                }
            }
            return false;
        });
        return sender;
    }
}
