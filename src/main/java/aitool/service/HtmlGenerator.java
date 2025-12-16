package aitool.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import aitool.config.Config;

/**
 * HTML工具生成器核心类
 */
public class HtmlGenerator {
    
    private final Map<String, ToolGenerator> tools;
    private AIClient aiClient;  // 保留用于向后兼容
    private AIFailoverManager failoverManager;  // 容灾管理器
    private boolean useAi;
    private File outputDir;
    
    private static HtmlGenerator instance;
    
    public HtmlGenerator() {
        tools = new HashMap<>();
        tools.put("table", new TableGeneratorTool());
        tools.put("calculator", new CalculatorTool());
        tools.put("text_replace", new TextReplaceTool());
        tools.put("data_converter", new DataConverterTool());
        tools.put("json_formatter", new JsonFormatterTool());
        tools.put("csv_processor", new CsvProcessorTool());
        
        // 初始化输出目录
        String projectRoot = System.getProperty("user.dir");
        this.outputDir = new File(projectRoot, "output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // 初始化AI客户端（使用容灾管理器）
        this.useAi = false;
        if (Config.checkAiConfig()) {
            this.useAi = Config.getBool("USE_AI", true);
            if (this.useAi) {
                // 优先使用容灾管理器
                this.failoverManager = AIClientFactory.createFailoverManager();
                if (this.failoverManager != null) {
                    System.out.println("✓ AI生成模式已启用（容灾机制）");
                    System.out.println("  已配置的AI服务: " + this.failoverManager.getClientNames());
                    // 为了向后兼容，也创建单个客户端（使用第一个）
                    // 但实际调用时会使用容灾管理器
                } else {
                    // 如果容灾管理器创建失败，尝试创建单个客户端（向后兼容）
                    this.aiClient = AIClientFactory.createAIClient();
                    if (this.aiClient != null) {
                        System.out.println("✓ AI生成模式已启用（单客户端模式）");
                    } else {
                        System.out.println("⚠ AI配置不完整，将使用模板模式");
                        this.useAi = false;
                    }
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
                System.out.println("  建议: 配置多个豆包接入点以获得容灾能力");
                try {
                    Config.createExampleConfig();
                    System.out.println("  ✓ 已创建配置文件模板: env.example");
                } catch (Exception e) {
                    // 忽略创建模板文件的错误
                }
            } else {
                System.out.println("⚠ .env 文件存在但配置不完整，将使用模板模式");
                System.out.println("  提示: 请检查 .env 文件中的AI密钥配置");
                System.out.println("  建议: 配置多个豆包接入点以获得容灾能力");
            }
        }
    }
    
    /**
     * 根据用户需求生成HTML工具
     */
    public String generateTool(String userRequest) throws Exception {
        // 步骤1: 检查output目录中是否已经存在对应的文件
        String existingHtml = findExistingFile(userRequest);
        if (existingHtml != null) {
            System.out.println("✓ 找到已生成的文件，直接返回");
            return existingHtml;
        }
        
        // 步骤2: 检查是否是简单示例需求，如果是则直接返回对应模板
        String simpleTemplate = getSimpleExampleTemplate(userRequest);
        if (simpleTemplate != null) {
            System.out.println("✓ 检测到简单示例需求，直接返回模板");
            return simpleTemplate;
        }
        
        // 步骤3: 其他情况调用AI生成（使用容灾机制）
        if (useAi) {
            try {
                System.out.println("🤖 正在使用AI分析需求并生成工具...");
                String htmlContent;
                
                // 优先使用容灾管理器
                if (failoverManager != null) {
                    htmlContent = failoverManager.generateHtmlTool(userRequest, null);
                } else if (aiClient != null) {
                    // 向后兼容：使用单个客户端
                    htmlContent = aiClient.generateHtmlTool(userRequest, null);
                } else {
                    throw new Exception("AI客户端未初始化");
                }
                
                // 验证生成的HTML是否有效
                if (htmlContent != null && htmlContent.length() > 100) {
                    if (htmlContent.contains("<!DOCTYPE") || htmlContent.contains("<html")) {
                        System.out.println("✓ AI生成成功！");
                        return htmlContent;
                    } else {
                        throw new Exception("AI生成的内容格式不正确，缺少必要的HTML标签");
                    }
                } else {
                    throw new Exception("AI生成的内容过短，可能生成失败");
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
                    System.out.println("   建议: 请检查.env文件中的AI密钥配置是否正确");
                } else if (errorMsg != null && errorMsg.contains("连接")) {
                    System.out.println("   建议: 请检查网络连接和API配置");
                } else if (errorMsg != null && errorMsg.contains("所有AI服务调用均失败")) {
                    System.out.println("   建议: 请检查至少一个AI服务的配置是否正确，或稍后再试");
                }
                
                // AI失败时抛出异常，不再回退到模板模式
                throw new Exception("AI生成失败，请稍后再试。错误信息: " + e.getMessage());
            }
        } else {
            // AI未启用或配置不可用
            throw new Exception("AI功能未启用或配置不可用，无法生成模板。请检查AI配置或稍后再试。");
        }
    }
    
    /**
     * 检查output目录中是否已存在对应的文件
     * 通过匹配文件名中的关键词来判断
     */
    private String findExistingFile(String userRequest) {
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            return null;
        }
        
        // 提取需求的关键词（用于匹配文件名）
        String[] keywords = extractKeywords(userRequest);
        if (keywords.length == 0) {
            return null;
        }
        
        File[] files = outputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".html"));
        if (files == null) {
            return null;
        }
        
        // 遍历文件，查找匹配的文件
        // 优先返回最近匹配的文件（按文件名排序，最新文件在后）
        File bestMatch = null;
        int bestMatchCount = 0;
        
        for (File file : files) {
            String filename = file.getName().toLowerCase();
            
            // 计算匹配的关键词数量
            int matchCount = 0;
            for (String keyword : keywords) {
                if (filename.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            
            // 如果匹配的关键词数量超过一半，或者所有关键词都匹配，则认为是匹配的
            if (matchCount > 0 && (matchCount >= keywords.length / 2 || matchCount == keywords.length)) {
                if (matchCount > bestMatchCount || (matchCount == bestMatchCount && file.lastModified() > (bestMatch != null ? bestMatch.lastModified() : 0))) {
                    bestMatch = file;
                    bestMatchCount = matchCount;
                }
            }
        }
        
        // 返回最佳匹配的文件内容
        if (bestMatch != null) {
            try {
                byte[] bytes = Files.readAllBytes(bestMatch.toPath());
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("读取已存在文件失败: " + bestMatch.getAbsolutePath());
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 从用户需求中提取关键词（用于匹配文件名）
     */
    private String[] extractKeywords(String request) {
        // 移除常见的描述性词汇
        String cleaned = request.replaceAll("生成一个|生成|一个|工具", "");
        cleaned = cleaned.trim();
        
        // 提取中文词和英文词
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]+|\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);
        
        java.util.List<String> keywords = new java.util.ArrayList<>();
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > 1) { // 忽略单字符
                keywords.add(word);
            }
        }
        
        return keywords.toArray(new String[0]);
    }
    
    /**
     * 检查是否是简单示例需求，如果是则返回对应的模板
     * 简单示例：与前端示例完全匹配的需求
     */
    private String getSimpleExampleTemplate(String userRequest) {
        String requestLower = userRequest.trim().toLowerCase();
        
        // 定义简单示例需求及其对应的模板
        Map<String, String> simpleExamples = new HashMap<>();
        simpleExamples.put("生成一个计算器工具", "calculator");
        simpleExamples.put("生成一个表格生成器", "table");
        simpleExamples.put("生成一个文本替换工具", "text_replace");
        simpleExamples.put("生成一个json格式化工具", "json_formatter");
        simpleExamples.put("生成一个数据转换工具", "data_converter");
        // 添加一些变体
        simpleExamples.put("计算器", "calculator");
        simpleExamples.put("计算器工具", "calculator");
        simpleExamples.put("表格", "table");
        simpleExamples.put("表格生成器", "table");
        simpleExamples.put("表格工具", "table");
        simpleExamples.put("文本替换", "text_replace");
        simpleExamples.put("文本替换工具", "text_replace");
        simpleExamples.put("json格式化", "json_formatter");
        simpleExamples.put("json格式化工具", "json_formatter");
        simpleExamples.put("数据转换", "data_converter");
        simpleExamples.put("数据转换工具", "data_converter");
        
        // 精确匹配
        if (simpleExamples.containsKey(requestLower)) {
            String templateName = simpleExamples.get(requestLower);
            ToolGenerator toolGenerator = tools.get(templateName);
            if (toolGenerator != null) {
                return toolGenerator.generate(userRequest);
            }
        }
        
        // 部分匹配（检查是否包含关键词，但要确保需求足够简单）
        // 只匹配明确的简单需求，避免误判
        if (containsKeyword(requestLower, new String[]{"计算器", "calculator"}) && 
            requestLower.length() < 20) { // 限制长度确保是简单需求
            return tools.get("calculator").generate(userRequest);
        }
        if (containsKeyword(requestLower, new String[]{"表格生成器", "表格", "table"}) && 
            requestLower.length() < 20) {
            return tools.get("table").generate(userRequest);
        }
        if (containsKeyword(requestLower, new String[]{"文本替换", "replace"}) && 
            requestLower.length() < 20) {
            return tools.get("text_replace").generate(userRequest);
        }
        if (containsKeyword(requestLower, new String[]{"json格式化", "json格式", "json formatter"}) && 
            requestLower.length() < 25) {
            return tools.get("json_formatter").generate(userRequest);
        }
        if (containsKeyword(requestLower, new String[]{"数据转换", "data converter"}) && 
            requestLower.length() < 20) {
            return tools.get("data_converter").generate(userRequest);
        }
        
        return null;
    }
    
    /**
     * 使用模板生成工具（已废弃，保留以防其他地方调用）
     */
    @Deprecated
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
