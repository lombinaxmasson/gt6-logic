# 重新设计

现在的逻辑元件不能用来搭电路。这里有两份互斥的方案，**选定的是方案 B**：

- [`b-projectred-bridge.md`](b-projectred-bridge.md)（选定）：开关量逻辑交给 ProjectRed，本模组让 GT 线缆和 GT 机器接进 ProjectRed 的电路。
- [`a-native-gates.md`](a-native-gates.md)（未采用）：自己做 GT 原生的门，照 ProjectRed 的端口规则，不依赖 ProjectRed。ProjectRed 已经把这块做得很完善，自己做只会是下位替代。

两份都从同一个问题开始：一个元件怎样从两个方向收两路信号，从第三个方向送出去，再接到下一个元件。

## 现在错在哪

这一节两份方案共用。

**端口在错误的轴上。** [`LogicPlateBlock`](../../src/main/java/com/lombinaxmasson/gt6logic/block/LogicPlateBlock.java) 是贴在方块面上的板，`FACING` 指向它贴着的那个方块。联锁、算术、可编程、比例、配比都把 `FACING` 当输入 A，把反方向当输入 B。玩家真放下去时，A 就是背后那块墙，真正能接线的只剩 B。双输入门在游戏里摆不出来。

**输出没有方向。** [`FaceSignals.send`](../../src/main/java/com/lombinaxmasson/gt6logic/block/FaceSignals.java) 往除输入以外的每一面送同一个值。门的四个侧面全是输出，接在侧面的线分不清是在喂门还是在读门。两个门贴在一起时，也说不清谁是谁的输入。

**测试绕开了摆放。** GameTest 用 `setBlockAndUpdate` 把门悬空放下，再在两侧放 [`SignalProbe`](../../src/gameTest/java/com/lombinaxmasson/gt6logic/gametest/SignalProbe.java)。这种测试从不经过 `getStateForPlacement`，从不检查支撑面，也不连真的线。62 个测试证明的是一个玩家摆不出来的结构。

**先定了方块清单，没定连接规则。** 一共 31 块，每块各自决定读哪面、写哪面。没有一条“端口”的定义，所以元件之间不能组合。

## 两边的事实

两份方案都建立在这些已核实的事实上。

GT6CE 信号线（`SignalWireBlock`，对照 `c899462a6` 字节码）：

- 占满一格。六个面各有一个连接开关，是方块状态里的布尔值。
- **放下时只连向被点的那一面**（`getStateForPlacement` 只把 `clickedFace.getOpposite()` 设成 true）。其余几面要用工具再接（`WireToolInteractions.use`）。
- 一张连通的线网每拍只算一次，取所有源里的**最大值**往外传，每过一格减 `UNIT / range`。两路信号进同一张线网会被合并成一个最大值，所以两路输入必须是两张互不相连的线网。
- 读邻居的顺序是：`GtSignalReceiver` 整块跳过，`GtSignalSource` 读 `gtSignal(side)`，否则读原版 `getSignal * UNIT`。

ProjectRed（1.21.1 分支，正式版 4.23.0，NeoForge）：

- 门贴在实心方块的一个面上，支撑方块没了门就掉。
- 门在贴着的那一面上有 4 个侧面，从 0 编到 3。0 是放置时背向玩家的那一侧，顺时针递增。端口都在这一平面里，不在贴着的那根轴上。
- 与门 3 个输入、1 个输出。输出在 0 号侧（玩家朝向），左、后、右是输入。螺丝刀右键旋转，潜行右键依次关掉某些输入。只看已启用的输入。
- 依赖 CodeChicken Lib 和 CB Multipart。开发接口在 `https://proxy-maven.covers1624.net/`，坐标是 `mrtjp:ProjectRed-1.21.1:<版本>:api`。
- 公开接口有 `IConnectable`、`IBundledEmitter`、`IBundledTile`、`IMaskedBundledTile`、`IBundledTileInteraction`、`ITransmissionAPI`。捆绑线缆是 16 色，每色 0–255。

## 怎么选

| | A 原生门 | B ProjectRed 衔接 |
|---|---|---|
| 玩家要装 | 只有 GT6CE | GT6CE、ProjectRed（核心、集成、传输）、CB Multipart、CodeChicken Lib |
| 开关量逻辑 | 自己做与、或、非、异或、锁存、计时、计数 | ProjectRed 现成的二十多种门和集成电路 |
| 小数信号 | 门里全程保留 | 只在 GT 一侧保留，进 ProjectRed 变成 0–15 |
| 多路总线 | 没有，GT 线一张网只有一个数 | 捆绑线缆 16 路 |
| 本模组的工作量 | 门的放置、旋转、渲染、端口、全套门 | 转接块、传感器、执行器、连续量模块 |
| 主要风险 | 做出来的门不如 ProjectRed 好用 | 依赖链长，跟着 ProjectRed 和 CB Multipart 的版本走 |
