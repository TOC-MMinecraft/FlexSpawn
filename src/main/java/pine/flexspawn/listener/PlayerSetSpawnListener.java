package pine.flexspawn.listener;

// 用法：仅在玩家通过床或重生锚变更出生点时，同步更新个人存档点。
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import pine.flexspawn.service.SpawnDecisionService;

public final class PlayerSetSpawnListener implements Listener {

    private final SpawnDecisionService decisionService;

    public PlayerSetSpawnListener(SpawnDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (!decisionService.isPersonalSpawnManaged()) {
            return;
        }

        PlayerSetSpawnEvent.Cause cause = event.getCause();
        if (cause != PlayerSetSpawnEvent.Cause.BED
                && cause != PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getLocation();
        if (location == null) {
            decisionService.clearPersonalSpawn(player.getUniqueId(), player.getName());
            return;
        }

        decisionService.recordPersonalSpawnAndNotify(player, location);
    }
}