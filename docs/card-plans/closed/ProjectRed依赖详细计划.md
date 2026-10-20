# ProjectRed 依赖详细计划

> 计划 slug：`rewrite/projectred-dependency`
> 状态：已关闭。本文件位于 `card-plans/closed/`。前置是 [`第一条电路`](第一条电路详细计划.md)。
> 性质：把“开发环境里放了五个 jar”变成“成品声明依赖，CI 也能跑”。

```text
lane            = closed
slug            = rewrite/projectred-dependency
owned_paths     = src/main/templates/META-INF/neoforge.mods.toml
                  .github/workflows/build.yml
                  fetch-projectred.ps1
depends_on      = rewrite/first-circuit
close_target    = mods.toml 声明 ProjectRed 为必需依赖，CI 先取 jar 再构建
```

## 0. 它解决什么

现在五个 jar 只在开发运行时出现，由 `fetch-projectred.ps1` 下载，目录为空时跨模组测试直接跳过。CI 没有这一步，所以 CI 从来没跑过这些测试。成品的 `neoforge.mods.toml` 也没写依赖，玩家可以在没装 ProjectRed 的情况下装上本模组，然后发现一半方块没有对应的电路可用。

## 1. 行为

- `neoforge.mods.toml` 里把 `projectred_core`、`projectred_integration`、`projectred_transmission` 写成必需依赖，版本范围 `[4.23,)`。CB Multipart 和 CodeChicken Lib 由 ProjectRed 自己声明，不重复写。
- CI 在构建前运行 `fetch-projectred.ps1` 的等价步骤：按脚本里的 Modrinth 版本 id 下载，校验 SHA1，放到 `libs/projectred/`。下载之后再 `gradlew build`，再 `gradlew runGameTestServer`。
- 描述文字改掉“timers, valves, and the machines that need them”，改成接 ProjectRed 和 GT6CE 的说法。

跨模组测试仍是 `required = false`。jar 在运行时存在时，这 40 个 GameTest 里包含 `ProjectRedInteropTests`，它们会实际跑完。jar 缺失时游戏在加载阶段拒绝启动，报错写明 `projectred_core`、`projectred_integration`、`projectred_transmission` 未安装，要求 4.23 或更高。`gradlew build` 不启动游戏，所以 jar 缺失时构建仍然通过。

## 2. 明确不做

- 不把 ProjectRed 的 jar 打进成品
- 不加编译依赖。捆绑线缆接口才需要 ProjectRed 的接口，那是下一张卡
- 不把跨模组测试改成必需。jar 缺失时跳过的行为保持不变，避免本地没跑脚本时构建失败

## 3. 验收

- [x] 只装本模组和 GT6CE、不装 ProjectRed 时，游戏拒绝加载并指出缺 ProjectRed
- [x] CI 在取到 jar 之后跑 `runGameTestServer`。本地同样的任务在 jar 存在时 40 个 GameTest 全部通过，其中包含 `ProjectRedInteropTests`
- [x] `gradlew build` 在 jar 存在和 jar 缺失两种情况下都通过
