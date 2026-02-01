# HTML工具生成器 - 项目架构文档

## 1. 项目概述

HTML工具生成器是一个基于Java的智能工具生成系统，能够根据用户自然语言需求自动生成完整可用的HTML工具页面。项目采用现代化的架构设计，支持多种运行模式和容灾机制。

### 核心特性

- **双模式运行**：支持命令行交互模式和Web服务模式
- **AI智能生成**：优先使用豆包大模型（Doubao）进行智能生成
- **多接入点容灾**：支持配置多个AI接入点，自动故障切换
- **模板回退机制**：AI生成失败时提供基础模板支持
- **完整诊断工具**：内置AI连接诊断功能，快速定位问题
- **中文友好**：完整支持中文文件名、中文内容处理

### 运行模式

1. **命令行模式**：交互式CLI界面，适合开发调试
2. **Web模式（Spring Boot）**：基于Spring Boot的RESTful API服务
3. **Web模式（原生HTTP）**：使用Java原生HttpServer的轻量级服务

---

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    应用入口层                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Main.java   │  │ WebApplication│  │  WebServer  │      │
│  │ (命令行模式) │  │ (Spring Boot) │  │ (原生HTTP)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    控制器层                                   │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │IndexController│ │ WebController │                         │
│  │ (前端页面)   │  │ (RESTful API) │                         │
│  └──────────────┘  └──────────────┘                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    服务层（核心业务逻辑）                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          HtmlGenerator (HTML生成器核心)              │   │
│  │  - 需求分析                                          │   │
│  │  - 文件缓存检查                                      │   │
│  │  - 简单模板匹配                                      │   │
│  │  - AI生成调度                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│                            ↓                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │      AIFailoverManager (AI容灾管理器)                │   │
│  │  - 多客户端管理                                       │   │
│  │  - 自动故障切换                                       │   │
│  │  - 错误分析                                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                            ↓                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          AIClient (AI客户端抽象层)                    │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │  DoubaoClient (豆包API客户端实现)              │  │   │
│  │  │  - 火山引擎ARK SDK集成                         │  │   │
│  │  │  - 系统提示词管理                              │  │   │
│  │  │  - HTML内容提取                                │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Templates (模板管理器)                       │   │
│  │  - 模板文件加载                                       │   │
│  │  - 模板格式化                                         │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    AIConnectionDiagnostic (连接诊断工具)             │   │
│  │  - 配置检查                                           │   │
│  │  - 网络连接测试                                       │   │
│  │  - API端点可达性测试                                 │   │
│  │  - 认证测试                                          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    配置层                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Config     │  │  WebConfig   │  │ ConfigSetup  │      │
│  │ (配置管理)   │  │ (Spring配置) │  │ (配置向导)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 核心模块详细说明

### 3.1 应用入口层

#### `Main.java` - 命令行模式入口
- **包路径**: `aitool.Main`
- **功能**: 提供交互式命令行工具生成器
- **主要职责**:
  1. 解析命令行参数（支持 `--web` 或 `-w` 启动Web模式）
  2. 初始化 `HtmlGenerator` 单例
  3. 循环接收用户输入并处理
  4. 处理特殊命令（config、diagnose、quit等）
  5. 保存生成的HTML文件到 `output/` 目录

- **代码块作用**:
  - `main(String[] args)`: 程序入口，判断运行模式
  - 命令行参数解析：支持 `--web [端口]` 启动Web服务器
  - 交互循环：持续接收用户输入直到退出
  - 特殊命令处理：
    - `config` / `配置` → 调用 `ConfigSetup.setupConfig()` 配置AI密钥
    - `diagnose` / `诊断` → 调用 `AIConnectionDiagnostic.diagnose()` 诊断连接
    - `quit` / `exit` / `退出` → 退出程序
  - 文件保存：使用UTF-8编码确保中文文件名和内容正确

#### `WebApplication.java` - Spring Boot Web应用入口
- **包路径**: `aitool.WebApplication`
- **功能**: Spring Boot应用主类，启动Web服务器
- **代码块作用**:
  - `@SpringBootApplication`: Spring Boot自动配置注解
  - `main()`: 启动Spring Boot应用，默认端口8080
  - 启动信息输出：显示服务器地址和停止提示

#### `WebServer.java` - 原生HTTP服务器（备用方案）
- **包路径**: `aitool.WebServer`
- **功能**: 使用Java原生 `com.sun.net.httpserver.HttpServer` 提供Web服务
- **代码块作用**:
  - `main(String[] args)`: 创建HTTP服务器，支持自定义端口
  - `StaticFileHandler`: 处理静态文件请求（前端页面）
  - `GenerateHandler`: 处理 `/api/generate` POST请求，生成工具
  - `DownloadHandler`: 处理 `/api/download` GET请求，下载文件
  - `FileListHandler`: 处理 `/api/files` GET请求，获取文件列表
  - 中文文件名支持：使用RFC 5987编码处理中文文件名下载

---

### 3.2 控制器层（Spring Boot）

#### `IndexController.java` - 首页控制器
- **包路径**: `aitool.controller.IndexController`
- **功能**: 提供前端HTML页面服务
- **代码块作用**:
  - `@GetMapping("/")` 和 `@GetMapping("/index.html")`: 映射根路径和index.html
  - `index()`: 从classpath加载 `resources/web/index.html` 并返回
  - 错误处理：如果文件加载失败，返回错误页面

#### `WebController.java` - Web API控制器
- **包路径**: `aitool.controller.WebController`
- **功能**: 提供RESTful API接口
- **代码块作用**:
  - `@Autowired HtmlGenerator`: 注入HTML生成器服务
  - `@Autowired WebConfig`: 注入Web配置服务
  - `generate()`: 
    - 接收POST请求，解析JSON中的用户需求
    - 调用 `HtmlGenerator.generateTool()` 生成HTML
    - 使用 `FilenameGenerator.generateFilename()` 生成文件名
    - 保存文件到输出目录
    - 返回JSON响应（包含success、filename、filepath、htmlContent）
  - `download()`: 
    - 接收GET请求，参数为文件名
    - 从输出目录读取文件
    - 设置Content-Disposition响应头（支持中文文件名）
    - 返回文件内容
  - `getFiles()`: 
    - 列出输出目录中所有.html文件
    - 返回文件名数组JSON

---

### 3.3 服务层（核心业务逻辑）

#### `HtmlGenerator.java` - HTML生成器核心类
- **包路径**: `aitool.service.HtmlGenerator`
- **功能**: 根据用户需求生成HTML工具页面的核心类
- **设计模式**: 单例模式（`getInstance()` / `reloadInstance()`）

- **代码块作用**:
  - **构造函数**:
    - 初始化工具生成器映射（table、calculator、text_replace等）
    - 创建输出目录
    - 检查AI配置，初始化AI客户端或容灾管理器
    - 输出初始化状态信息
  
  - **`generateTool(String userRequest)`** - 核心生成方法:
    1. **文件缓存检查** (`findExistingFile()`):
       - 提取用户需求中的关键词
       - 在output目录中查找匹配的已生成文件
       - 如果找到匹配文件，直接返回（避免重复生成）
    
    2. **简单模板匹配** (`getSimpleExampleTemplate()`):
       - 检查是否是简单示例需求（如"生成一个计算器工具"）
       - 如果匹配，直接返回对应的模板HTML（快速响应）
    
    3. **AI生成**:
       - 优先使用 `AIFailoverManager`（如果已配置）
       - 否则使用单个 `AIClient`（向后兼容）
       - 验证生成的HTML格式（必须包含<!DOCTYPE或<html>标签）
       - 如果AI生成失败，抛出异常（不再回退到模板）
  
  - **`findExistingFile(String userRequest)`** - 文件缓存查找:
    - 提取需求关键词（中文词和英文词）
    - 遍历output目录中的HTML文件
    - 计算文件名与关键词的匹配度
    - 返回最佳匹配的文件内容
  
  - **`getSimpleExampleTemplate(String userRequest)`** - 简单模板匹配:
    - 定义简单示例需求映射（计算器、表格生成器等）
    - 精确匹配和部分匹配
    - 限制需求长度，避免误判复杂需求
  
  - **工具生成器类**（内部类）:
    - `ToolGenerator`: 抽象基类
    - `TableGeneratorTool`: 表格生成工具
    - `CalculatorTool`: 计算器工具
    - `TextReplaceTool`: 文本替换工具
    - `DataConverterTool`: 数据转换工具
    - `JsonFormatterTool`: JSON格式化工具
    - `CsvProcessorTool`: CSV处理工具

#### `AIClient.java` - AI客户端抽象类
- **包路径**: `aitool.service.AIClient`
- **功能**: 定义AI生成接口，提供通用方法
- **代码块作用**:
  - **抽象方法**:
    - `generateHtmlTool()`: 生成HTML工具（子类实现）
    - `shutdown()`: 关闭客户端资源
    - `getClientName()`: 获取客户端名称
  
  - **`getDefaultSystemPrompt()`** - 默认系统提示词:
    - 定义AI生成HTML工具的系统提示词
    - 要求生成完整、美观、可用的HTML页面
  
  - **`extractHtml(String content)`** - HTML提取:
    - 移除markdown代码块标记（```html 或 ```）
    - 确保以<!DOCTYPE或<html>开头
    - 清理多余的说明文字

- **`DoubaoClient`** - 豆包API客户端实现:
  - **构造函数**:
    - 接收API密钥、端点ID、基础URL
    - 创建 `ArkService` 实例（火山引擎ARK SDK）
  
  - **`generateHtmlTool()`**:
    - 使用默认或自定义系统提示词
    - 调用 `generateWithArkSdk()` 生成内容
  
  - **`generateWithArkSdk()`** - 核心生成逻辑:
    - 构建系统消息和用户消息
    - 使用ARK SDK创建请求
    - 发送请求并解析响应
    - 返回生成的HTML内容
  
  - **`setClientName()` / `getClientName()`**: 设置和获取客户端名称（用于区分不同接入点）

#### `AIFailoverManager.java` - AI容灾管理器
- **包路径**: `aitool.service.AIFailoverManager`
- **功能**: 管理多个AI客户端，实现自动故障切换
- **代码块作用**:
  - **构造函数**:
    - 接收AI客户端列表（按优先级排序）
    - 初始化当前索引（使用AtomicInteger保证线程安全）
    - 构建客户端名称列表（用于日志）
  
  - **`generateHtmlTool()`** - 容灾生成方法:
    1. 从当前索引开始尝试
    2. 循环尝试所有客户端（如果当前失败，切换到下一个）
    3. 验证生成结果（长度>100，包含HTML标签）
    4. 如果成功，更新当前索引（下次优先使用成功的客户端）
    5. 如果所有客户端都失败，抛出包含所有错误信息的异常
  
  - **`analyzeFailureReason()`** - 失败原因分析:
    - 分析异常类型和错误消息
    - 识别连接失败、认证失败、配额不足、服务不可用等错误
    - 返回友好的错误描述
  
  - **`getClientNames()`**: 返回所有客户端名称（用于日志和错误报告）

#### `AIClientFactory.java` - AI客户端工厂
- **包路径**: `aitool.service.AIClientFactory`
- **功能**: 创建AI客户端实例，支持多接入点配置
- **代码块作用**:
  - **`createAIClient()`** - 创建单个客户端（已废弃，向后兼容）:
    - 从Config读取 `DOUBAO_API_KEY` 和 `DOUBAO_ENDPOINT_ID`
    - 创建单个DoubaoClient实例
  
  - **`createFailoverManager()`** - 创建容灾管理器（推荐）:
    1. 尝试创建第一个豆包客户端（主接入点）:
       - 读取 `DOUBAO_API_KEY`、`DOUBAO_ENDPOINT_ID`、`DOUBAO_BASE_URL`
       - 如果配置存在，创建客户端并命名为"豆包(Doubao-主)"
    
    2. 尝试创建第二个豆包客户端（备用接入点1）:
       - 读取 `DOUBAO_API_KEY_2`、`DOUBAO_ENDPOINT_ID_2`、`DOUBAO_BASE_URL_2`
       - 如果配置存在，创建客户端并命名为"豆包(Doubao-备用)"
    
    3. 尝试创建第三个豆包客户端（备用接入点2）:
       - 读取 `DOUBAO_API_KEY_3`、`DOUBAO_ENDPOINT_ID_3`、`DOUBAO_BASE_URL_3`
       - 如果配置存在，创建客户端并命名为"豆包(Doubao-备用2)"
    
    4. 将所有成功创建的客户端添加到列表
    5. 如果至少有一个客户端，创建并返回 `AIFailoverManager`
    6. 如果没有可用客户端，返回null
  
  - **`hasAvailableConfig()`**: 检查是否有可用的AI配置

#### `AIConnectionDiagnostic.java` - AI连接诊断工具
- **包路径**: `aitool.service.AIConnectionDiagnostic`
- **功能**: 诊断AI连接问题，帮助用户快速定位配置或网络问题
- **代码块作用**:
  - **`diagnose()`** - 完整诊断流程:
    1. **配置检查** (`checkConfig()`):
       - 检查API密钥、端点ID、基础URL是否配置
       - 验证配置完整性
    
    2. **网络连接测试** (`testNetworkConnection()`):
       - 解析API基础URL获取主机名
       - 测试DNS解析
       - 验证网络可达性
    
    3. **API端点可达性测试** (`testEndpointReachability()`):
       - 发送GET请求到API端点（不带认证）
       - 如果能收到响应（即使是401/403），说明端点可达
    
    4. **API认证测试** (`testAuthentication()`):
       - 构建最小测试请求
       - 发送POST请求到API端点（带认证）
       - 根据HTTP状态码判断认证是否成功
       - 解析错误响应提供详细错误信息
  
  - **`printResult()`**: 打印单个检查结果（成功/失败）
  - **`printSummary()`**: 打印诊断总结和建议解决方案

#### `Templates.java` - 模板管理器
- **包路径**: `aitool.service.Templates`
- **功能**: 加载和管理HTML模板文件
- **代码块作用**:
  - **静态初始化块**:
    - 定义模板名称列表
    - 调用 `loadTemplates()` 加载所有模板
  
  - **`loadTemplates()`**:
    - 遍历模板名称列表
    - 从 `resources/web/templates/` 目录加载模板文件
    - 如果某个模板不存在，使用 `custom_tool` 作为默认模板
  
  - **`loadTemplateFromResource()`**:
    - 从classpath资源文件读取模板内容
    - 使用UTF-8编码确保中文正确
    - 处理文件不存在的情况
  
  - **`getTemplate()`**: 获取指定模板，如果不存在返回默认模板
  - **`formatTemplate()`**: 格式化模板（替换占位符如{title}、{description}等）
  - **`reloadTemplates()`**: 重新加载所有模板（用于模板更新后）

#### `FilenameGenerator.java` - 文件名生成器
- **包路径**: `aitool.service.FilenameGenerator`
- **功能**: 根据用户需求生成唯一的文件名
- **代码块作用**:
  - **`generateFilename()`**:
    - 使用正则表达式提取中文词和英文词
    - 取前3个关键词作为文件名基础
    - 限制文件名长度30字符
    - 添加时间戳（格式：yyyyMMdd_HHmmss）确保唯一性
    - 返回格式：`关键词1_关键词2_关键词3_时间戳.html`

---

### 3.4 配置层

#### `Config.java` - 配置管理类
- **包路径**: `aitool.config.Config`
- **功能**: 管理 `.env` 配置文件、系统环境变量和默认配置
- **代码块作用**:
  - **静态变量**:
    - `CONFIG_FILE`: `.env` 文件路径（项目根目录）
    - `dotenv`: Dotenv实例（懒加载）
    - `DEFAULTS`: 默认配置映射
  
  - **默认配置项**:
    - 豆包主接入点：`DOUBAO_API_KEY`、`DOUBAO_ENDPOINT_ID`、`DOUBAO_BASE_URL`
    - 豆包备用接入点1：`DOUBAO_API_KEY_2`、`DOUBAO_ENDPOINT_ID_2`、`DOUBAO_BASE_URL_2`
    - 豆包备用接入点2：`DOUBAO_API_KEY_3`、`DOUBAO_ENDPOINT_ID_3`、`DOUBAO_BASE_URL_3`
    - 火山引擎官方配置（兼容）：`VOLC_ACCESSKEY`、`VOLC_SECRETKEY`
    - 功能开关：`USE_AI`、`AI_FALLBACK_TO_TEMPLATE`
    - 超时配置：`AI_CONNECT_TIMEOUT`、`AI_READ_TIMEOUT`、`AI_WRITE_TIMEOUT`
  
  - **`loadConfig()`** - 懒加载配置:
    - 如果 `.env` 文件存在，从文件加载
    - 如果文件不存在，仅使用系统环境变量
  
  - **`get()`** - 获取配置值（优先级）:
    1. `.env` 文件中的值
    2. 系统环境变量
    3. 默认配置值
    4. 提供的默认值参数
  
  - **`getBool()`**: 获取布尔配置值（支持true/1/yes/on）
  - **`saveConfig()`**: 保存配置到 `.env` 文件
  - **`checkAiConfig()`**: 检查是否有至少一个有效的AI配置
  - **`createExampleConfig()`**: 创建示例配置文件模板

#### `WebConfig.java` - Web配置类
- **包路径**: `aitool.config.WebConfig`
- **功能**: Spring Boot配置类，管理输出目录和Bean
- **代码块作用**:
  - **`@Configuration`**: 标识为Spring配置类
  - **`@Value("${app.output.dir:output}")`**: 从配置文件读取输出目录路径（默认"output"）
  - **`@PostConstruct init()`**: 初始化时确保输出目录存在
  - **`@Bean htmlGenerator()`**: 提供 `HtmlGenerator` 单例Bean
  - **`getOutputDir()`**: 获取输出目录File对象

#### `ConfigSetup.java` - 配置设置工具
- **包路径**: `aitool.config.ConfigSetup`
- **功能**: 交互式配置AI密钥
- **代码块作用**:
  - **`setupConfig()`** - 配置向导:
    1. 显示配置方式选择菜单（代理配置/官方配置/取消）
    2. 根据选择收集配置信息
    3. 调用 `Config.saveConfig()` 保存配置
    4. 验证配置是否有效
  
  - **`checkAndPrompt()`**: 检查配置是否存在，如果不存在提示用户配置

---

### 3.5 数据模型层

#### `CheckResult.java` - 检查结果模型
- **包路径**: `aitool.model.CheckResult`
- **功能**: 存储单个检查项的结果
- **字段**:
  - `success`: 是否成功
  - `message`: 结果消息

#### `DiagnosticResult.java` - 诊断结果模型
- **包路径**: `aitool.model.DiagnosticResult`
- **功能**: 存储完整诊断结果
- **字段**:
  - `configCheck`: 配置检查结果
  - `networkCheck`: 网络连接检查结果
  - `endpointCheck`: API端点检查结果
  - `authCheck`: 认证检查结果
  - `overallStatus`: 总体状态
  - `summary`: 总结说明

---

## 4. 数据流向

### 4.1 命令行模式完整流程

```
用户输入需求
    ↓
Main.java (解析输入)
    ↓
HtmlGenerator.generateTool()
    ↓
┌─────────────────────────────────────┐
│ 1. 文件缓存检查                      │
│    findExistingFile()               │
│    - 提取关键词                      │
│    - 查找匹配文件                    │
│    - 如果找到，直接返回              │
└─────────────────────────────────────┘
    ↓ (未找到)
┌─────────────────────────────────────┐
│ 2. 简单模板匹配                      │
│    getSimpleExampleTemplate()       │
│    - 检查是否是简单需求              │
│    - 如果匹配，返回模板              │
└─────────────────────────────────────┘
    ↓ (不匹配)
┌─────────────────────────────────────┐
│ 3. AI生成                            │
│    - 检查AI是否启用                  │
│    - 使用AIFailoverManager或AIClient│
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ AIFailoverManager.generateHtmlTool()│
│    - 尝试客户端1                    │
│    - 如果失败，尝试客户端2          │
│    - 如果失败，尝试客户端3          │
│    - 验证生成结果                   │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ DoubaoClient.generateHtmlTool()     │
│    - 构建系统提示词                 │
│    - 调用ARK SDK                    │
│    - 提取HTML内容                   │
└─────────────────────────────────────┘
    ↓
FilenameGenerator.generateFilename()
    ↓
保存文件到 output/ 目录
    ↓
提示用户文件位置
```

### 4.2 Web模式完整流程（Spring Boot）

```
HTTP POST /api/generate
    ↓
WebController.generate()
    ↓
解析JSON请求体 → 提取用户需求
    ↓
HtmlGenerator.generateTool()
    ↓
[同命令行模式的生成流程]
    ↓
FilenameGenerator.generateFilename()
    ↓
保存文件到输出目录
    ↓
构建JSON响应
    ↓
返回HTTP响应（包含success、filename、filepath、htmlContent）
```

### 4.3 AI容灾机制流程

```
用户请求生成
    ↓
AIFailoverManager.generateHtmlTool()
    ↓
┌─────────────────────────────────────┐
│ 尝试客户端1（主接入点）              │
│    - 调用generateHtmlTool()         │
│    - 验证结果                       │
└─────────────────────────────────────┘
    ↓ (失败)
┌─────────────────────────────────────┐
│ 分析失败原因                         │
│    - 连接失败？                     │
│    - 认证失败？                     │
│    - 配额不足？                     │
└─────────────────────────────────────┘
    ↓
┌─────────────────────────────────────┐
│ 尝试客户端2（备用接入点1）          │
│    - 调用generateHtmlTool()         │
│    - 验证结果                       │
└─────────────────────────────────────┘
    ↓ (失败)
┌─────────────────────────────────────┐
│ 尝试客户端3（备用接入点2）          │
│    - 调用generateHtmlTool()         │
│    - 验证结果                       │
└─────────────────────────────────────┘
    ↓ (全部失败)
抛出异常（包含所有错误信息）
```

---

## 5. 文件输入输出总结表

| 文件 | 输入 | 输出 | 主要职责 |
|------|------|------|---------|
| `Main.java` | 用户命令行输入、命令行参数 | HTML文件到 `output/`、控制台输出 | 命令行模式入口，交互式处理 |
| `WebApplication.java` | Spring Boot启动参数 | HTTP服务器（端口8080） | Spring Boot应用启动 |
| `WebServer.java` | HTTP请求、端口号 | HTTP响应（HTML/JSON） | 原生HTTP服务器实现 |
| `IndexController.java` | `GET /` 或 `GET /index.html` | HTML页面内容 | 前端页面服务 |
| `WebController.java` | POST/GET请求、JSON请求体 | JSON响应或文件下载 | RESTful API接口 |
| `HtmlGenerator.java` | 用户需求字符串 | HTML代码字符串 | 核心生成逻辑，协调各组件 |
| `AIFailoverManager.java` | 用户需求、系统提示词 | HTML代码字符串 | AI容灾管理，故障切换 |
| `AIClient.java` | 用户需求、系统提示词 | HTML代码字符串 | AI客户端抽象接口 |
| `DoubaoClient.java` | 用户需求、系统提示词 | HTML代码字符串 | 豆包API具体实现 |
| `AIClientFactory.java` | Config配置 | AIFailoverManager或AIClient | AI客户端工厂，创建实例 |
| `AIConnectionDiagnostic.java` | 无（读取Config） | DiagnosticResult | AI连接诊断工具 |
| `Templates.java` | 模板名称 | HTML模板内容 | 模板文件加载和管理 |
| `FilenameGenerator.java` | 用户需求 | 文件名字符串 | 生成唯一文件名 |
| `Config.java` | 配置键名 | 配置值 | 配置管理，优先级处理 |
| `WebConfig.java` | Spring配置属性 | Bean和配置对象 | Spring Boot配置 |
| `ConfigSetup.java` | Scanner输入 | `.env` 文件 | 交互式配置向导 |

---

## 6. 技术栈

- **Java版本**: Java 11
- **构建工具**: Maven 3.x
- **Web框架**: 
  - Spring Boot 2.7.18（主要Web框架）
  - Java原生 HttpServer（备用Web服务器）
- **AI SDK**: 火山引擎 ARK SDK（豆包API）
- **JSON处理**: Gson 2.10.1
- **HTTP客户端**: OkHttp 4.12.0（用于诊断工具）
- **环境变量**: Dotenv Java 3.0.0
- **安全修复**: 
  - Jackson Core 2.15.4（修复CVE-2025-52999）
  - Commons Lang3 3.18.0（修复CVE-2025-48924）

---

## 7. 项目特点

1. **双模式运行**: 支持命令行和Web两种运行方式，满足不同使用场景
2. **AI智能生成**: 优先使用豆包大模型进行智能生成，生成质量高
3. **多接入点容灾**: 支持配置多个AI接入点，自动故障切换，提高可用性
4. **文件缓存机制**: 自动检测已生成文件，避免重复生成
5. **简单模板快速响应**: 对于常见简单需求，直接返回模板，响应速度快
6. **完整诊断工具**: 内置AI连接诊断功能，快速定位配置或网络问题
7. **模块化设计**: 职责分离，易于维护和扩展
8. **配置灵活**: 通过 `.env` 文件管理配置，支持多环境
9. **中文友好**: 完整支持中文文件名、中文内容处理
10. **响应式设计**: Web界面适配不同屏幕尺寸

---

## 8. 目录结构

```
AI/
├── src/
│   └── main/
│       ├── java/
│       │   └── aitool/
│       │       ├── config/                    # 配置管理
│       │       │   ├── Config.java          # 配置管理核心类
│       │       │   ├── ConfigSetup.java     # 交互式配置向导
│       │       │   └── WebConfig.java        # Spring Boot配置
│       │       ├── controller/               # 控制器层
│       │       │   ├── IndexController.java  # 前端页面控制器
│       │       │   └── WebController.java   # RESTful API控制器
│       │       ├── model/                    # 数据模型
│       │       │   ├── CheckResult.java     # 检查结果模型
│       │       │   └── DiagnosticResult.java # 诊断结果模型
│       │       ├── service/                 # 服务层（核心业务逻辑）
│       │       │   ├── AIClient.java         # AI客户端抽象类
│       │       │   ├── AIClientFactory.java  # AI客户端工厂
│       │       │   ├── AIFailoverManager.java # AI容灾管理器
│       │       │   ├── AIConnectionDiagnostic.java # AI连接诊断工具
│       │       │   ├── FilenameGenerator.java # 文件名生成器
│       │       │   ├── HtmlGenerator.java    # HTML生成器核心类
│       │       │   └── Templates.java        # 模板管理器
│       │       ├── Main.java                 # 命令行模式入口
│       │       ├── WebApplication.java        # Spring Boot入口
│       │       └── WebServer.java             # 原生HTTP服务器
│       └── resources/
│           └── web/
│               ├── index.html                # 前端页面
│               └── templates/                # HTML模板
│                   ├── calculator.html       # 计算器模板
│                   ├── custom_tool.html      # 自定义工具模板
│                   ├── table_generator.html  # 表格生成器模板
│                   └── ...                   # 其他模板
├── output/                                   # 生成的HTML文件输出目录
├── .env                                     # 配置文件（需自行创建）
├── env.example                              # 配置模板
├── pom.xml                                  # Maven配置
├── README_WEB.md                            # Web使用说明
└── ARCHITECTURE.md                          # 本文档
```

---

## 9. API接口详细说明

### 9.1 生成工具接口

**URL**: `/api/generate`  
**方法**: `POST`  
**请求头**: `Content-Type: application/json`

**请求体**:
```json
{
  "request": "生成一个计算器工具"
}
```

**成功响应** (200):
```json
{
  "success": true,
  "filename": "计算器_20251216_175430.html",
  "filepath": "F:/JavaProject/AI/output/计算器_20251216_175430.html",
  "htmlContent": "<!DOCTYPE html>..."
}
```

**错误响应** (400/500):
```json
{
  "success": false,
  "error": "错误信息"
}
```

### 9.2 下载文件接口

**URL**: `/api/download?file=文件名.html`  
**方法**: `GET`  
**响应**: 文件下载（Content-Type: text/html; charset=utf-8）
- 支持中文文件名（使用RFC 5987编码）
- 如果文件不存在，返回404

### 9.3 获取文件列表接口

**URL**: `/api/files`  
**方法**: `GET`  
**响应** (200):
```json
[
  "计算器_20251216_175430.html",
  "表格生成器_20251216_151622.html"
]
```

---

## 10. 配置说明

### 10.1 创建配置文件

在项目根目录创建 `.env` 文件，参考 `env.example`：

```properties
# ============================================
# 豆包AI配置 - 主接入点（必需）
# ============================================
DOUBAO_API_KEY=your_api_key_here
DOUBAO_ENDPOINT_ID=your_endpoint_id_here
DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3

# ============================================
# 豆包AI配置 - 备用接入点1（可选，推荐配置）
# ============================================
DOUBAO_API_KEY_2=your_api_key_2_here
DOUBAO_ENDPOINT_ID_2=your_endpoint_id_2_here
DOUBAO_BASE_URL_2=https://ark.cn-beijing.volces.com/api/v3

# ============================================
# 豆包AI配置 - 备用接入点2（可选）
# ============================================
DOUBAO_API_KEY_3=your_api_key_3_here
DOUBAO_ENDPOINT_ID_3=your_endpoint_id_3_here
DOUBAO_BASE_URL_3=https://ark.cn-beijing.volces.com/api/v3

# ============================================
# 功能开关
# ============================================
USE_AI=true
AI_FALLBACK_TO_TEMPLATE=true

# ============================================
# 超时配置（秒）
# ============================================
AI_CONNECT_TIMEOUT=30   # 连接超时（默认30秒）
AI_READ_TIMEOUT=120     # 读取超时（默认120秒，生成大量内容时可能需要更长时间）
AI_WRITE_TIMEOUT=60     # 写入超时（默认60秒）
```

### 10.2 配置优先级

1. `.env` 文件中的配置
2. 系统环境变量
3. 默认配置值（在 `Config.java` 中定义）

### 10.3 多接入点容灾配置

为了获得最佳的可用性和容灾能力，建议配置多个AI接入点：

- **主接入点**：使用 `DOUBAO_API_KEY`、`DOUBAO_ENDPOINT_ID`、`DOUBAO_BASE_URL`
- **备用接入点1**：使用 `DOUBAO_API_KEY_2`、`DOUBAO_ENDPOINT_ID_2`、`DOUBAO_BASE_URL_2`
- **备用接入点2**：使用 `DOUBAO_API_KEY_3`、`DOUBAO_ENDPOINT_ID_3`、`DOUBAO_BASE_URL_3`

当主接入点失败时，系统会自动切换到备用接入点，确保服务可用性。

---

## 11. 扩展指南

### 11.1 添加新的工具模板

1. 在 `resources/web/templates/` 目录下创建新的HTML模板文件（如 `my_tool.html`）
2. 在 `Templates.java` 的 `loadTemplates()` 方法中添加模板名称
3. 在 `HtmlGenerator.java` 中添加对应的工具生成器类：
   ```java
   class MyToolGenerator extends ToolGenerator {
       @Override
       public String generate(String request) {
           return Templates.getTemplate("my_tool");
       }
   }
   ```
4. 在 `HtmlGenerator` 构造函数中注册工具：
   ```java
   tools.put("my_tool", new MyToolGenerator());
   ```
5. 在 `getSimpleExampleTemplate()` 方法中添加简单需求匹配逻辑

### 11.2 添加新的AI客户端

1. 继承 `AIClient` 抽象类：
   ```java
   class MyAIClient extends AIClient {
       @Override
       public String generateHtmlTool(String userRequest, String systemPrompt) throws Exception {
           // 实现AI生成逻辑
       }
       
       @Override
       public void shutdown() {
           // 清理资源
       }
       
       @Override
       public String getClientName() {
           return "我的AI服务";
       }
   }
   ```
2. 在 `AIClientFactory.createFailoverManager()` 中添加创建逻辑
3. 在 `Config.java` 中添加对应的配置项

### 11.3 自定义系统提示词

在调用 `AIClient.generateHtmlTool()` 时，可以传入自定义系统提示词：

```java
String customPrompt = "你是一个专业的HTML工具生成专家...";
String html = aiClient.generateHtmlTool(userRequest, customPrompt);
```

如果不传入，将使用 `AIClient.getDefaultSystemPrompt()` 返回的默认提示词。

---

## 12. 常见问题

### Q: AI生成失败怎么办？
A: 
1. 首先运行诊断工具：在命令行输入 `diagnose` 或 `诊断`
2. 检查 `.env` 文件中的AI配置是否正确
3. 如果配置了多个接入点，系统会自动切换到备用接入点
4. 检查网络连接和防火墙设置
5. 查看控制台输出的详细错误信息

### Q: 如何修改输出目录？
A: 
- **Spring Boot模式**：在 `application.properties` 中设置 `app.output.dir=自定义目录`
- **命令行模式**：修改 `HtmlGenerator` 构造函数中的 `outputDir` 初始化代码
- **WebServer模式**：修改 `WebServer.java` 中的 `OUTPUT_DIR` 常量

### Q: 如何添加自定义模板？
A: 参考"扩展指南"中的"添加新的工具模板"部分。

### Q: 支持哪些AI服务？
A: 目前主要支持豆包（Doubao）API，通过火山引擎ARK SDK调用。可以通过扩展 `AIClient` 接口添加其他AI服务支持。

### Q: 如何启用容灾机制？
A: 在 `.env` 文件中配置多个接入点（`DOUBAO_API_KEY_2`、`DOUBAO_API_KEY_3` 等），系统会自动创建容灾管理器。

### Q: 生成的HTML文件在哪里？
A: 默认保存在项目根目录下的 `output/` 目录中。文件名格式为：`关键词_时间戳.html`。

---

## 13. 设计模式和最佳实践

### 13.1 使用的设计模式

1. **单例模式**：`HtmlGenerator` 使用单例模式，确保全局只有一个实例
2. **工厂模式**：`AIClientFactory` 负责创建AI客户端实例
3. **策略模式**：不同的工具生成器（`ToolGenerator` 子类）实现不同的生成策略
4. **模板方法模式**：`AIClient` 定义生成流程，子类实现具体逻辑

### 13.2 最佳实践

1. **配置管理**：使用 `.env` 文件管理敏感配置，不提交到版本控制
2. **错误处理**：详细的错误信息和友好的错误提示
3. **资源管理**：使用try-with-resources确保资源正确关闭
4. **编码规范**：统一使用UTF-8编码处理中文内容
5. **线程安全**：使用 `AtomicInteger` 保证容灾管理器的线程安全

---

**文档版本**: 2.0  
**最后更新**: 2025-12-16  
**维护者**: AI工具生成器开发团队
