package pine.flexspawn.listener;

// 用法：仅在玩家通过床或重生锚更新出生点时，同步个人出生点记录。
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import pine.flexspawn.service.SpawnDecisionService;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PlayerSetSpawnListener implements Listener {

    private final Supplier<SpawnDecisionService> decisionServiceSupplier;

    public PlayerSetSpawnListener(Supplier<SpawnDecisionService> decisionServiceSupplier) {
        this.decisionServiceSupplier = decisionServiceSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        SpawnDecisionService decisionService = decisionServiceSupplier.get();
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
