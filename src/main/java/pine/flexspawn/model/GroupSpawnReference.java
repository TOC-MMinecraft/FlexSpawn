package pine.flexspawn.model;

// 用法：表示某个出生场景在 config.yml 中引用的坐标组和可选坐标点。
import java.util.Objects;

public record GroupSpawnReference(
        String group,
        String point
) {

    public GroupSpawnReference {
        Objects.requireNonNull(group, "group");
        if (group.isBlank()) {
            throw new IllegalArgumentException("group 不能为空。");
        }
        if (point != null && point.isBlank()) {
            point = null;
        }
    }

    public boolean hasPoint() {
        return point != null;
    }
}
