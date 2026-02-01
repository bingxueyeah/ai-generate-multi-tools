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
     * @param userRequest 用户需求
     * @param skipExistingCheck 是否跳过已存在文件的检查（如果外部已经检查过，可以设置为true避免重复检查）
     */
    public String generateTool(String userRequest, boolean skipExistingCheck) throws Exception {
        // 步骤1: 先检查是否是简单示例需求，如果是则直接返回对应模板
        // （优先检查简单示例需求，避免返回错误的已存在文件）
        String simpleTemplate = getSimpleExampleTemplate(userRequest);
        if (simpleTemplate != null) {
            System.out.println("✓ 检测到简单示例需求，直接返回模板");
            return simpleTemplate;
        }
        
        // 步骤2: 检查output目录中是否已经存在对应的文件（如果外部已检查过，可以跳过）
        if (!skipExistingCheck) {
            String existingHtml = findExistingFile(userRequest);
            if (existingHtml != null) {
                System.out.println("✓ 找到已生成的文件，直接返回");
                return existingHtml;
            }
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
     * 根据用户需求生成HTML工具（默认会检查已存在的文件）
     */
    public String generateTool(String userRequest) throws Exception {
        return generateTool(userRequest, false);
    }
    
    /**
     * 检查output目录中是否已存在对应的文件
     * 先通过匹配文件名中的关键词来判断
     * 如果文件名匹配失败，则调用AI进行检验
     * @param userRequest 用户需求
     * @return 如果找到已存在的文件，返回文件内容；否则返回null
     */
    public String findExistingFile(String userRequest) {
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            return null;
        }
        
        // 步骤1: 先用文件名匹配（原有逻辑）
        String result = findExistingFileByName(userRequest);
        if (result != null) {
            System.out.println("✓ 通过文件名匹配找到已生成的文件");
            return result;
        }
        
        // 步骤2: 如果文件名匹配失败，调用AI进行检验
        if (useAi) {
            System.out.println("AI正在分析文件是否曾生成过...");
            try {
                result = findExistingFileByAI(userRequest);
                if (result != null) {
                    System.out.println("✓ AI检验确认已生成过，返回已有文件");
                    return result;
                } else {
                    System.out.println("✓ AI检验确认未生成过");
                }
            } catch (Exception e) {
                System.out.println("⚠ AI检验失败: " + e.getMessage() + "，继续生成新文件");
                // AI检验失败不影响生成新文件，返回null继续后续流程
            }
        }
        
        return null;
    }
    
    /**
     * 通过文件名匹配查找已存在的文件（改进的逻辑，考虑功能差异）
     */
    private String findExistingFileByName(String userRequest) {
        // 提取需求的关键词（用于匹配文件名）
        String[] keywords = extractKeywords(userRequest);
        if (keywords.length == 0) {
            return null;
        }
        
        // 检查用户需求是否包含高级功能关键词
        boolean hasAdvancedFeatures = containsAdvancedFeatureKeywords(userRequest);
        
        // 收集所有分类文件夹下的HTML文件
        java.util.List<File> allFiles = getAllHtmlFiles(outputDir);
        if (allFiles.isEmpty()) {
            return null;
        }
        
        // 遍历文件，查找匹配的文件
        // 优先返回最近匹配的文件（按修改时间排序，最新文件优先）
        File bestMatch = null;
        int bestMatchCount = 0;
        
        for (File file : allFiles) {
            String filename = file.getName().toLowerCase();
            
            // 计算匹配的关键词数量
            int matchCount = 0;
            for (String keyword : keywords) {
                if (filename.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            
            // 如果用户需求包含高级功能关键词，要求更严格的匹配
            if (hasAdvancedFeatures) {
                // 必须所有关键词都匹配，且文件名不能缺少关键功能词
                if (matchCount == keywords.length) {
                    // 检查文件名是否也包含高级功能关键词
                    boolean filenameHasAdvanced = containsAdvancedFeatureKeywords(filename);
                    if (filenameHasAdvanced) {
                        // 文件名也包含高级功能关键词，认为是匹配的
                        if (matchCount > bestMatchCount || (matchCount == bestMatchCount && file.lastModified() > (bestMatch != null ? bestMatch.lastModified() : 0))) {
                            bestMatch = file;
                            bestMatchCount = matchCount;
                        }
                    }
                    // 如果文件名不包含高级功能关键词，即使所有关键词都匹配，也不认为是匹配的
                }
            } else {
                // 普通匹配：如果匹配的关键词数量超过一半，或者所有关键词都匹配，则认为是匹配的
                if (matchCount > 0 && (matchCount >= keywords.length / 2 || matchCount == keywords.length)) {
                    if (matchCount > bestMatchCount || (matchCount == bestMatchCount && file.lastModified() > (bestMatch != null ? bestMatch.lastModified() : 0))) {
                        bestMatch = file;
                        bestMatchCount = matchCount;
                    }
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
     * 检查文本是否包含高级功能关键词
     */
    private boolean containsAdvancedFeatureKeywords(String text) {
        String textLower = text.toLowerCase();
        // 高级功能关键词列表
        String[] advancedKeywords = {
            "科学", "高级", "专业", "增强", "扩展", "完整", "全功能", "全面",
            "三角函数", "trigonometric", "trigonometry", "sin", "cos", "tan", "对数", "log", "指数", "exp", 
            "矩阵", "统计", "微积分", "积分", "微分",
            "正则表达式", "regex", "批量", "多文件", "并发", "异步",
            "可视化", "图表", "图形", "绘图",
            "scientific", "advanced", "professional", "enhanced", "matrix", "statistics", "calculus"
        };
        
        for (String keyword : advancedKeywords) {
            if (textLower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 通过AI检验是否已生成过对应的文件
     */
    private String findExistingFileByAI(String userRequest) throws Exception {
        // 收集所有分类文件夹下的HTML文件
        java.util.List<File> allFiles = getAllHtmlFiles(outputDir);
        if (allFiles.isEmpty()) {
            return null;
        }
        
        // 构建文件列表信息（用于AI判断）
        StringBuilder fileListInfo = new StringBuilder();
        java.util.Set<String> addedNames = new java.util.HashSet<>(); // 用于去重
        for (File file : allFiles) {
            // 移除时间戳后缀，保留原始文件名部分（用于AI判断）
            String filename = file.getName();
            // 移除最后的 .html 扩展名和时间戳
            // 例如: "生成一个计算器工具_20260114_190914.html" -> "生成一个计算器工具"
            String cleanName = filename.replaceAll("_\\d{8}_\\d{6}\\.html$", "");
            cleanName = cleanName.replaceAll("\\.html$", "");
            
            // 去重：只添加未添加过的文件名
            if (!addedNames.contains(cleanName)) {
                addedNames.add(cleanName);
                fileListInfo.append("- ").append(cleanName).append("\n");
            }
        }
        
        // 构建AI提示词
        String systemPrompt = "你是一个文件匹配助手。根据用户需求和已生成的文件名列表，判断是否有匹配的文件。\n\n" +
                "重要规则（必须严格遵守）：\n" +
                "1. 只有当用户需求与某个文件名表达的意思完全相同，且功能要求也完全一致时，才认为已生成过\n" +
                "2. 如果用户需求包含更高级、更具体的功能要求，即使文件名相似，也不能认为是匹配的\n" +
                "   例如：\"科学计算器\"（需要三角函数等功能）与\"计算器\"（普通计算器）不匹配\n" +
                "   \"高级文本编辑器\"与\"文本编辑器\"不匹配\n" +
                "   \"支持正则表达式的文本替换工具\"与\"文本替换工具\"不匹配\n" +
                "3. 如果用户需求包含以下关键词，需要更严格的匹配：\n" +
                "   - 科学、高级、专业、增强、扩展、完整、全功能等表示更高级功能的词\n" +
                "   - 三角函数、对数、指数、矩阵、统计等具体功能词\n" +
                "   - 正则表达式、批量处理、多文件等高级特性词\n" +
                "4. 只考虑文件名的核心含义，忽略时间戳等无关信息\n" +
                "5. 如果找到完全匹配的文件，请只返回该文件的完整文件名（包含.html扩展名）\n" +
                "6. 如果没有完全匹配的文件，请只返回\"无\"或\"none\"\n" +
                "7. 只返回文件名，不要有其他说明文字\n\n" +
                "已生成的文件列表：\n" + fileListInfo.toString();
        
        String aiRequest = "用户需求：" + userRequest + "\n\n请严格按照上述规则判断是否有完全匹配的文件。\n" +
                "特别注意：如果用户需求包含更高级的功能要求，即使文件名相似，也不能认为是匹配的。\n" +
                "如果有完全匹配的文件，请返回完整的文件名（包含.html扩展名）；如果没有，请返回\"无\"。";
        
        // 调用AI
        String aiResponse;
        if (failoverManager != null) {
            aiResponse = failoverManager.generateText(aiRequest, systemPrompt);
        } else if (aiClient != null) {
            aiResponse = aiClient.generateText(aiRequest, systemPrompt);
        } else {
            return null;
        }
        
        // 解析AI返回结果
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }
        
        String response = aiResponse.trim().toLowerCase();
        // 检查是否返回"无"或"none"
        if (response.equals("无") || response.equals("none") || response.equals("没有") || response.equals("no")) {
            return null;
        }
        
        // 提取文件名（可能包含.html或不包含）
        String matchedFilename = extractFilenameFromAIResponse(aiResponse, allFiles);
        if (matchedFilename == null) {
            return null;
        }
        
        // 查找对应的文件
        File matchedFile = null;
        for (File file : allFiles) {
            String filename = file.getName();
            // 检查文件名是否匹配（考虑带或不带.html的情况）
            if (filename.equals(matchedFilename) || 
                filename.equals(matchedFilename + ".html") ||
                filename.toLowerCase().equals(matchedFilename.toLowerCase()) ||
                filename.toLowerCase().equals(matchedFilename.toLowerCase() + ".html")) {
                matchedFile = file;
                break;
            }
            // 也检查文件名中的核心部分是否匹配
            String cleanName = filename.replaceAll("_\\d{8}_\\d{6}\\.html$", "").replaceAll("\\.html$", "");
            if (cleanName.equals(matchedFilename.replaceAll("\\.html$", ""))) {
                matchedFile = file;
                break;
            }
        }
        
        // 如果找到了匹配的文件，返回文件内容
        if (matchedFile != null && matchedFile.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(matchedFile.toPath());
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("读取AI匹配的文件失败: " + matchedFile.getAbsolutePath());
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 从AI响应中提取文件名
     */
    private String extractFilenameFromAIResponse(String aiResponse, java.util.List<File> allFiles) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return null;
        }
        
        // 清理响应文本（移除可能的markdown标记等）
        String cleaned = aiResponse.trim();
        // 移除markdown代码块标记
        cleaned = cleaned.replaceAll("```[\\w]*", "").trim();
        // 移除可能的说明文字，只保留文件名
        String[] lines = cleaned.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 如果行包含.html或者看起来像文件名
            if (line.toLowerCase().endsWith(".html") || line.matches(".*[_\\u4e00-\\u9fa5\\w]+\\.html.*")) {
                // 提取文件名部分
                line = line.replaceAll(".*?([_\\u4e00-\\u9fa5\\w]+\\.html).*", "$1");
                if (!line.isEmpty()) {
                    return line;
                }
            }
        }
        
        // 如果没有明显的.html，尝试直接使用第一行作为文件名
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            // 检查是否有对应的文件
            for (File file : allFiles) {
                String filename = file.getName();
                String cleanName = filename.replaceAll("_\\d{8}_\\d{6}\\.html$", "").replaceAll("\\.html$", "");
                if (firstLine.contains(cleanName) || cleanName.contains(firstLine)) {
                    return filename;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取所有分类文件夹下的HTML文件列表（递归搜索）
     */
    private java.util.List<File> getAllHtmlFiles(File directory) {
        java.util.List<File> htmlFiles = new java.util.ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return htmlFiles;
        }
        
        File[] items = directory.listFiles();
        if (items == null) {
            return htmlFiles;
        }
        
        for (File item : items) {
            if (item.isDirectory()) {
                // 递归搜索子目录
                htmlFiles.addAll(getAllHtmlFiles(item));
            } else if (item.isFile() && item.getName().toLowerCase().endsWith(".html")) {
                // 添加HTML文件
                htmlFiles.add(item);
            }
        }
        
        return htmlFiles;
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
     * 检查是否是简单示例需求，如果是则返回对应的模板（公开方法）
     * 简单示例：与前端示例完全匹配的需求
     * @param userRequest 用户需求
     * @return 如果是简单示例，返回对应的HTML模板；否则返回null
     */
    public String checkSimpleExampleTemplate(String userRequest) {
        return getSimpleExampleTemplate(userRequest);
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
        
        // 完全匹配（检查用户输入是否完全包含关键词，避免误判）
        // 只有当用户输入完全等于关键词或包含关键词作为独立词时，才返回简单模板
        // 注意：如果用户需求包含高级功能关键词（如三角函数、科学计算等），则不应匹配简单模板
        boolean hasAdvancedFeatures = containsAdvancedFeatureKeywords(userRequest);
        
        if (!hasAdvancedFeatures && exactlyMatches(requestLower, new String[]{"计算器", "calculator"})) {
            return tools.get("calculator").generate(userRequest);
        }
        if (!hasAdvancedFeatures && exactlyMatches(requestLower, new String[]{"表格生成器", "表格", "table"})) {
            return tools.get("table").generate(userRequest);
        }
        if (!hasAdvancedFeatures && exactlyMatches(requestLower, new String[]{"文本替换", "replace"})) {
            return tools.get("text_replace").generate(userRequest);
        }
        if (!hasAdvancedFeatures && exactlyMatches(requestLower, new String[]{"json格式化", "json格式", "json formatter"})) {
            return tools.get("json_formatter").generate(userRequest);
        }
        if (!hasAdvancedFeatures && exactlyMatches(requestLower, new String[]{"数据转换", "data converter"})) {
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
     * 检查用户输入是否完全匹配关键词（作为独立词，不是其他词的一部分）
     * 例如："计算器"匹配，"生成一个计算器"匹配，"科学计算器"不匹配
     */
    private boolean exactlyMatches(String text, String[] keywords) {
        for (String keyword : keywords) {
            String keywordLower = keyword.toLowerCase();
            
            // 1. 完全等于关键词
            if (text.equals(keywordLower)) {
                return true;
            }
            // 2. 等于"生成一个" + 关键词
            if (text.equals("生成一个" + keywordLower)) {
                return true;
            }
            // 3. 等于关键词 + "工具"
            if (text.equals(keywordLower + "工具")) {
                return true;
            }
            // 4. 等于"生成一个" + 关键词 + "工具"
            if (text.equals("生成一个" + keywordLower + "工具")) {
                return true;
            }
            
            // 5. 检查关键词是否是独立的词（前后是边界、空格或标点，不是其他词的一部分）
            int index = text.indexOf(keywordLower);
            if (index >= 0) {
                // 检查前一个字符：允许的修饰词前缀
                boolean validBefore = (index == 0);
                if (!validBefore) {
                    char beforeChar = text.charAt(index - 1);
                    // 允许空格、标点符号
                    if (Character.isWhitespace(beforeChar) || !Character.isLetterOrDigit(beforeChar) && !isChineseChar(beforeChar)) {
                        validBefore = true;
                    } else if (isChineseChar(beforeChar)) {
                        // 如果是中文字符，检查是否是允许的修饰词（如"一个"、"的"等）
                        // 检查前面是否是"生成一个"、"一个"等允许的前缀
                        String beforeText = text.substring(0, index);
                        if (beforeText.endsWith("生成一个") || beforeText.endsWith("一个") || 
                            beforeText.endsWith("的") || beforeText.endsWith("个")) {
                            validBefore = true;
                        } else {
                            // 前面有中文字符但不是允许的修饰词，说明是其他词的一部分，不匹配
                            validBefore = false;
                        }
                    } else {
                        // 前面是字母数字，说明是其他词的一部分，不匹配
                        validBefore = false;
                    }
                }
                
                // 检查后一个字符：必须是边界、空格、标点或"工具"
                int afterIndex = index + keywordLower.length();
                boolean validAfter = (afterIndex >= text.length());
                if (!validAfter) {
                    char afterChar = text.charAt(afterIndex);
                    // 允许空格、标点符号
                    if (Character.isWhitespace(afterChar) || !Character.isLetterOrDigit(afterChar) && !isChineseChar(afterChar)) {
                        validAfter = true;
                    } else if (isChineseChar(afterChar)) {
                        // 如果是中文字符，检查是否是"工具"
                        String afterText = text.substring(afterIndex);
                        if (afterText.startsWith("工具")) {
                            validAfter = true;
                        } else {
                            // 后面有中文字符但不是"工具"，说明是其他词的一部分，不匹配
                            validAfter = false;
                        }
                    } else {
                        // 后面是字母数字，说明是其他词的一部分，不匹配
                        validAfter = false;
                    }
                }
                
                // 如果前后都有效，说明关键词是独立的词
                if (validBefore && validAfter) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检查字符是否是中文字符
     */
    private boolean isChineseChar(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
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
