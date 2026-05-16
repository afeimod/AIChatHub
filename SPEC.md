# AI Chat Hub - 多平台AI对话应用规格说明

## 1. 项目概述

**项目名称**: AI Chat Hub  
**项目类型**: Android原生应用 (Kotlin + Jetpack Compose)  
**核心功能**: 统一的多平台AI对话客户端，支持DeepSeek、MiniMax、OpenAI GPT、Google Gemini等主流AI平台的API接入，提供安全的密钥管理和现代化的对话界面。  
**目标用户**: 开发者、AI爱好者、需要使用多个AI服务的专业人士

## 2. 技术栈选择

### 框架与语言
- **语言**: Kotlin 1.9.21
- **最低SDK**: API 26 (Android 8.0)
- **目标SDK**: API 34 (Android 14)
- **UI框架**: Jetpack Compose + Material Design 3
- **构建工具**: Gradle 8.4 with Kotlin DSL

### 关键库/依赖
| 库 | 版本 | 用途 |
|---|---|---|
| Jetpack Compose BOM | 2023.10.01 | UI框架 |
| Hilt | 2.50 | 依赖注入 |
| Retrofit | 2.9.0 | HTTP客户端 |
| OkHttp | 4.12.0 | 网络请求 |
| Kotlinx Serialization | 1.6.2 | JSON序列化 |
| DataStore Preferences | 1.0.0 | 本地存储 |
| Security Crypto | 1.1.0-alpha06 | API密钥加密 |
| Navigation Compose | 2.7.6 | 页面导航 |

### 架构模式
- **Clean Architecture + MVVM**
  - **Presentation Layer**: Compose UI + ViewModel + StateFlow
  - **Domain Layer**: Use Cases + Repository Interfaces
  - **Data Layer**: Repository Implementations + Remote/Local Data Sources

### CI/CD
- **GitHub Actions**: 自动化构建、测试和构建APK
- **触发条件**: push、pull_request、release tag

## 3. 功能列表

### 核心功能

#### 3.1 多平台API接入
| 平台 | 支持模型 | API格式 |
|---|---|---|
| DeepSeek | deepseek-chat, deepseek-coder | OpenAI兼容 |
| MiniMax | abab6.5s-chat, abab5.5s-chat | 定制格式 |
| OpenAI | gpt-4, gpt-4-turbo, gpt-3.5-turbo | OpenAI标准 |
| Google Gemini | gemini-pro, gemini-pro-vision | Google定制 |

#### 3.2 API密钥管理
- 安全加密存储各平台API密钥
- 密钥添加、编辑、删除功能
- 设置活跃密钥
- 连接测试功能

#### 3.3 对话界面
- 现代化的Material Design 3聊天UI
- 用户消息和AI响应区分显示
- 消息时间戳显示
- 对话历史记录
- 新建对话功能

#### 3.4 设置功能
- 明/暗主题切换
- 默认参数配置（temperature, max_tokens）
- 清空对话历史
- 应用信息

## 4. UI/UX 设计方向

### 视觉风格
- Material Design 3 (Material You)
- 支持动态颜色主题
- 卡片式布局

### 配色方案
| 元素 | 亮色模式 | 暗色模式 |
|---|---|---|
| 主色调 | #2196F3 | #2196F3 |
| 背景色 | #F5F5F5 | #1C1C1E |
| 表面色 | #FFFFFF | #2C2C2E |
| 用户消息 | #DCF8C6 | #DCF8C6 |
| AI消息 | #E8E8E8 | #3A3A3C |

### 布局结构
```
主界面 (ChatScreen)
├── 顶部导航栏 (TopAppBar)
│   ├── 平台名称
│   ├── 模型名称
│   ├── API密钥按钮
│   └── 设置按钮
├── 平台选择器 (PlatformSelector)
├── 模型选择器 (ModelSelector)
├── 消息列表 (LazyColumn)
│   ├── 用户消息 (UserMessage)
│   └── AI响应 (AssistantMessage)
└── 输入区域 (MessageInput)
    ├── 文本输入框
    └── 发送按钮

API密钥管理 (APIKeyScreen)
├── 密钥列表
├── 添加新密钥
└── 编辑/删除密钥

设置界面 (SettingsScreen)
├── 主题切换
├── 默认参数
└── 清空历史
```

## 5. API接入规范

### 5.1 DeepSeek API
- **Endpoint**: `https://api.deepseek.com/v1/chat/completions`
- **认证**: `Authorization: Bearer {API_KEY}`
- **请求格式**: OpenAI兼容

### 5.2 MiniMax API
- **Endpoint**: `https://api.minimax.chat/v1/text/chatcompletion_v2`
- **认证**: `Authorization: Bearer {API_KEY}`
- **请求格式**: 定制JSON格式

### 5.3 OpenAI API
- **Endpoint**: `https://api.openai.com/v1/chat/completions`
- **认证**: `Authorization: Bearer {API_KEY}`
- **请求格式**: OpenAI标准

### 5.4 Google Gemini API
- **Endpoint**: `https://generativelanguage.googleapis.com/v1/models/{model}:generateContent`
- **认证**: `?key={API_KEY}` Query参数

## 6. 项目结构

```
AIChatHub/
├── app/
│   ├── src/main/
│   │   ├── java/com/aichathub/
│   │   │   ├── AIChatHubApplication.kt          # Application类
│   │   │   ├── di/                              # 依赖注入
│   │   │   │   ├── NetworkModule.kt             # 网络模块
│   │   │   │   └── RepositoryModule.kt         # 仓库模块
│   │   │   ├── data/                           # 数据层
│   │   │   │   ├── model/
│   │   │   │   │   └── ApiModels.kt            # API数据模型
│   │   │   │   ├── remote/
│   │   │   │   │   └── AIServiceApi.kt         # API服务接口
│   │   │   │   ├── local/
│   │   │   │   │   └── SecureKeyStorage.kt    # 加密存储
│   │   │   │   └── repository/
│   │   │   │       ├── AIServiceRepositoryImpl.kt
│   │   │   │       └── RepositoryImpl.kt
│   │   │   ├── domain/                         # 领域层
│   │   │   │   ├── model/
│   │   │   │   │   └── Models.kt              # 领域模型
│   │   │   │   ├── repository/
│   │   │   │   │   └── Repositories.kt        # 仓库接口
│   │   │   │   └── usecase/
│   │   │   │       └── UseCases.kt            # 用例
│   │   │   └── ui/                             # 表现层
│   │   │       ├── theme/
│   │   │       │   └── Theme.kt               # Compose主题
│   │   │       ├── components/
│   │   │       │   ├── ChatBubble.kt          # 聊天气泡
│   │   │       │   └── InputComponents.kt    # 输入组件
│   │   │       ├── screens/
│   │   │       │   ├── ChatScreen.kt         # 聊天界面
│   │   │       │   ├── APIKeyScreen.kt       # API密钥管理
│   │   │       │   └── SettingsScreen.kt    # 设置界面
│   │   │       ├── viewmodel/
│   │   │       │   ├── ChatViewModel.kt
│   │   │       │   ├── APIKeyViewModel.kt
│   │   │       │   └── SettingsViewModel.kt
│   │   │       └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── proguard-rules.pro
├── .github/
│   └── workflows/
│       └── android.yml                        # CI/CD工作流
├── build.gradle.kts                           # 根构建配置
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## 7. GitHub Actions 工作流

### 构建触发条件
- push 到 main 分支
- pull_request 到 main 分支
- 创建 v*.*.* 版本标签

### 构建流程
1. 检出代码
2. 配置JDK 17
3. 配置Android SDK
4. 缓存依赖
5. 运行构建
6. 单元测试
7. 构建Debug APK
8. 上传构建产物

### 构建产物
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## 8. 安全考虑

1. **API密钥加密**: 使用AndroidX Security Crypto加密存储
2. **不记录日志**: 生产环境不输出敏感信息
3. **网络传输**: HTTPS强制使用
4. **最小权限**: 只申请INTERNET权限