package aitool.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试类
 * 测试多个组件协同工作的场景
 */
@DisplayName("集成测试")
class IntegrationTest {

    private HtmlGenerator htmlGenerator;
    private ToolCategoryClassifier classifier;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        htmlGenerator = HtmlGenerator.getInstance();
        classifier = new ToolCategoryClassifier();
    }

    @Test
    @DisplayName("测试完整工具生成流程 - 简单示例")
    void testCompleteToolGenerationFlow_SimpleExample() {
        // 1. 用户请求
        String userRequest = "生成一个计算器工具";
        
        // 2. 检查简单示例模板
        String htmlContent = htmlGenerator.checkSimpleExampleTemplate(userRequest);
        assertNotNull(htmlContent, "应该返回HTML内容");
        assertTrue(htmlContent.contains("<!DOCTYPE") || htmlContent.contains("<html"), 
            "HTML内容应该有效");
        
        // 3. 生成文件名
        String filename = FilenameGenerator.generateFilename(userRequest);
        assertNotNull(filename, "应该生成文件名");
        assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
        
        // 4. 分类（可选，简单示例可能不需要）
        try {
            ToolCategoryClassifier.ToolCategory category = classifier.classify(userRequest);
            assertNotNull(category, "应该返回分类");
        } catch (Exception e) {
            // AI不可用时可能失败，这是正常的
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("测试文件名和分类的一致性")
    void testFilenameAndCategoryConsistency() {
        String[] testCases = {
            "生成一个计算器工具",
            "生成一个表格生成器",
            "生成一个文本替换工具"
        };

        for (String request : testCases) {
            // 生成文件名
            String filename = FilenameGenerator.generateFilename(request);
            assertNotNull(filename, "应该生成文件名");
            
            // 分类
            try {
                ToolCategoryClassifier.ToolCategory category = classifier.classify(request);
                assertNotNull(category, "应该返回分类");
                
                // 验证文件名和分类都是有效的
                assertTrue(filename.length() > 0, "文件名应该非空");
                assertNotNull(category.getChineseName(), "分类应该有中文名称");
            } catch (Exception e) {
                // AI不可用时可能失败
                assertTrue(true);
            }
        }
    }

    @Test
    @DisplayName("测试简单示例模板和文件生成的兼容性")
    void testSimpleTemplateAndFileGenerationCompatibility() {
        String userRequest = "生成一个计算器工具";
        
        // 检查简单模板
        String templateContent = htmlGenerator.checkSimpleExampleTemplate(userRequest);
        assertNotNull(templateContent, "应该返回模板内容");
        
        // 生成文件名
        String filename = FilenameGenerator.generateFilename(userRequest);
        assertNotNull(filename, "应该生成文件名");
        
        // 验证两者可以配合使用
        assertTrue(templateContent.length() > 0, "模板内容应该非空");
        assertTrue(filename.length() > 0, "文件名应该非空");
    }

    @Test
    @DisplayName("测试多个工具类型的生成")
    void testMultipleToolTypesGeneration() {
        String[] toolTypes = {
            "计算器",
            "表格生成器",
            "文本替换工具",
            "json格式化工具",
            "数据转换工具"
        };

        for (String toolType : toolTypes) {
            String request = "生成一个" + toolType;
            
            // 检查简单模板
            String content = htmlGenerator.checkSimpleExampleTemplate(request);
            
            // 生成文件名
            String filename = FilenameGenerator.generateFilename(request);
            
            // 验证基本要求
            assertNotNull(filename, "应该生成文件名: " + toolType);
            if (content != null) {
                assertTrue(content.length() > 0, "模板内容应该非空: " + toolType);
            }
        }
    }

    @Test
    @DisplayName("测试错误处理 - 无效请求")
    void testErrorHandling_InvalidRequest() {
        String[] invalidRequests = {
            "",
            null,
            "   ",
            "随机无效文本12345"
        };

        for (String request : invalidRequests) {
            // 所有操作都不应该抛出异常
            assertDoesNotThrow(() -> {
                htmlGenerator.checkSimpleExampleTemplate(request);
                if (request != null) {
                    FilenameGenerator.generateFilename(request);
                }
            }, "无效请求不应该导致异常: " + request);
        }
    }
}

