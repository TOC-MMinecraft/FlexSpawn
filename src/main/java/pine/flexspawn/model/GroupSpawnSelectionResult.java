package pine.flexspawn.model;

// 用法：统一表示公共出生点解析成功或被拒绝的结果。
import java.util.Objects;

public record GroupSpawnSelectionResult(
        LocationData locationData,
        String denialMessage
) {

    public GroupSpawnSelectionResult {
        if ((locationData == null) == (denialMessage == null)) {
            throw new IllegalArgumentException("必须且只能提供 locationData 或 denialMessage。");
        }
    }

    public static GroupSpawnSelectionResult success(LocationData locationData) {
        return new GroupSpawnSelectionResult(Objects.requireNonNull(locationData, "locationData"), null);
    }

    public static GroupSpawnSelectionResult denied(String denialMessage) {
        String message = Objects.requireNonNull(denialMessage, "denialMessage");
        if (message.isBlank()) {
            throw new IllegalArgumentException("denialMessage 不能为空。");
        }
        return new GroupSpawnSelectionResult(null, message);
    }

    public boolean isSuccess() {
        return locationData != null;
    }
}
