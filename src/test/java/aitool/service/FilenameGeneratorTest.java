package aitool.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilenameGenerator 测试类
 */
@DisplayName("FilenameGenerator 测试")
class FilenameGeneratorTest {

    @Nested
    @DisplayName("文件名生成测试")
    class FilenameGenerationTests {

        @Test
        @DisplayName("测试中文需求生成文件名")
        void testChineseRequest() {
            String request = "生成一个计算器工具";
            String filename = FilenameGenerator.generateFilename(request);
            
            assertNotNull(filename, "文件名不应该为null");
            assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
            assertTrue(filename.contains("计算器") || filename.contains("工具"), 
                "文件名应该包含关键词");
        }

        @Test
        @DisplayName("测试英文需求生成文件名")
        void testEnglishRequest() {
            String request = "generate a calculator tool";
            String filename = FilenameGenerator.generateFilename(request);
            
            assertNotNull(filename, "文件名不应该为null");
            assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
        }

        @Test
        @DisplayName("测试混合中英文需求生成文件名")
        void testMixedRequest() {
            String request = "生成一个table工具";
            String filename = FilenameGenerator.generateFilename(request);
            
            assertNotNull(filename, "文件名不应该为null");
            assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
        }

        @Test
        @DisplayName("测试文件名包含时间戳")
        void testFilenameContainsTimestamp() {
            String request = "计算器";
            String filename = FilenameGenerator.generateFilename(request);
            
            // 时间戳格式: yyyyMMdd_HHmmss
            Pattern timestampPattern = Pattern.compile("_\\d{8}_\\d{6}\\.html$");
            assertTrue(timestampPattern.matcher(filename).find(), 
                "文件名应该包含时间戳");
        }

        @Test
        @DisplayName("测试文件名长度限制")
        void testFilenameLengthLimit() {
            String longRequest = "生成一个非常非常非常非常非常非常非常非常非常非常非常非常长的工具名称";
            String filename = FilenameGenerator.generateFilename(longRequest);
            
            // 文件名应该被限制在合理长度（基础名+时间戳+扩展名）
            assertTrue(filename.length() < 100, "文件名长度应该被限制");
        }

        @Test
        @DisplayName("测试空字符串生成默认文件名")
        void testEmptyStringGeneratesDefault() {
            String filename = FilenameGenerator.generateFilename("");
            
            assertNotNull(filename, "文件名不应该为null");
            assertTrue(filename.contains("tool") || filename.contains("_"), 
                "空字符串应该生成默认文件名");
            assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
        }

        @Test
        @DisplayName("测试特殊字符处理")
        void testSpecialCharacters() {
            String[] specialCases = {
                "生成一个计算器工具！@#",
                "生成一个计算器工具（测试）",
                "生成一个计算器工具【高级版】"
            };

            for (String request : specialCases) {
                String filename = FilenameGenerator.generateFilename(request);
                assertNotNull(filename, "文件名不应该为null");
                assertTrue(filename.endsWith(".html"), "文件名应该以.html结尾");
                // 文件名不应该包含特殊字符（除了下划线和时间戳）
                assertFalse(filename.contains("！") || filename.contains("@") || 
                    filename.contains("#") || filename.contains("（") || 
                    filename.contains("）") || filename.contains("【") || 
                    filename.contains("】"), 
                    "文件名不应该包含特殊字符: " + filename);
            }
        }

        @Test
        @DisplayName("测试多次生成文件名唯一性")
        void testFilenameUniqueness() throws InterruptedException {
            String request = "计算器";
            String filename1 = FilenameGenerator.generateFilename(request);
            
            // 等待至少1秒确保时间戳不同
            Thread.sleep(1100);
            
            String filename2 = FilenameGenerator.generateFilename(request);
            
            assertNotEquals(filename1, filename2, 
                "不同时间生成的文件名应该不同");
        }
    }
}

