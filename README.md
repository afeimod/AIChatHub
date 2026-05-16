# AI Chat Hub

多平台AI对话应用，支持 DeepSeek、MiniMax、OpenAI GPT、Google Gemini 等主流AI平台。

## 功能特性

- 多平台API接入 (DeepSeek, MiniMax, OpenAI GPT, Google Gemini)
- 安全的API密钥加密存储
- 现代化的Material Design 3聊天界面
- 支持Markdown代码渲染
- 明/暗主题切换
- 对话历史管理
- GitHub Actions 自动化构建

## 技术栈

- **语言**: Kotlin 1.9.21
- **UI**: Jetpack Compose + Material Design 3
- **架构**: Clean Architecture + MVVM
- **依赖注入**: Hilt 2.50
- **网络**: Retrofit 2.9.0 + OkHttp 4.12.0
- **存储**: DataStore + Security Crypto

## 项目结构

```
AIChatHub/
├── app/src/main/java/com/aichathub/
│   ├── di/                    # 依赖注入模块
│   ├── data/                  # 数据层
│   │   ├── model/            # API数据模型
│   │   ├── remote/           # API服务
│   │   ├── local/            # 本地存储
│   │   └── repository/       # 仓库实现
│   ├── domain/               # 领域层
│   │   ├── model/           # 领域模型
│   │   ├── repository/      # 仓库接口
│   │   └── usecase/         # 用例
│   └── ui/                   # 表现层
│       ├── theme/           # Compose主题
│       ├── components/      # UI组件
│       ├── screens/         # 页面
│       └── viewmodel/       # ViewModel
├── .github/workflows/        # CI/CD工作流
├── build.gradle.kts         # 根构建配置
└── settings.gradle.kts      # 项目设置
```

## 构建说明

### 本地构建

```bash
# 克隆项目
git clone <repository-url>
cd AIChatHub

# 授予执行权限
chmod +x gradlew

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease
```

### GitHub Actions 自动构建

推送到 main 分支或创建 Pull Request 时自动触发：

1. **Debug Build**: 每次 push/PR 自动构建
2. **Test**: 运行单元测试
3. **Release Build**: 发布版本时构建

构建产物位置：`app/build/outputs/apk/`

## API配置

### DeepSeek
- Endpoint: `https://api.deepseek.com/v1/chat/completions`
- 模型: `deepseek-chat`, `deepseek-coder`

### MiniMax
- Endpoint: `https://api.minimax.chat/v1/text/chatcompletion_v2`
- 模型: `abab6.5s-chat`

### OpenAI
- Endpoint: `https://api.openai.com/v1/chat/completions`
- 模型: `gpt-4`, `gpt-3.5-turbo`

### Google Gemini
- Endpoint: `https://generativelanguage.googleapis.com/v1/models`
- 模型: `gemini-pro`

## License

MIT License