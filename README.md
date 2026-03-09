# SpawnOnJoin

`SpawnOnJoin` 是一个面向 `Paper 1.21` 的出生点控制插件。

它会接管玩家首次加入、后续加入和死亡重生时的落点，并把玩家通过床或重生锚设置的个人出生点持久化到 `players.yml`。如果玩家没有个人出生点，插件会回退到统一配置的默认重生点。

## 快速开始

### 适用环境

- `Java 21`
- `Paper 1.21.x`

### 安装步骤

1. 从构建产物中取得插件 JAR。
2. 将 JAR 放入服务器的 `plugins/` 目录。
3. 启动服务器一次，生成默认配置文件。
4. 按需修改 `plugins/SpawnOnJoin/config.yml`。
5. 重启服务器使配置生效。

### 安装后会生成的文件

- `plugins/SpawnOnJoin/config.yml`
- `plugins/SpawnOnJoin/players.yml`

## 这个插件会做什么

| 场景 | 行为 |
| --- | --- |
| 玩家首次加入服务器 | 传送到 `first-join` |
| 玩家再次加入服务器，且已有个人出生点 | 传送到个人出生点 |
| 玩家再次加入服务器，但没有个人出生点 | 传送到 `default-respawn` |
| 玩家因死亡重生，且已有个人出生点 | 重生到个人出生点 |
| 玩家因死亡重生，但没有个人出生点 | 重生到 `default-respawn` |
| 玩家通过床或重生锚设置出生点 | 同步更新 `players.yml` 中的个人出生点，并发送聊天栏与 `Action Bar` 提示 |

注意事项：
- 插件只会覆盖“死亡重生”场景，不会干预其他类型的重生原因。
- 插件不会使用世界默认出生点作为兜底，而是始终优先使用个人出生点或 `default-respawn`。
- 是否属于首次加入，是通过 `players.yml` 中是否已经存在该玩家的 `UUID` 记录来判断。

## 配置说明

默认配置文件如下：

```yaml
first-join:
  world: world
  x: 2760
  y: 72
  z: 2453
  yaw: 0.0
  pitch: 0.0

default-respawn:
  world: world
  x: 2760
  y: 72
  z: 2453
  yaw: 0.0
  pitch: 0.0

spawn-update-message:
  chat:
    enabled: true
    text: 已设置重生点
  action-bar:
    enabled: true
    text: 已设置重生点
```

### `first-join`

用于控制玩家第一次进入服务器时的传送位置。

字段说明：
- `world`：目标世界名
- `x`、`y`、`z`：坐标
- `yaw`：水平朝向
- `pitch`：俯仰角

### `default-respawn`

用于控制以下两种情况的统一落点：
- 非首次加入且没有个人出生点的玩家
- 死亡重生且没有个人出生点的玩家

配置要求：
- `world` 必须是服务器上真实存在的世界名
- 所有坐标字段都必须存在且为数值

如果配置缺失或世界不存在，插件会在启用阶段报错并禁用自身。

### `spawn-update-message`

用于控制玩家通过床或重生锚成功更新个人出生点时的提示消息。

字段说明：
- `chat.enabled`：是否启用聊天栏提示。
- `chat.text`：聊天栏提示内容；留空时视为不发送。
- `action-bar.enabled`：是否启用 `Action Bar` 提示。
- `action-bar.text`：`Action Bar` 提示内容；留空时视为不发送。

兼容性说明：
- 如果旧版本配置文件里没有这个节点，插件会使用内置默认值，不会因为缺少该配置而启动失败。

## `players.yml` 数据文件

`players.yml` 用于保存玩家档案和个人出生点，根节点结构如下：

```yaml
players:
  550e8400-e29b-41d4-a716-446655440000:
    playername: Steve
    spawn:
      world: world
      x: 100.5
      y: 64.0
      z: -30.5
      yaw: 90.0
      pitch: 0.0
```

说明：
- `players` 以玩家 `UUID` 作为主键。
- `playername` 会在玩家加入或个人出生点变更时同步更新。
- 如果某个玩家存在记录，但没有 `spawn` 节点，表示该玩家不是首次加入，但当前没有个人出生点。
- 当玩家的个人出生点被清除时，插件会移除对应的 `spawn` 节点。

## 开发与构建

### 本地构建

前提：

- 已安装 `Gradle`
- 已安装 `Java 21`

Windows:

```powershell
gradle build
```

macOS / Linux:

```bash
gradle build
```

构建完成后，产物默认位于：

```text
build/libs/SpawnOnJoin-<version>.jar
```

### 项目结构

- `src/main/java/`：插件源码
- `src/main/resources/plugin.yml`：插件元数据
- `src/main/resources/config.yml`：共享出生点配置
- `src/main/resources/players.yml`：玩家数据模板

## 许可证

本项目使用 `GNU General Public License v3.0`。

详见 [LICENSE](./LICENSE)。
