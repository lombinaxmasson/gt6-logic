# GT6 Logic

GT6CE 的 NeoForge 1.21.1 附属：一批友好的控制元件（恒温器、浮球开关、定时器、调节阀、指示灯……），以及一批需要它们的工艺机器。

目标是让 GT6 的自动化变得更有意思，同时不变得更难：最常见的需求用一个方块、设两个数就能解决，信号线和逻辑组合是可选的进阶玩法。

> 指示灯已经能放下来。信号双缓冲、回差判定和厨房定时器的节拍已经合入。逻辑元件的分母在 [`docs/components.md`](docs/components.md)。

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
- CI 会按 `build-commit.txt` 钉住的 GT6CE 提交自己构建 mod jar 和 SDK，所以这个提交必须已经合并进 SaltNya/GregTech6，并且能产出 NeoForge SDK。
- 常用命令：`gradlew build`、`gradlew runClient`、`gradlew runGameTestServer`。

## 许可

GT6 Logic 使用 GNU Lesser General Public License 第 3 版或（由你选择）任何更新版本。`LICENSE` 是 LGPL-3.0 原文，`COPYING` 是它引用的 GPL-3.0 原文。两份原文也随 jar 发布在 `META-INF/licenses/` 下。模组列表显示的 SPDX 标识是 `LGPL-3.0-or-later`。
