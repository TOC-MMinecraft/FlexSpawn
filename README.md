# FlexSpawn

`FlexSpawn` 是一个的出生与重生点控制插件。

它会按场景接管玩家的出生或重生位置，并将玩家档案与可选的个人存档点持久化到 `players.yml`。

## 适用环境

- `Java 21`
- `Paper 1.21.x`

## 安装

1. 构建得到插件 JAR。
2. 将 JAR 放入服务器 `plugins/` 目录。
3. 启动服务器一次，生成默认配置文件。
4. 按需修改 `plugins/FlexSpawn/config.yml` 与 `plugins/FlexSpawn/groups.yml`。
5. 重启服务器使配置生效。

## 生成的文件

- `plugins/FlexSpawn/config.yml`
- `plugins/FlexSpawn/groups.yml`
- `plugins/FlexSpawn/players.yml`

## 场景行为

| 场景 | 行为 |
| --- | --- |
| 玩家首次加入服务器 | `first-join.enabled=true` 时按 `first-join` 解析；否则回退到 `join-default` |
| 玩家再次加入服务器 | 先判断个人存档点；未命中时按 `join-default` 解析 |
| 玩家因死亡重生 | 先判断个人存档点；未命中时按 `death-respawn` 解析 |
| 玩家通过床或重生锚设置出生点 | `personal-spawn.enabled=true` 时同步写入或清理 `players.yml` 中的个人 `spawn` |

补充规则：
- 首次加入的判断标准是 `players.yml` 中是否已存在该玩家的 UUID 档案。
- 当 `first-join.enabled=false` 且 `join-default.enabled=false` 时，首次加入场景不接管。
- 当 `personal-spawn.enabled=false` 时，插件不会读取、写入或清理个人 `spawn`，但仍会维护玩家 UUID 档案。
- 坐标组名、权限组名和坐标点名在匹配时都不区分大小写。
- 插件只覆盖“死亡重生”场景，不会干预其他类型的重生原因。
- 玩家命中多个权限组时，会合并这些权限组下的候选坐标点，并按 `weight` 加权随机。
- 当坐标组内没有命中的权限组时，插件会尝试使用 `Default` 权限组。
- 如果既没有命中的权限组也没有 `Default`，则本次公共出生点接管失败，并提示“你没有权限”。

## 配置说明

默认 `config.yml`：

```yaml
first-join:
  enabled: true
  group: first-join

join-default:
  enabled: true
  group: join-default

death-respawn:
  enabled: true
  group: death-respawn

personal-spawn:
  enabled: true

no-permission-message: 你没有权限

spawn-update-message:
  chat:
    enabled: true
    text: 已设置重生点
  action-bar:
    enabled: true
    text: 已设置重生点
```

### `first-join`

控制首次加入场景。

同下

### `join-default`

控制非首次加入且未命中个人存档点时的出生位置。

同下

### `death-respawn`

控制死亡重生且未命中个人存档点时的出生位置。

字段说明：
- `enabled`：是否接管死亡重生场景。
- `group`：目标坐标组名。
- `point`：可选，指定固定坐标点。

### `personal-spawn`

控制是否接管个人存档点。

字段说明：
- `enabled`：
  - `true`：读取 `players.yml` 中的个人 `spawn`，并监听床/重生锚事件同步更新。
  - `false`：不读取、不写入、不清理个人 `spawn`，但仍保留玩家档案用于首次加入判断。

### `groups.yml`

`groups.yml` 用于定义共享出生点组，结构如下：

```yaml
first-join:
  Default:
    point1:
      world: world
      x: 2760
      y: 72
      z: 2453
      yaw: 0.0
      pitch: 0.0
      weight: 1

join-default:
  Default:
    point1:
      world: world
      x: 2760
      y: 72
      z: 2453
      yaw: 0.0
      pitch: 0.0
      weight: 1
  vip:
    point1:
      world: world
      x: 2780
      y: 72
      z: 2453
      yaw: 0.0
      pitch: 0.0
      weight: 3

death-respawn:
  Default:
    point1:
      world: world
      x: 2760
      y: 72
      z: 2453
      yaw: 0.0
      pitch: 0.0
      weight: 1
```

规则说明：
- 顶层键表示坐标组名称。
- 第二层键表示权限组名；除 `Default` 外，其它键会自动拼接为 `flexspawn.group.<组名>` 后再做权限判断。
- 第三层键表示坐标点名称。
- `weight` 必须大于 0，用于候选点加权随机。
- 玩家命中多个权限组时，会把这些权限组里的候选点合并。
- 没有命中任何权限组时，会尝试 `Default`。
- 如果既没有命中权限组也没有 `Default`，则本次公共出生点接管失败。

## `players.yml`

`players.yml` 用于保存玩家档案和个人存档点：

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
- `playername` 会在玩家加入或个人存档点变更时同步更新。
- 如果某个玩家存在记录但没有 `spawn` 节点，表示该玩家不是首次加入，但当前没有被插件接管的个人存档点。

## 构建

前提：
- 已安装 `Gradle`
- 已安装 `Java 21`

Windows：

```powershell
gradle build
```

macOS / Linux：

```bash
gradle build
```

构建产物默认位于：

```text
build/libs/FlexSpawn-<version>.jar
```

## 项目结构

- `src/main/java/`：插件源码
- `src/main/resources/plugin.yml`：插件元数据
- `src/main/resources/config.yml`：场景配置
- `src/main/resources/groups.yml`：共享出生点组配置
- `src/main/resources/players.yml`：玩家数据模板

## 许可证

本项目使用 `GNU General Public License v3.0`，详见 [LICENSE](./LICENSE)。
