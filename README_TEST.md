# 自动化测试文档

本文档介绍HTML工具生成器项目的自动化测试脚本。

## 测试结构

项目包含以下测试类：

### 1. HtmlGeneratorTest
测试 `HtmlGenerator` 类的核心功能：
- **简单示例模板测试**: 测试各种工具模板的匹配（计算器、表格、文本替换等）
- **文件查找功能测试**: 测试已存在文件的查找逻辑
- **关键词提取测试**: 测试从用户需求中提取关键词
- **高级功能关键词检测测试**: 测试识别高级功能需求（如科学计算器）
- **边界情况测试**: 测试空字符串、null值、特殊字符等边界情况
- **单例模式测试**: 测试单例模式的正确性

### 2. FilenameGeneratorTest
测试 `FilenameGenerator` 类的文件名生成功能：
- 中文、英文、混合语言的文件名生成
- 时间戳的包含和格式
- 文件名长度限制
- 特殊字符处理
- 文件名唯一性

### 3. ToolCategoryClassifierTest
测试 `ToolCategoryClassifier` 类的工具分类功能：
- 效率工具、创造工具、管理工具、处理工具的分类
- 规则匹配分类
- 边界情况处理
- 分类枚举的完整性

### 4. IntegrationTest
集成测试，测试多个组件协同工作：
- 完整的工具生成流程
- 文件名和分类的一致性
- 多个工具类型的生成
- 错误处理

## 运行测试

### 方法1: 使用Maven命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=HtmlGeneratorTest

# 运行特定测试方法
mvn test -Dtest=HtmlGeneratorTest#testCalculatorTemplateMatch
```

### 方法2: 使用测试脚本

**Windows:**
```cmd
run-tests.bat
```

**Linux/Mac:**
```bash
chmod +x run-tests.sh
./run-tests.sh
```

### 方法3: 使用IDE

在IntelliJ IDEA或Eclipse中：
1. 右键点击 `src/test/java` 目录
2. 选择 "Run All Tests" 或 "Run Tests in..."

## 测试覆盖率

当前测试覆盖以下功能：

✅ 简单示例模板匹配  
✅ 文件名生成  
✅ 工具分类（规则匹配）  
✅ 边界情况处理  
✅ 单例模式  
✅ 集成测试  

⚠️ 注意：由于AI客户端需要配置，以下功能在无AI配置时可能无法完全测试：
- AI生成HTML工具
- AI辅助文件查找
- AI工具分类

## 测试依赖

项目使用以下测试框架：
- **JUnit 5**: 测试框架
- **Mockito**: Mock框架（用于模拟依赖）
- **Spring Boot Test**: Spring Boot测试支持

所有测试依赖已在 `pom.xml` 中配置。

## 编写新测试

添加新测试时，请遵循以下规范：

1. **测试类命名**: `*Test.java`
2. **测试方法命名**: 使用 `@DisplayName` 注解提供中文描述
3. **测试组织**: 使用 `@Nested` 类组织相关测试
4. **断言**: 使用JUnit 5的断言方法
5. **临时文件**: 使用 `@TempDir` 创建临时测试文件

示例：

```java
@Nested
@DisplayName("功能测试")
class FeatureTests {
    @Test
    @DisplayName("测试某个功能")
    void testFeature() {
        // 测试代码
        assertTrue(condition, "错误消息");
    }
}
```

## 持续集成

测试可以在CI/CD流程中自动运行：

```yaml
# GitHub Actions 示例
- name: Run tests
  run: mvn test
```

## 故障排查

### 问题1: 测试失败 - AI配置不可用
**解决方案**: 这是正常的。某些测试需要AI配置，如果没有配置，相关测试会跳过或使用规则匹配。

### 问题2: 测试失败 - 文件路径问题
**解决方案**: 确保测试使用 `@TempDir` 创建临时目录，避免依赖实际文件系统。

### 问题3: 测试失败 - 时间戳唯一性测试
**解决方案**: 如果测试运行很快，时间戳可能相同。测试中已包含1秒延迟。

## 贡献

添加新功能时，请同时添加相应的测试用例，确保：
1. 新功能有测试覆盖
2. 边界情况被测试
3. 错误处理被测试

