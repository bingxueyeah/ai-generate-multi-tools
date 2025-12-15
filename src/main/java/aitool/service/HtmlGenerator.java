package aitool.service;

import java.util.HashMap;
import java.util.Map;
import aitool.config.Config;

/**
 * HTML工具生成器核心类
 */
public class HtmlGenerator {
    
    private final Map<String, ToolGenerator> tools;
    private AIClient aiClient;
    private boolean useAi;
    
    private static HtmlGenerator instance;
    
    public HtmlGenerator() {
        tools = new HashMap<>();
        tools.put("table", new TableGeneratorTool());
        tools.put("calculator", new CalculatorTool());
        tools.put("text_replace", new TextReplaceTool());
        tools.put("data_converter", new DataConverterTool());
        tools.put("json_formatter", new JsonFormatterTool());
        tools.put("csv_processor", new CsvProcessorTool());
        
        // 初始化AI客户端
        this.useAi = false;
        if (Config.checkAiConfig()) {
            this.useAi = Config.getBool("USE_AI", true);
            if (this.useAi) {
                this.aiClient = AIClientFactory.createAIClient();
                if (this.aiClient != null) {
                    System.out.println("✓ AI生成模式已启用（豆包API）");
                } else {
                    System.out.println("⚠ AI配置不完整，将使用模板模式");
                    this.useAi = false;
                }
            } else {
                System.out.println("⚠ AI功能已禁用（USE_AI=false），将使用模板模式");
            }
        } else {
            // 检查是否存在 .env 文件
            java.io.File envFile = new java.io.File(System.getProperty("user.dir"), ".env");
            if (!envFile.exists()) {
                System.out.println("⚠ 未检测到AI配置，将使用模板模式");
                System.out.println("  提示: 请在项目根目录创建 .env 文件并配置AI密钥");
                System.out.println("  参考: 可查看 env.example 文件了解配置格式");
                try {
                    Config.createExampleConfig();
                    System.out.println("  ✓ 已创建配置文件模板: env.example");
                } catch (Exception e) {
                    // 忽略创建模板文件的错误
                }
            } else {
                System.out.println("⚠ .env 文件存在但配置不完整，将使用模板模式");
                System.out.println("  提示: 请检查 .env 文件中的AI密钥配置");
            }
        }
    }
    
    /**
     * 根据用户需求生成HTML工具
     */
    public String generateTool(String userRequest) throws Exception {
        // 尝试使用AI生成
        if (useAi && aiClient != null) {
            try {
                System.out.println("🤖 正在使用AI分析需求并生成工具...");
                String htmlContent = aiClient.generateHtmlTool(userRequest, null);
                
                // 验证生成的HTML是否有效
                if (htmlContent != null && htmlContent.length() > 100) {
                    if (htmlContent.contains("<!DOCTYPE") || htmlContent.contains("<html")) {
                        System.out.println("✓ AI生成成功！");
                        return htmlContent;
                    } else {
                        System.out.println("⚠ AI生成的内容格式不正确，回退到模板模式");
                    }
                } else {
                    System.out.println("⚠ AI生成的内容过短，回退到模板模式");
                }
            } catch (Exception e) {
                System.out.println("⚠ AI生成失败: " + e.getMessage());
                
                // 打印详细的错误信息以便调试
                if (e.getCause() != null) {
                    System.out.println("   详细错误: " + e.getCause().getMessage());
                }
                
                // 如果是超时错误，提供解决建议
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("超时")) {
                    System.out.println("   建议: 可以尝试增加超时时间配置（在.env文件中设置AI_READ_TIMEOUT）");
                } else if (errorMsg != null && (errorMsg.contains("认证") || errorMsg.contains("401"))) {
                    System.out.println("   建议: 请检查.env文件中的DOUBAO_API_KEY配置是否正确");
                } else if (errorMsg != null && errorMsg.contains("连接")) {
                    System.out.println("   建议: 请检查网络连接和DOUBAO_BASE_URL配置");
                }
                
                if (Config.getBool("AI_FALLBACK_TO_TEMPLATE", true)) {
                    System.out.println("   正在使用模板模式作为备选方案...");
                } else {
                    throw e;
                }
            }
        }
        
        // 回退到模板模式
        return generateWithTemplate(userRequest);
    }
    
    /**
     * 使用模板生成工具
     */
    private String generateWithTemplate(String userRequest) {
        // 分析需求，确定工具类型
        String toolType = analyzeRequest(userRequest);
        
        // 获取对应的工具生成器
        ToolGenerator toolGenerator = tools.get(toolType);
        
        if (toolGenerator == null) {
            // 如果没有匹配的工具，使用通用模板
            return generateCustomTool(userRequest);
        }
        
        // 生成工具
        return toolGenerator.generate(userRequest);
    }
    
    /**
     * 分析用户需求，确定工具类型
     */
    private String analyzeRequest(String request) {
        String requestLower = request.toLowerCase();
        
        // 表格相关关键词
        if (containsKeyword(requestLower, new String[]{"表格", "table", "列表", "数据表"})) {
            return "table";
        }
        
        // 计算相关关键词
        if (containsKeyword(requestLower, new String[]{"计算", "calculator", "算", "公式"})) {
            return "calculator";
        }
        
        // 文本替换相关关键词
        if (containsKeyword(requestLower, new String[]{"替换", "replace", "查找替换", "文本替换"})) {
            return "text_replace";
        }
        
        // 数据转换相关关键词
        if (containsKeyword(requestLower, new String[]{"转换", "convert", "格式转换", "数据转换"})) {
            return "data_converter";
        }
        
        // JSON格式化相关关键词
        if (containsKeyword(requestLower, new String[]{"json", "格式化", "format"})) {
            return "json_formatter";
        }
        
        // CSV处理相关关键词
        if (containsKeyword(requestLower, new String[]{"csv", "逗号分隔"})) {
            return "csv_processor";
        }
        
        // 默认返回表格工具
        return "table";
    }
    
    private boolean containsKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 生成自定义工具（通用模板）
     */
    private String generateCustomTool(String request) {
        return Templates.formatTemplate(
            Templates.getTemplate("custom_tool"),
            "自定义工具",
            request,
            "请输入你的需求描述..."
        );
    }
    
    /**
     * 获取单例实例
     */
    public static HtmlGenerator getInstance() {
        if (instance == null) {
            instance = new HtmlGenerator();
        }
        return instance;
    }
    
    /**
     * 重新加载实例（用于配置更新后）
     */
    public static HtmlGenerator reloadInstance() {
        instance = new HtmlGenerator();
        return instance;
    }
}

/**
 * 工具生成器基类
 */
abstract class ToolGenerator {
    public abstract String generate(String request);
}

/**
 * 表格生成工具
 */
class TableGeneratorTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("table_generator");
    }
}

/**
 * 计算工具
 */
class CalculatorTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("calculator");
    }
}

/**
 * 文本替换工具
 */
class TextReplaceTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("text_replace");
    }
}

/**
 * 数据转换工具
 */
class DataConverterTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("data_converter");
    }
}

/**
 * JSON格式化工具
 */
class JsonFormatterTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("json_formatter");
    }
}

/**
 * CSV处理工具
 */
class CsvProcessorTool extends ToolGenerator {
    @Override
    public String generate(String request) {
        return Templates.getTemplate("csv_processor");
    }
}
