package pine.spawnonjoin.model;

// 用法：统一保存可序列化的位置数据，避免业务层直接依赖 YAML 字段。
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

public record LocationData(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public LocationData {
        Objects.requireNonNull(world, "world");
        if (world.isBlank()) {
            throw new IllegalArgumentException("world 不能为空。");
        }
    }

    public static LocationData fromLocation(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location.world");
        return new LocationData(
                world.getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Location toLocation(Server server) {
        World targetWorld = server.getWorld(world);
        if (targetWorld == null) {
            throw new IllegalStateException("未找到世界: " + world);
        }
        return new Location(targetWorld, x, y, z, yaw, pitch);
    }
}
