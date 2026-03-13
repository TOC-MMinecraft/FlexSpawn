package pine.flexspawn.model;

// 用法：表示从权限组中解析出的候选坐标点及其权重。
import java.util.Objects;

public record WeightedLocationEntry(
        String permissionGroup,
        String pointName,
        int weight,
        LocationData locationData
) {

    public WeightedLocationEntry {
        Objects.requireNonNull(permissionGroup, "permissionGroup");
        Objects.requireNonNull(pointName, "pointName");
        Objects.requireNonNull(locationData, "locationData");
        if (permissionGroup.isBlank()) {
            throw new IllegalArgumentException("permissionGroup 不能为空。");
        }
        if (pointName.isBlank()) {
            throw new IllegalArgumentException("pointName 不能为空。");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight 必须大于 0。");
        }
    }
}
