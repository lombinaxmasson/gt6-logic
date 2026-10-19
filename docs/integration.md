# 与 GT6CE 的对接

本文记录 GT6 Logic 怎样接到 GT6CE 上：依赖哪个版本、用到哪些 API、这些 API 的实际行为，以及还缺什么。凡是写“已核实”的条目，都是对照 GT6CE 源码 `c899462a6` 确认过的；写“待验证”的条目还没有在游戏里试过。

## 依赖与版本

- 编译只依赖 GT6CE 的 NeoForge 附属 SDK：`libs/gt6ce/gregtech6-addon-sdk-1.21.1-neoforge-0.7.0.jar`，`compileOnly`。SDK 之外的 GT 类一律看不见，也不要通过反射去碰。
- 开发运行和 GameTest 用完整的 `libs/gt6ce/gregtech6-1.21.1-neoforge.jar`，`localRuntime`，不打进成品。
- 模组依赖写 `gregtech6`，`versionRange="[0.7,)"`。GT 的内容和存档命名空间仍是 `gregtech`。
- 钉住的上游提交写在 `libs/gt6ce/build-commit.txt`。CI 会检出这个提交，自己构建 mod jar 和 SDK，所以它必须是 SaltNya/GregTech6 上已经合并、并且能产出 NeoForge SDK 的提交。
- 本地更新：运行 `update-gt.ps1`，它会下载上游 `main` 最近一次成功构建的产物。下载完要同步更新 `build-commit.txt` 和 `gregtech6-1.21.1-neoforge.jar.sha256`。
- 只做 NeoForge 1.21.1。Forge 1.20.1 暂不考虑。

## 用到的 API

### 信号线（核心）

SDK 里的三个接口：`GtSignalCarrier`、`GtSignalSource`、`GtSignalReceiver`，都在 `com.gregtech.gregtech.api.signal`。

数值约定（已核实）：

- `GtSignalCarrier.UNIT` 等于一级原版红石，数值是 `Integer.MAX_VALUE`（约 \(2^{31}\)）。满信号 `15 * UNIT` 仍然放得进 `long`。
- `GtSignalCarrier.clamp` 把任意值夹到 \([0, 15 \cdot \text{UNIT}]\)。
- 精细信号只在 GT 信号线内部保持分数。信号线对外接原版方块时，按向上取整给出 0–15。

信号线怎样从邻居取值（已核实，见 GT6CE `SignalWireBlockEntity.tickNetwork`）：

1. 每个服务端 tick，一个连通的信号线网络整体重算一次。
2. 对每根线的每个已连接面，依次判断邻居：
   - 是信号线、发射器、漏斗、活塞、TNT、音符盒、活板门、门、动力铁轨、红石灯，跳过；
   - 方块在 `gregtech:signal_wire_receivers` 方块标签里，跳过；
   - 方块实体实现了 `GtSignalReceiver`，**整个方块跳过，所有面都不读**；
   - 方块实体实现了 `GtSignalSource`，读 `gtSignal(side)` 并夹紧；
   - 否则读原版 `level.getSignal(pos, side) * UNIT`。
3. 网络里取**最大值**传播，每过一根线减去 `UNIT / range`（`range` 由线的种类决定）。多个信号源不会相加。
4. 线记住信号是从哪一面来的，不会往回输出。

由此得出的写法规则：

- **元件只实现 `GtSignalSource`，不要同时实现 `GtSignalReceiver`。** 两个都实现时按接收端处理，输出会被吞掉。不需要输出的面，在 `gtSignal(side)` 里返回 0。
- **只接收、不输出的方块**（显示屏、报警器、执行器）实现 `GtSignalReceiver`，或者加进 `gregtech:signal_wire_receivers` 标签。
- **`gtSignal(side)` 必须是纯读取**：直接返回缓存好的值，不要在里面读世界。信号线采样期间，GT 线自己的 `output()` 会返回 0，在采样回调里再去读别的线会读到错误的值。
- **读输入**：相邻方块实体是 `GtSignalCarrier` 时读 `signal()`，信号线的分数留着。否则若它是 `GtSignalSource`，读已经发布的 `gtSignal(从自己指向它的方向)`，两块贴在一起也不会把分数取整掉。都不是的话，读 `level.getSignal(neighborPos, dir) * UNIT`，原版红石也能当输入。
- **双缓冲**：信号线和元件谁先 tick 不确定。元件在自己的 tick 里算出新值，到 tick 末尾再发布给 `gtSignal`。这样每个元件固定延迟 1 tick，结果和放置顺序无关。
- **溢出**：两个接近满值的信号直接相乘会超出 `long`，要用 `Math.multiplyHigh` 或先缩放再乘。
- `side` 按 `Direction.ordinal()`：0 下、1 上、2 北、3 南、4 西、5 东，是从信号线指向本方块的方向。
- 对原版方块也要输出：方块重写 `getSignal` / `getDirectSignal`，把精细值向上取整到 0–15。

### 本体传感器（读得到什么、输出什么）

GT6CE 有 20 种传感器面板（`SensorBlockEntity`），8 种模式：显示、大于、等于、小于、缩放、百分比、满、未满，可以对读数取平均。已核实的行为：

- **输出只有 0–15 的整数级。** 传感器不实现 `GtSignalSource`，信号线读到的是 `getSignal * UNIT`。所以从本体传感器来的信号没有小数部分。
- **流体和物品类传感器**（液位、物品、容量、重量）通过 NeoForge 能力读目标：`Capabilities.FluidHandler.BLOCK`、`Capabilities.ItemHandler.BLOCK`。所以**我们自己的方块只要注册了这两种能力，本体传感器就能读**（待验证）。
- **进度传感器**读 `ProgressSensorSource` 接口或 GT 的机器控制。这个接口在 GT 的 `core/src/main` 里，**不在 SDK 里**，附属实现不了。
- **温度计**只认 GT 的坩埚，没有通用接口。
- **电量传感器**只读 GT 的电线。

结论：我们的工艺机器要对外暴露温度、压力这类读数，不能指望本体传感器，要自己在指定面实现 `GtSignalSource`。液位可以同时走能力，让本体传感器也能读。

### 行为机器

`BehaviorMachineDeclaration` + `BehaviorMachineContributor` + `MachineBehavior`。适合做“吃电、有物品槽和储罐、GUI 简单”的工艺机器。已核实的限制：

- 必须接能源：`energyFaces` 不能为空，`energyPerTick` 至少 1。不用电的东西不能用行为机器做。
- **不能输出信号**：外壳不转发 `gtSignal`，`MachineBehavior` 也没有相关方法。
- **可以读信号**：`MachineBehaviorContext.level()` 和 `pos()` 可用，行为可以自己读相邻信号线的 `signal()` 或原版红石。
- GUI 只能用声明的控件：物品槽、储罐、能量条、进度箭头、开关。没有数字输入、滑条、曲线。
- 只有单方块。

结论：控制元件（恒温器、浮球开关、定时器……）一律写成**附属自己的方块和方块实体**，GUI 自己画。工艺机器先看需求：只需要“被控制”、不需要“报告读数”的，可以用行为机器；需要往外报读数的，自己写方块实体。

### 其他会用到的 API

- `AddonTools.isMagnifyingGlass` / `AddonTools.matches`：放大镜右键读精确值。`AddonTool` 目前只有 `SPADE`、`UNIVERSAL_SPADE`、`MAGNIFYING_GLASS`，**没有螺丝刀、扳手、软锤**。
- `EnergyBlock` / `AddonEnergy`：附属自己的方块实现 `EnergyBlock`，GT 的 EU 电缆就能往里送电或抽电。用电的执行器（加热器、电动阀）走这里。
- `AddonRecipes`：往已有的 GT 配方表加行，例如元件的组装配方。合成表配方用自己的数据包。
- `AddonFluids`：按 GT 流体字段名取流体，例如冷却水、蒸汽。
- `AddonMaterialContributor`：如果工艺线需要新材料（例如硝化产物），按名字声明。
- `AddonMachineContributor` 的燃料表和燃料发电机：如果做燃烧配比类的玩法会用到。

## 本附属已经定下的接法

这些是这块附属自己的约定，GameTest 或 JUnit 盖过的才写在这里。

- **电的短名**是 `EU`，热量是 `HU`，冷量是 `CU`。用电的方块实现 `EnergyBlock`，并且只重写 `doInject` / `doExtract`。`doEnergyInjection` 留在接口默认实现上，电压门先跑。推荐输入是 LV，32 EU 一包。HU 和 CU 的推荐输出大小是 1，小的量也能过门。
- **电表**是自己的电容器，容量 10240 EU，六个面既进也出。它把缓冲的充满比例写成信号，不是去读别人的电池。管家只读这路信号。
- **加热器和冷却器**把这一拍花掉的 EU 原数提供成 HU 或 CU。没抽走的下一拍就没了。它们不改 GT 坩埚的温度。
- **储罐**在每个面注册 `Capabilities.FluidHandler.BLOCK`，侧面为空的查询也指向同一只 16000 mB 的罐子。打破储罐，流体消失。
- **阀和泵**不提供流体能力。它们在自己的 tick 里向入口邻居抽、向出口邻居灌，邻居要有 `IFluidHandler`。GameTest 用两只储罐。GT 流体管只要那一面有这个能力就能接，这一轮没有在游戏里接过管子。
- **温度**：0 信号是 0 °C，满信号是 150 °C。机器若能报毫摄氏度，实现 `TemperatureSource`。没有这个接口时，恒温器和程序控温器把邻面的信号译成温度。本体坩埚仍读不到。

## 需要向上游提的 API

按对本附属的价值从高到低：

1. **让本体传感器实现 `GtSignalSource`**，输出不取整的精细值。改动很小，但这样本体所有读数（进度、电量、温度、液位）都能精确地接进我们的元件。
2. **把 `ProgressSensorSource` 放进 SDK**，再加一个通用的温度读数接口，让附属机器能被本体的进度传感器和温度计读到。
3. **`AddonTool` 加上螺丝刀、扳手、软锤**，让元件能按 GT6 的习惯用工具调参数。
4. **行为机器支持输出信号**：在 `MachineBehavior` 里加一个可选的 `gtSignal(context, side)`，或者在 `MachineBehaviorContext` 里加 `emitSignal(side, value)`。
5. **行为机器允许不接能源**，用于纯机械或纯红石的方块。

提请求时附上本附属里的具体用例和复现方式，参考 gt6-crops 的 `docs/gt6ce-crop-api-request.md` 的写法。

## 测试

- 纯逻辑（数值换算、回差判定、定时器状态机）写 JUnit，放 `src/test`，不依赖游戏。
- 和信号线的交互写 GameTest，放 `src/gameTest`。GT6CE 自己的 `SignalWireAddonTests` 是“附属信号源接到信号线上”的现成样例，可以照着写。
- `gradlew runGameTestServer` 跑全部 GameTest。没有任何 GameTest 时服务端会报错退出，这是正常的。
