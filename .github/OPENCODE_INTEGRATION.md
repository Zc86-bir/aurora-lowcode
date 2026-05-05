# OpenCode GitHub 集成配置指南

## 概述

本项目已配置 [OpenCode](https://github.com/anomalyco/opencode) 官方 GitHub App 集成。
通过在 Issue 或 PR 评论中输入 `/oc` 或 `/opencode` 命令，即可触发 AI Agent 自动处理请求。

## 安装步骤

### 1. 安装 OpenCode GitHub App

访问 [github.com/apps/opencode-agent](https://github.com/apps/opencode-agent)，点击 **Install**，选择目标仓库进行安装。

### 2. 配置 API 密钥

进入仓库 **Settings → Secrets and variables → Actions → New repository secret**，添加：

| Secret 名称 | 值 | 说明 |
|-------------|-----|------|
| `OPENAI_API_KEY` | `sk-...` | OpenAI 兼容 API 密钥（用于 kimi-k2.6） |

> **安全提示**：切勿将 API 密钥提交到代码仓库中，务必使用 GitHub Secrets 管理。

### 3. 使用方法

在任意 **Issue** 或 **Pull Request 的 Review Comment** 中评论：

```
/oc 请帮我分析这个 PR 的代码变更
```

或：

```
/opencode 请修复这个 bug 并提交更改
```

## 工作流配置说明

工作流文件位于 `.github/workflows/opencode.yml`：

| 配置项 | 当前值 | 说明 |
|--------|--------|------|
| `model` | `opencode-go/kimi-k2.6` | 使用的 AI 模型（必填） |
| `agent` | 默认 `build` | 指定使用的主代理名称 |
| `share` | 公开仓库默认 `true` | 是否共享 OpenCode 会话 |
| `github_token` | 默认使用 App Token | 用于创建评论、提交变更和 PR |

### 自定义模型

如需更换模型，修改 `with.model`：

```yaml
with:
  model: opencode-go/kimi-k2.6    # 当前配置（Kimi K2.6）
  # model: anthropic/claude-sonnet-4-20250514  # Anthropic 模型
  # model: openai/gpt-4o                         # OpenAI 模型
```

### 使用 GITHUB_TOKEN 替代 App Token

如果不安装 OpenCode GitHub App，可以使用内置的 `GITHUB_TOKEN`：

```yaml
permissions:
  id-token: write
  contents: write
  pull-requests: write
  issues: write

# ...

with:
  github_token: ${{ secrets.GITHUB_TOKEN }}
```

> 注意：使用 `GITHUB_TOKEN` 时，评论和提交会显示为 "GitHub Actions" 而非 "OpenCode App"。

## 权限说明

工作流已配置以下权限：

| 权限 | 用途 |
|------|------|
| `id-token: write` | OpenID Connect 身份验证 |
| `contents: write` | 读取和修改仓库内容 |
| `pull-requests: write` | 创建和更新 PR、评论 |
| `issues: write` | 创建和更新 Issue 评论 |

## 触发条件

工作流在以下情况触发：

| 事件 | 触发条件 |
|------|----------|
| `issue_comment.created` | 评论包含 `/oc` 或 `/opencode` |
| `pull_request_review_comment.created` | Review 评论包含 `/oc` 或 `/opencode` |

## 示例用法

### 代码审查

在 PR Review Comment 中：

```
/opencode 请审查这段代码，检查是否存在潜在的性能问题
```

### 自动修复

在 Issue 中：

```
/oc 请根据 Issue 描述修复问题，并创建一个 Pull Request
```

### 询问问题

在 PR 评论中：

```
/oc 这个变更对现有用户有什么影响？
```

## 故障排除

| 问题 | 解决方案 |
|------|----------|
| 评论后无响应 | 检查 OpenCode GitHub App 是否已安装到该仓库 |
| "API Key not found" | 确认 `OPENAI_API_KEY` 已添加到仓库 Secrets |
| 权限不足错误 | 检查工作流中的 `permissions` 配置是否完整 |
| 工作流未触发 | 确认评论中包含 `/oc` 或 `/opencode` 关键字 |
| 模型不可用 | 检查 `model` 参数格式是否为 `provider/model-name` |

## 相关文件

- `.github/workflows/opencode.yml` — OpenCode 工作流配置
- `https://github.com/apps/opencode-agent` — OpenCode GitHub App 安装页面
