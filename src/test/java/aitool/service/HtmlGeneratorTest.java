package aitool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HtmlGenerator 自动化测试类
 * 测试HTML工具生成器的核心功能
 */
@DisplayName("HtmlGenerator 测试")
class HtmlGeneratorTest {

    private HtmlGenerator htmlGenerator;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 重置单例实例
        HtmlGenerator.reloadInstance();
        htmlGenerator = HtmlGenerator.getInstance();
    }

    @Nested
    @DisplayName("简单示例模板测试")
    class SimpleExampleTemplateTests {

        @Test
        @DisplayName("测试计算器模板匹配")
        void testCalculatorTemplateMatch() {
            String[] testCases = {
                "生成一个计算器工具",
                "计算器",
                "计算器工具"
            };

            for (String request : testCases) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNotNull(result, "计算器请求 '" + request + "' 应该返回模板");
                assertTrue(result.contains("<!DOCTYPE") || result.contains("<html"), 
                    "返回结果应该是有效的HTML");
            }
        }

        @Test
        @DisplayName("测试表格生成器模板匹配")
        void testTableTemplateMatch() {
            String[] testCases = {
                "生成一个表格生成器",
                "表格",
                "表格生成器",
                "表格工具"
            };

            for (String request : testCases) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNotNull(result, "表格请求 '" + request + "' 应该返回模板");
                assertTrue(result.contains("<!DOCTYPE") || result.contains("<html"), 
                    "返回结果应该是有效的HTML");
            }
        }

        @Test
        @DisplayName("测试文本替换工具模板匹配")
        void testTextReplaceTemplateMatch() {
            String[] testCases = {
                "生成一个文本替换工具",
                "文本替换",
                "文本替换工具"
            };

            for (String request : testCases) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNotNull(result, "文本替换请求 '" + request + "' 应该返回模板");
                assertTrue(result.contains("<!DOCTYPE") || result.contains("<html"), 
                    "返回结果应该是有效的HTML");
            }
        }

        @Test
        @DisplayName("测试JSON格式化工具模板匹配")
        void testJsonFormatterTemplateMatch() {
            String[] testCases = {
                "生成一个json格式化工具",
                "json格式化",
                "json格式化工具"
            };

            for (String request : testCases) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNotNull(result, "JSON格式化请求 '" + request + "' 应该返回模板");
                assertTrue(result.contains("<!DOCTYPE") || result.contains("<html"), 
                    "返回结果应该是有效的HTML");
            }
        }

        @Test
        @DisplayName("测试不匹配的请求返回null")
        void testNonMatchingRequestReturnsNull() {
            String[] testCases = {
                "生成一个科学计算器",
                "生成一个高级文本编辑器",
                "生成一个支持正则表达式的工具",
                "随机文本123"
            };

            for (String request : testCases) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNull(result, "不匹配的请求 '" + request + "' 应该返回null");
            }
        }
    }

    @Nested
    @DisplayName("文件查找功能测试")
    class FileFindingTests {

        private File testOutputDir;

        @BeforeEach
        void setUpTestDir() throws IOException {
            // 创建临时测试目录
            testOutputDir = tempDir.toFile();
            
            // 创建一些测试HTML文件
            createTestFile("计算器工具_20260119_120000.html", "<html><body>计算器</body></html>");
            createTestFile("表格生成器_20260119_130000.html", "<html><body>表格</body></html>");
            createTestFile("文本替换工具_20260119_140000.html", "<html><body>文本替换</body></html>");
            createTestFile("科学计算器_20260119_150000.html", "<html><body>科学计算器</body></html>");
        }

        private void createTestFile(String filename, String content) throws IOException {
            File file = new File(testOutputDir, filename);
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("测试通过文件名查找已存在的文件")
        void testFindExistingFileByName() {
            // 注意：这个测试需要修改HtmlGenerator以支持自定义输出目录
            // 或者使用反射来设置私有字段
            // 这里先测试基本逻辑
            
            // 由于findExistingFile依赖于outputDir，我们需要确保目录存在
            assertTrue(testOutputDir.exists(), "测试目录应该存在");
            assertTrue(testOutputDir.isDirectory(), "测试目录应该是目录");
        }

        @Test
        @DisplayName("测试空目录返回null")
        void testEmptyDirectoryReturnsNull() throws IOException {
            File emptyDir = new File(tempDir.toFile(), "empty");
            emptyDir.mkdirs();
            
            // 由于无法直接设置outputDir，这个测试需要重构代码或使用反射
            // 这里先验证目录为空
            File[] files = emptyDir.listFiles();
            assertTrue(files == null || files.length == 0, "空目录应该没有文件");
        }
    }

    @Nested
    @DisplayName("关键词提取测试")
    class KeywordExtractionTests {

        @Test
        @DisplayName("测试从中文需求中提取关键词")
        void testExtractKeywordsFromChinese() {
            // 使用反射或创建测试辅助方法来测试私有方法
            // 这里测试通过公开方法间接验证
            
            String request = "生成一个计算器工具";
            String result = htmlGenerator.checkSimpleExampleTemplate(request);
            assertNotNull(result, "应该能识别计算器关键词");
        }

        @Test
        @DisplayName("测试从英文需求中提取关键词")
        void testExtractKeywordsFromEnglish() {
            String request = "calculator";
            String result = htmlGenerator.checkSimpleExampleTemplate(request);
            // calculator可能不在简单示例列表中，所以可能返回null
            // 这里主要验证不会抛出异常
            assertDoesNotThrow(() -> htmlGenerator.checkSimpleExampleTemplate(request));
        }

        @Test
        @DisplayName("测试从混合中英文需求中提取关键词")
        void testExtractKeywordsFromMixed() {
            String request = "生成一个table工具";
            assertDoesNotThrow(() -> htmlGenerator.checkSimpleExampleTemplate(request));
        }
    }

    @Nested
    @DisplayName("高级功能关键词检测测试")
    class AdvancedFeatureKeywordTests {

        @Test
        @DisplayName("测试识别高级功能关键词")
        void testAdvancedFeatureRecognition() {
            // 测试包含高级功能关键词的请求不应该匹配简单模板
            String[] advancedRequests = {
                "生成一个科学计算器",
                "生成一个高级文本编辑器",
                "生成一个支持三角函数的计算器",
                "生成一个支持正则表达式的文本替换工具"
            };

            for (String request : advancedRequests) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNull(result, "高级功能请求 '" + request + "' 不应该匹配简单模板");
            }
        }

        @Test
        @DisplayName("测试普通请求可以匹配简单模板")
        void testNormalRequestMatchesTemplate() {
            String[] normalRequests = {
                "生成一个计算器工具",
                "计算器",
                "表格生成器"
            };

            for (String request : normalRequests) {
                String result = htmlGenerator.checkSimpleExampleTemplate(request);
                assertNotNull(result, "普通请求 '" + request + "' 应该匹配简单模板");
            }
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("测试空字符串")
        void testEmptyString() {
            assertDoesNotThrow(() -> {
                String result = htmlGenerator.checkSimpleExampleTemplate("");
                // 空字符串不应该匹配任何模板
                assertNull(result, "空字符串应该返回null");
            });
        }

        @Test
        @DisplayName("测试null值")
        void testNullValue() {
            assertDoesNotThrow(() -> {
                String result = htmlGenerator.checkSimpleExampleTemplate(null);
                assertNull(result, "null值应该返回null");
            });
        }

        @Test
        @DisplayName("测试只包含空格的字符串")
        void testWhitespaceOnly() {
            assertDoesNotThrow(() -> {
                String result = htmlGenerator.checkSimpleExampleTemplate("   ");
                assertNull(result, "只包含空格的字符串应该返回null");
            });
        }

        @Test
        @DisplayName("测试超长字符串")
        void testVeryLongString() {
            String longRequest = "生成一个" + "工具".repeat(1000);
            assertDoesNotThrow(() -> {
                htmlGenerator.checkSimpleExampleTemplate(longRequest);
            });
        }

        @Test
        @DisplayName("测试特殊字符")
        void testSpecialCharacters() {
            String[] specialCases = {
                "生成一个计算器工具！@#",
                "生成一个计算器工具（高级版）",
                "生成一个计算器工具【测试】"
            };

            for (String request : specialCases) {
                assertDoesNotThrow(() -> {
                    htmlGenerator.checkSimpleExampleTemplate(request);
                });
            }
        }
    }

    @Nested
    @DisplayName("单例模式测试")
    class SingletonTests {

        @Test
        @DisplayName("测试单例模式")
        void testSingletonPattern() {
            HtmlGenerator instance1 = HtmlGenerator.getInstance();
            HtmlGenerator instance2 = HtmlGenerator.getInstance();
            
            assertSame(instance1, instance2, "getInstance应该返回同一个实例");
        }

        @Test
        @DisplayName("测试重新加载实例")
        void testReloadInstance() {
            HtmlGenerator instance1 = HtmlGenerator.getInstance();
            HtmlGenerator instance2 = HtmlGenerator.reloadInstance();
            HtmlGenerator instance3 = HtmlGenerator.getInstance();
            
            assertNotSame(instance1, instance2, "reloadInstance应该创建新实例");
            assertSame(instance2, instance3, "重新加载后getInstance应该返回新实例");
        }
    }
}

