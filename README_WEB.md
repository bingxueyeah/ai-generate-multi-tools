# HTML工具生成器 - Web应用使用说明

## 功能说明

这是一个本地Web应用，可以根据用户需求自动生成可用的HTML小工具。

## 启动方式

### 方式1：使用Maven启动Web服务器

```bash
# 启动Web服务器（默认端口8080）
mvn exec:java@web

# 或者指定端口
mvn exec:java@web -Dexec.args="9000"
```

### 方式2：使用Java直接运行

```bash
# 先编译项目
mvn clean package

# 运行Web服务器
java -cp target/com-1.0-SNAPSHOT.jar aitool.WebServer

# 或者指定端口
java -cp target/com-1.0-SNAPSHOT.jar aitool.WebServer 9000
```

### 方式3：使用Main类启动（支持命令行参数）

```bash
# 启动Web服务器模式
java -cp target/com-1.0-SNAPSHOT.jar aitool.Main --web

# 或者指定端口
java -cp target/com-1.0-SNAPSHOT.jar aitool.Main --web 9000

# 启动命令行模式（原有功能）
java -cp target/com-1.0-SNAPSHOT.jar aitool.Main
```

## 访问应用

启动成功后，在浏览器中访问：

```
http://localhost:8080
```

## 使用说明

1. **左侧面板**：输入您的需求描述
   - 例如："生成一个计算器工具"
   - 例如："生成一个表格生成器"
   - 可以点击示例需求快速填充

2. **右侧面板**：显示生成的工具
   - 生成成功后，会显示工具预览
   - 可以点击"下载文件"按钮保存到本地
   - 生成的文件保存在 `output` 目录下

## API接口

### 生成工具
- **URL**: `/api/generate`
- **方法**: POST
- **请求体**: `{"request": "您的需求描述"}`
- **响应**: 
  ```json
  {
    "success": true,
    "filename": "生成的文件名.html",
    "filepath": "完整文件路径",
    "htmlContent": "HTML内容"
  }
  ```

### 下载文件
- **URL**: `/api/download?file=文件名.html`
- **方法**: GET
- **响应**: 文件下载

### 获取文件列表
- **URL**: `/api/files`
- **方法**: GET
- **响应**: 文件列表JSON数组

## 注意事项

1. 确保已配置AI密钥（可选，如果不配置将使用模板模式）
   - 在项目根目录创建 `.env` 文件
   - 参考 `env.example` 文件配置

2. 生成的文件保存在 `output` 目录下

3. 默认端口为8080，如果端口被占用，可以指定其他端口

## 功能特点

- ✅ 美观的Web界面
- ✅ 实时预览生成的工具
- ✅ 一键下载生成的HTML文件
- ✅ 支持AI生成和模板生成两种模式
- ✅ 响应式设计，适配不同屏幕尺寸
