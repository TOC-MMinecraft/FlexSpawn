package pine.flexspawn.listener;

// 用法：仅在玩家因死亡重生时覆盖目标点，不干扰其他重生流程。
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.service.SpawnDecisionService;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerRespawnListener implements Listener {

    private final JavaPlugin plugin;
    private final Supplier<SpawnDecisionService> decisionServiceSupplier;

    public PlayerRespawnListener(JavaPlugin plugin, Supplier<SpawnDecisionService> decisionServiceSupplier) {
        this.plugin = plugin;
        this.decisionServiceSupplier = decisionServiceSupplier;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) {
            return;
        }

        Optional<GroupSpawnSelectionResult> result = decisionServiceSupplier.get().resolveRespawnLocation(event.getPlayer());
        if (result.isEmpty()) {
            return;
        }

        GroupSpawnSelectionResult selectionResult = result.get();
        if (selectionResult.isSuccess()) {
            event.setRespawnLocation(selectionResult.locationData().toLocation(event.getPlayer().getServer()));
            return;
        }

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> event.getPlayer().sendMessage(selectionResult.denialMessage())
        );
    }
}
