package pine.flexspawn.listener;

// 用法：仅在玩家因死亡重生时覆盖目标点，不干扰末地回城或其他插件的重生流程。
import pine.flexspawn.model.GroupSpawnSelectionResult;
import pine.flexspawn.service.SpawnDecisionService;
import java.util.Optional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerRespawnListener implements Listener {

    private final JavaPlugin plugin;
    private final SpawnDecisionService decisionService;

    public PlayerRespawnListener(JavaPlugin plugin, SpawnDecisionService decisionService) {
        this.plugin = plugin;
        this.decisionService = decisionService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) {
            return;
        }

        Optional<GroupSpawnSelectionResult> result = decisionService.resolveRespawnLocation(event.getPlayer());
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