# AI Chat Hub

一个支持多平台 AI 服务的统一 Android 聊天客户端，基于 Jetpack Compose + Material 3 + Hilt + Clean Architecture 构建。

## ✨ 核心特性

### 支持的 AI 平台（18+ 内置 + 自定义）
| 平台 | API 风格 | 备注 |
|------|---------|------|
| DeepSeek | OpenAI 兼容 | deepseek-chat / reasoner / coder |
| OpenAI (GPT) | OpenAI 原生 | gpt-4o / gpt-4-turbo / o1 |
| Google Gemini | Gemini | gemini-2.0-flash / 1.5-pro |
| Anthropic Claude | Anthropic | claude-3.5-sonnet / haiku / opus |
| 通义千问 (Qwen) | OpenAI 兼容 | qwen-max / qwen-plus / qwen-long |
| 智谱 GLM | OpenAI 兼容 | glm-4-plus / glm-4v |
| Moonshot (Kimi) | OpenAI 兼容 | moonshot-v1-8k/32k/128k |
| 零一万物 (Yi) | OpenAI 兼容 | yi-large / yi-vision |
| 百川 (Baichuan) | OpenAI 兼容 | Baichuan4 |
| 豆包 (Doubao) | OpenAI 兼容 | doubao-pro / lite |
| 腾讯混元 | OpenAI 兼容 | hunyuan-pro / standard |
| 讯飞星火 | OpenAI 兼容 | generalv3.5 / spark-v4 |
| SiliconFlow | OpenAI 兼容 | Qwen / DeepSeek / Llama |
| Groq | OpenAI 兼容 | llama-3.3-70b (超快推理) |
| Together AI | OpenAI 兼容 | Llama / Mixtral / Qwen |
| OpenRouter | OpenAI 兼容 | 聚合 100+ 模型 |
| MiniMax | OpenAI 兼容 + VLM | MiniMax-M2.x / abab |
| **自定义平台** | OpenAI/Anthropic/Gemini | 用户自添加任意端点 |

### 自定义 API 模型
- 每个 API Key 可设置**自定义模型名**（覆盖默认）
- 可添加**额外可选模型列表**
- 下拉选择中提供"✎ 自定义模型名…"入口，输入任意模型 ID

### 工作目录
- **默认使用手机 Download 目录**（无需任何权限）
- 支持**自定义工作目录**（通过 SAF 选择，持久化权限）
- 浏览工作目录中的文件
- 将 AI 回复保存为 .txt/.md 文件
- 删除工作目录中的文件

### 终端输出界面
- 实时显示 API 请求 / 响应 / 流式 chunk / 错误
- 7 种日志级别：INFO / REQUEST / RESPONSE / STREAM / ERROR / WARN / DEBUG
- 终端风格黑色背景 + 等宽字体 + 颜色编码
- 支持暂停 / 自动滚动 / 过滤 / 复制全部 / 清空
- 持久化最近 500 条日志

### 上下文管理（无限制 + 智能策略）
- **无限制**：发送全部历史消息（受模型上下文窗口限制）
- **滑动窗口**：保留最近 N 条消息
- **智能摘要**：保留首条 + 最近 N-1 条
- **仅系统**：只发送系统提示 + 当前消息
- 实时 Token 估算（输入 + 会话总量）

### 其他完整功能
- **Markdown 渲染**：标题 / 粗体 / 斜体 / 代码块 / 列表 / 引用 / 链接 / 分割线
- **流式响应 (SSE)**：逐 token 实时显示，支持停止生成
- **多模态输入**：图片 / PDF / 文档 / 压缩包 / 音频 / 视频
- **系统提示词**：每个会话独立的 system prompt
- **消息操作**：复制 / 重新生成 / 删除
- **多会话管理**：会话历史 / 新建 / 切换 / 删除
- **API Key 管理**：加密存储 / 添加 / 编辑 / 删除 / 测试连接 / 设为活跃
- **设置**：暗色模式 / 字体大小 / Markdown / Temperature / Max Tokens / 流式开关 / 多模态开关 / 自动标题
- **安全**：API Key 使用 AndroidX Security Crypto (AES256-GCM/SIV) 加密存储
- **Token 计数器**：实时显示输入与会话 token 估算

## 🛠 技术栈

- **UI**: Jetpack Compose + Material 3 (BOM 2023.10.01)
- **DI**: Hilt 2.50
- **网络**: Retrofit 2.9.0 + OkHttp 4.12.0 + kotlinx-serialization
- **存储**: DataStore Preferences + EncryptedSharedPreferences
- **图片**: Coil 2.5.0
- **文件**: DocumentFile (SAF) + java.io.File
- **架构**: Clean Architecture + MVVM

## 📦 构建

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

输出 APK 位于 `app/build/outputs/apk/`。

## 📂 项目结构

```
app/src/main/java/com/aichathub/
├── AIChatHubApplication.kt       # Hilt 入口
├── data/
│   ├── local/                     # 本地存储
│   │   ├── SecureKeyStorage.kt    # 加密存储
│   │   └── TerminalLogManager.kt  # 终端日志管理器
│   ├── model/                     # API DTO
│   │   └── ApiModels.kt
│   ├── remote/                    # Retrofit API
│   │   └── AIServiceApi.kt
│   └── repository/                # 仓库实现
│       ├── AIServiceRepositoryImpl.kt   # AI 服务（18+ 平台 + 流式）
│       └── RepositoryImpl.kt            # 其他仓库实现
├── di/
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── domain/
│   ├── model/Models.kt            # 领域模型
│   ├── repository/Repositories.kt # 仓库接口
│   ├── usecase/UseCases.kt        # 用例
│   └── util/ContextManager.kt     # 上下文管理 + Token 估算
└── ui/
    ├── MainActivity.kt
    ├── theme/Theme.kt
    ├── components/
    │   ├── ChatBubble.kt          # 聊天气泡 + Markdown
    │   ├── InputComponents.kt     # 输入组件
    │   └── MarkdownRenderer.kt    # 轻量 Markdown 渲染器
    ├── screens/
    │   ├── ChatScreen.kt
    │   ├── APIKeyScreen.kt
    │   ├── SettingsScreen.kt
    │   ├── TerminalScreen.kt      # 终端输出
    │   ├── WorkspaceScreen.kt     # 工作目录
    │   └── CustomProviderScreen.kt # 自定义平台
    └── viewmodel/
        ├── ChatViewModel.kt
        ├── APIKeyViewModel.kt
        ├── SettingsViewModel.kt
        ├── TerminalViewModel.kt
        ├── WorkspaceViewModel.kt
        └── CustomProviderViewModel.kt
```

## 📝 版本

- **v2.0.0** — 大规模功能补全：18+ AI 平台、自定义平台、自定义模型名、工作目录、终端输出、无限制上下文、Markdown 渲染、流式响应、系统提示词、消息操作
- **v1.0.0** — 初始版本：4 平台 + 基础聊天

## 📄 License

MIT
