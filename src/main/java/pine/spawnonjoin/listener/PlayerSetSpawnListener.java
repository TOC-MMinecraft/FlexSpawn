package pine.spawnonjoin.listener;

// 用法：仅在玩家通过床或重生锚变更出生点时，同步更新个人存档点。
import pine.spawnonjoin.service.SpawnDecisionService;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PlayerSetSpawnListener implements Listener {

    private final SpawnDecisionService decisionService;

    public PlayerSetSpawnListener(SpawnDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        PlayerSetSpawnEvent.Cause cause = event.getCause();
        if (cause != PlayerSetSpawnEvent.Cause.BED
                && cause != PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR) {
            return;
        }

        Location location = event.getLocation();
        if (location == null) {
            decisionService.clearPersonalSpawn(event.getPlayer().getUniqueId(), event.getPlayer().getName());
            return;
        }

        decisionService.recordPersonalSpawn(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                location
        );
    }
}
