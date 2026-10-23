# 删掉与 GT 重复的方块详细计划

> 计划 slug：`rewrite/remove-gt-duplicates`
> 状态：已关闭。本文件位于 `card-plans/closed/`。清单按 [`供给计量方向卡`](../active/供给计量方向卡.md) 第 2 节冻结。
> 性质：先清场。删掉 GT6 本体已经覆盖的 11 块，留下只属于本模组的 11 块。不是新功能卡。

```text
lane            = closed
slug            = rewrite/remove-gt-duplicates
owned_paths     = src/main/java/com/lombinaxmasson/gt6logic/Gt6Logic.java
                  src/main/java/com/lombinaxmasson/gt6logic/client/Gt6LogicClient.java
                  src/main/java/com/lombinaxmasson/gt6logic/client/DigitDisplayRenderer.java
                  src/main/java/com/lombinaxmasson/gt6logic/block/{AdjustableCooler,AdjustableHeater,BatterySteward,DigitDisplay,ElectricMeter,ElectricValve,FloatSwitch,LevelGauge,ProgramThermostat,Tank,Thermostat}*.java
                  src/main/java/com/lombinaxmasson/gt6logic/block/{BandText,FluidCapabilities,FluidLevel,HysteresisPlate,ThermalBlockEntity}.java
                  src/main/java/com/lombinaxmasson/gt6logic/control/{Band,DisplayReading,MachineBands,PowerDraw,Temperatures,TemperatureSchedule}.java
                  src/main/java/com/lombinaxmasson/gt6logic/heat/{TemperatureInputs,TemperatureSource}.java
                  src/main/resources/assets/gt6logic/blockstates/{adjustable_cooler,adjustable_heater,battery_steward,digit_display,electric_meter,electric_valve,float_switch,level_gauge,program_thermostat,tank,thermostat}.json
                  src/main/resources/assets/gt6logic/models/block/{adjustable_cooler,adjustable_cooler_on,adjustable_heater,adjustable_heater_on,battery_steward,battery_steward_on,digit_display,electric_meter,electric_meter_on,electric_valve,electric_valve_on,float_switch,float_switch_on,level_gauge,level_gauge_on,level_mark,program_thermostat,program_thermostat_on,tank,thermostat,thermostat_on}.json
                  src/main/resources/assets/gt6logic/models/item/{adjustable_cooler,adjustable_heater,battery_steward,digit_display,electric_meter,electric_valve,float_switch,level_gauge,program_thermostat,tank,thermostat}.json
                  src/main/resources/assets/gt6logic/textures/block/{adjustable_cooler,adjustable_cooler_on,adjustable_heater,adjustable_heater_on,battery_steward,digit_display,electric_meter,electric_meter_on,electric_valve,electric_valve_on,float_switch,level_gauge,program_thermostat,tank,tank_top,thermostat}.png
                  src/main/resources/assets/gt6logic/lang/{en_us,zh_cn}.json
                  src/main/resources/data/gt6logic/loot_table/blocks/{adjustable_cooler,adjustable_heater,battery_steward,digit_display,electric_meter,electric_valve,float_switch,level_gauge,program_thermostat,tank,thermostat}.json
                  src/gameTest/java/com/lombinaxmasson/gt6logic/gametest/{FirstCircuitTests,FloatAndHeatTests,LevelGaugeTests}.java
                  src/gameTest/java/com/lombinaxmasson/gt6logic/gametest/{AnalogPlateTests,Gt6FluidInteropTests,GameTestSupport,ReadoutFaceTests,RoomReadoutTests,SecondTierTabTests,SignalMathTests,SignalProbe,ValveTests}.java
                  src/test/java/com/lombinaxmasson/gt6logic/control/{RemainingControlTest,RoomControlTest}.java
                  README.md
                  docs/components.md
                  docs/content.md
                  docs/how-logic-works.md
                  docs/integration.md
depends_on      = direction/supply-metering
close_target    = 注册表从 22 块变成 11 块；删除的 id 不再出现；保留的 11 块注册、资源、测试、文档一致；gradlew test 和 gradlew runGameTestServer 通过
```

## 0. 它解决什么

方向卡确认了：GT6 本体已经有测量、阈值比较、机器开关、管道开关、定时和防满罐。本模组继续保留这些方块，只会让玩家分不清该用哪一套，也让后面的死区继电器和比例流量卡背着旧代码走。

这张卡把方向卡第 2 节的“候选”冻结成决定，并一次清干净：22 块留下 11 块。

## 1. 冻结的清单

### 1.1 保留 11 块

| 方块 | id | 为什么留 |
|---|---|---|
| 调节阀 | `control_valve` | GT 没有按信号连续变化的流量执行器；GT 管道回路已验证 |
| 可调泵 | `adjustable_pump` | 同上，满信号 1000 mB/tick |
| 信号表 | `signal_meter` | GT 没有 GT 线细值读出面板 |
| 补偿中继器 | `compensation_repeater` | GT 线每跳都衰减，本体没有补线损的方块 |
| 捆绑线缆接口 | `bundled_tap` | GT 细值 ↔ ProjectRed 捆绑线缆，本体没有 |
| 算术模块 | `arithmetic` | 本体没有连续量运算 |
| 比例控制器 | `proportional_controller` | 同上 |
| 配比控制器 | `ratio_controller` | 同上 |
| 可编程模块 | `formula_module` | 同上 |
| 指针表 | `pointer_gauge` | 本体六格显示只有当前值，没有指针和一眼可读的开度 |
| 记录仪 | `chart_recorder` | 本体没有历史曲线 |

### 1.2 删除 11 块

| 方块 | GT 本体替代 | 为什么删 |
|---|---|---|
| 可调加热器 | Electric / Flux Heater | 功能重复，且我们的 HU 没有消费者 |
| 可调冷却器 | Thermoelectric / Flux Cooler | 同上 |
| 电表 | Electrometer、energy sensor 覆盖板 | 我们只读自家 10240 EU 缓冲，视野比本体窄 |
| 蓄电池管家 | 电表 + 机器开关 | 本体已经能用传感器驱动机器开关；独特处只剩回差 |
| 浮球开关 | fluidometer + 机器开关 | 本体能测液位并做阈值判断；独特处只剩回差 |
| 恒温器 | thermometer + 机器开关 | 本体温度计只读坩埚，但我们的恒温器也读不到本体机器 |
| 液位计 | fluidometer 的百分比/缩放模式 | 本体已经输出连续 0–15；我们只多出小数 |
| 电动阀 | shutter / 机器开关 | 开关量的通断，本体已有 |
| 储罐 | GT 的桶、储罐、大储罐 | 本体有完整流体存储；它只是浮球开关的测试夹具 |
| 数码管 | 传感器面板的六格显示 | 本体显示更贴身；我们只多出“读 GT 线”一种用法 |
| 程序控温器 | 无直接替代 | 唯一的物理去向是加热器/冷却器；两者都删了 |

### 1.3 这张卡新增的判断

- **程序控温器不在方向卡的保留名单里，也没有出现在候选删除名单里。** 这张卡把它归入删除：它按糖化曲线输出加热/冷却需求，而加热器和冷却器都要删，留下它没有执行器。热量路径卡需要时再加回来。
- **指针表和记录仪保留，即使本体有六格数字显示。** 六格显示是“现在是多少”，指针和记录仪是“开了多大”和“刚才有没有冲出去”。这两件事本体没有。
- **回差不是永久删除。** 浮球开关、恒温器、蓄电池管家删掉的是方块，回差本身由下一张 `rewrite/deadband-relay` 做成通用继电器。

## 2. 行为

### 2.1 注册与代码

- `Gt6Logic` 去掉 11 个 `bootstrap()` 调用和对应的 import。创造栏图标从电表换成信号表，因为电表要删。
- 删掉 11 块方块类与方块实体类，以及只为它们服务的 `BandText`、`FluidCapabilities`、`FluidLevel`、`HysteresisPlate`、`ThermalBlockEntity`、`DigitDisplayRenderer`。
- 删掉只被删掉方块使用的纯逻辑：`Band`、`MachineBands`、`PowerDraw`、`DisplayReading`、`Temperatures`、`TemperatureSchedule`、`TemperatureInputs`、`TemperatureSource`。
- `SignalProbe` 去掉 `TemperatureSource` 实现和温度上报字段/方法；删掉的热相关测试是它们唯一的使用者。
- **保留**共享基础设施：`SignalBlock`、`SignalBlockEntity`、`PublishedSignal`、`SignalLevels`、`SignalInputs`、`FaceSignals`、`NeighborSignals`、`PlateShapes`、`GatePorts`、`PlateOutput`、`ReceiverPlate`、`BlockFeedback`、`FlowKind`、`FlowBlockEntity`、`PoweredEntity`、`EuStore`、`EuPort`、`FluidMove`、`Presets`、`HysteresisSwitch`（其中 `Mode` 仍被比例控制和比例控制器使用）。
- 不重命名、不迁移任何保留方块的 id。

### 2.2 资源与语言

- 删掉 1.2 表中 11 块的 blockstate、方块模型、物品模型、纹理、掉落表和语言条目。
- 顺带删掉只为它们存在的 `level_mark.json`、`tank_top.png`。
- 保留共用模型和纹理：`plate.json`、`analog_plate.json`、`readout_plate.json`、`plate_edge.png`、`port_signal_in.png`、`port_signal_out.png` 等。
- 创造栏由 `ITEMS` 动态生成，不需要手改清单；但 `SecondTierTabTests` 里的期望名单要改。

### 2.3 测试迁移

**整文件删除：**

- `FirstCircuitTests`：浮球开关 + 恒温器 + 电动阀的旧旗舰电路，四块被测方块都要删。ProjectRed 驱动我们执行器的电路测试留到 `rewrite/proportional-flow` 补。
- `FloatAndHeatTests`：浮球、恒温、电表、加热器、冷却器。
- `LevelGaugeTests`：液位计。

**删方法、留其余：**

- `ValveTests`：删 `electricValveNeedsSignalAndPower`、`electricValveIgnoresTheRightSide`、`projectRedGateDrivesElectricValve` 和 `placeValve`；保留调节阀、可调泵的两条，并把测试夹具从本模组储罐换成 GT 的 `drum_bronze`。
- `ReadoutFaceTests`：删 `digitDisplayReadsOnlyTheFreeFace`；信号表、指针表、记录仪三条保留。
- `RoomReadoutTests`：删两条数码管测试；指针和记录仪两条保留。
- `AnalogPlateTests`：`fivePlatesFaceTheWayThePlayerLooks` 变成四块板，去掉程序控温器。
- `SignalMathTests`：删 `programStartsAtTheMashRest`；其余保留。
- `RemainingControlTest`：删 `temperatureRoundTripsOnTenDegreeSteps`、`mashScheduleHoldsTheLastStep`、`flowAndPortionAndPower`；其余保留。
- `RoomControlTest`：删 `digitDisplaySpeaksThreeUnits`；指针和记录仪两条保留。
- `SecondTierTabTests`：期望名单改成保留的 11 个 id。
- `Gt6FluidInteropTests`：入口储罐换成 GT `drum_bronze`，比例回路的断言不变。
- `GameTestSupport`：如果多个测试都要 GT 桶，加一个按注册名放 GT 桶并取流体处理器的辅助方法，避免每个测试各写一份。

**保留但需要复核：**

- `ProjectRedInteropTests`、`BundledTapTests`、`CompensationRepeaterTests`、`ArithmeticTests`、`SignalMeterTests`、`SignalContractTests`。它们不引用被删方块，但要在 `gradlew runGameTestServer` 里确认没有间接依赖。

### 2.4 文档

- `README.md`：把“18 块逻辑方块和 5 块执行器”的旧数字改成删除后的实际数量，并说明 GT6 本体承担测量和开关控制。
- `docs/components.md`：删掉 1.2 表里的方块，保留表改成“GT 本体没有的那一半”，并补一句每块为什么不是重复。
- `docs/content.md`：删掉对应条目和分档里的旧内容，保留设计原则。
- `docs/how-logic-works.md`：删掉浮球/恒温/液位/电表/加热器的算例，保留算术、比例、配比、公式、回差本身。
- `docs/integration.md`：删掉储罐、电表、加热器的接法；把 `Gt6FluidInteropTests` 验证出的管道端口规则、机器进液面规则、缓冲会削弱限流写进去。
- `docs/redesign/README.md`：在开头加一句“开关量仍交给 ProjectRed；测量和开关控制交给 GT6 本体；本模组只做连续量”。不改方案 B 的历史结论。

## 3. 明确不做

- 不做死区继电器。那是 `rewrite/deadband-relay`。
- 不改调节阀、可调泵的行为。那是 `rewrite/proportional-flow`。
- 不做配方和电压档接入。那是 `rewrite/recipes`。
- 不做热量路径。等上游确认 HU/CU 消费者再做。
- 不做存档迁移。模组还没有对外发布，旧存档里的这些方块会变成空气。
- 不删共享信号、能量和端口基础设施。保留的 11 块还在用。
- 不顺手改保留方块的端口、模型或数值。

## 4. 验收

- [x] 11 个删除 id 不再出现在注册表、创造栏、blockstate、模型、物品模型、纹理、掉落表和语言文件里
- [x] 11 个保留 id 仍然能放下、能从创造栏拿到、中英文名称正确
- [x] 代码和现行测试里搜不到被删的方块类、`HysteresisPlate`、`FluidLevel`、`ThermalBlockEntity`、`DisplayReading`、`PowerDraw`、`TemperatureSchedule`、`TemperatureInputs`、`TemperatureSource`
- [x] 阀门和泵的测试使用 GT 桶作为流体夹具，不再依赖本模组储罐
- [x] `gradlew test` 通过
- [x] `gradlew runGameTestServer` 通过，剩余 38 个测试
- [x] 文档按 2.4 节更新，`README.md` 的数字与实际注册数一致
- [x] 没有存档迁移代码
