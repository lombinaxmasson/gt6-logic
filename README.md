# GT6 Logic

GT6CE 的 NeoForge 1.21.1 附属：一批连续量控制元件（调节阀、可调泵、比例控制器、信号表、记录仪……），让 GT6 的机器和流体系统能按实际数值被调节。

目标是补 GT6 和 ProjectRed 都没有的那一半：GT6 本体负责测量和开关控制，ProjectRed 负责开关量逻辑，本模组负责按比例执行、回差、细值读写和连续量运算。

> 11 块方块：2 块连续量执行器、4 块连续量模块、5 块工具与仪表。创造栏图标是信号表。还没有配方。方向和当前分母见 [`docs/components.md`](docs/components.md)。

## 文档

- [`docs/content.md`](docs/content.md)：内容设计。设计原则、元件清单、工艺机器、进度安排。
- [`docs/components.md`](docs/components.md)：逻辑元件总集。18 块逻辑方块、5 块执行器，以及做到哪一步。
- [`docs/how-logic-works.md`](docs/how-logic-works.md)：这些元件在算什么。电平、组合逻辑、时序逻辑，对照到具体方块。
- [`docs/production-ideas.md`](docs/production-ideas.md)：需要控制电路的产线和配方点子，持续收集。
- [`docs/integration.md`](docs/integration.md)：和 GT6CE 的对接。用到的 API、它们的实际行为、要向上游提的接口。
- [`docs/card-plans/`](docs/card-plans/)：计划卡。正在做的在 `active/`，排队的在 `prep/`，做完的在 `closed/`。

## 开发

- 需要 JDK 21。使用 Gradle 前把 `JAVA_HOME` 指向 JDK 21。
- 编译只依赖 GT6CE 的附属 SDK `gregtech6-addon-sdk-1.21.1-neoforge-0.7.0.jar`。`runClient`、`runServer` 和 GameTest 使用完整的 `gregtech6-1.21.1-neoforge.jar`。两个 jar 都不进 git，也不打进成品。
- 运行 `update-gt.ps1` 从 GT6CE 的 NeoForge CI 产物下载这两个 jar（需要已登录的 `gh`）。下载后更新 `libs/gt6ce/build-commit.txt` 和 `gregtech6-1.21.1-neoforge.jar.sha256`。
- 运行 `fetch-projectred.ps1` 从 Modrinth 下载 ProjectRed 4.23.0 和它声明的库到 `libs/projectred/`。这些 jar 不进 git，也不打进成品。成品把 ProjectRed 写成必需依赖，没装时游戏拒绝加载。
- CI 会按 `build-commit.txt` 钉住的 GT6CE 提交自己构建 mod jar 和 SDK，所以这个提交必须已经合并进 SaltNya/GregTech6，并且能产出 NeoForge SDK。
- 常用命令：`gradlew build`、`gradlew runClient`、`gradlew runGameTestServer`。

## 许可

GT6 Logic 使用 GNU Lesser General Public License 第 3 版或（由你选择）任何更新版本。`LICENSE` 是 LGPL-3.0 原文，`COPYING` 是它引用的 GPL-3.0 原文。两份原文也随 jar 发布在 `META-INF/licenses/` 下。模组列表显示的 SPDX 标识是 `LGPL-3.0-or-later`。
