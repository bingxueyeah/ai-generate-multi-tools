package aitool.service;

import aitool.config.Config;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 工具分类器
 * 通过AI判断用户需求属于4类中的哪一类
 */
public class ToolCategoryClassifier {
    
    // 工具分类枚举
    public enum ToolCategory {
        EFFICIENCY("效率工具", "efficiency"),
        CREATIVE("创造工具", "creative"),
        MANAGEMENT("管理工具", "management"),
        PROCESSING("处理工具", "processing");
        
        private final String chineseName;
        private final String englishName;
        
        ToolCategory(String chineseName, String englishName) {
            this.chineseName = chineseName;
            this.englishName = englishName;
        }
        
        public String getChineseName() {
            return chineseName;
        }
        
        public String getEnglishName() {
            return englishName;
        }
    }
    
    private AIFailoverManager failoverManager;
    private AIClient aiClient;
    private boolean useAi;
    
    public ToolCategoryClassifier() {
        // 初始化AI客户端（使用容灾管理器）
        this.useAi = false;
        if (Config.checkAiConfig()) {
            this.useAi = Config.getBool("USE_AI", true);
            if (this.useAi) {
                // 优先使用容灾管理器
                this.failoverManager = AIClientFactory.createFailoverManager();
                if (this.failoverManager != null) {
                    System.out.println("✓ 工具分类器已初始化（容灾机制）");
                } else {
                    // 如果容灾管理器创建失败，尝试创建单个客户端（向后兼容）
                    this.aiClient = AIClientFactory.createAIClient();
                    if (this.aiClient != null) {
                        System.out.println("✓ 工具分类器已初始化（单客户端模式）");
                    } else {
                        System.out.println("⚠ AI配置不完整，分类器将使用规则匹配模式");
                        this.useAi = false;
                    }
                }
            }
        }
    }
    
    /**
     * 判断用户需求属于哪一类工具
     * @param userRequest 用户需求
     * @return 工具分类
     */
    public ToolCategory classify(String userRequest) throws Exception {
        if (userRequest == null || userRequest.trim().isEmpty()) {
            return ToolCategory.PROCESSING; // 默认返回处理工具
        }
        
        // 如果AI可用，使用AI分类
        if (useAi) {
            try {
                return classifyWithAI(userRequest);
            } catch (Exception e) {
                System.out.println("⚠ AI分类失败: " + e.getMessage() + "，将使用规则匹配");
                // AI失败时回退到规则匹配
                return classifyWithRules(userRequest);
            }
        } else {
            // AI不可用时使用规则匹配
            return classifyWithRules(userRequest);
        }
    }
    
    /**
     * 使用AI进行分类
     */
    private ToolCategory classifyWithAI(String userRequest) throws Exception {
        String systemPrompt = "你是一个工具分类专家。根据用户的需求描述，判断这个需求属于以下4类工具中的哪一类：\n\n" +
                "1. 效率工具：用于提高工作效率、节省时间的工具，如：待办事项、时间管理、任务清单、提醒工具、快捷方式工具等\n" +
                "2. 创造工具：用于创作、设计、生成的工具，如：代码生成器、图片编辑器、文本编辑器、图表制作、音乐制作等\n" +
                "3. 管理工具：用于管理、组织、分析数据的工具，如：数据管理、项目管理、文件管理、数据分析、报表生成等\n" +
                "4. 处理工具：用于处理、转换、格式化数据的工具，如：格式转换、数据处理、文本处理、文件处理、计算器等\n\n" +
                "请仔细分析用户需求，只返回分类名称（中文），必须是以下4个词之一：效率工具、创造工具、管理工具、处理工具\n" +
                "不要返回其他任何内容，只返回分类名称。";
        
        String result;
        try {
            // 优先使用容灾管理器（使用generateText方法，不验证HTML格式）
            if (failoverManager != null) {
                result = failoverManager.generateText(userRequest, systemPrompt);
            } else if (aiClient != null) {
                result = aiClient.generateText(userRequest, systemPrompt);
            } else {
                throw new Exception("AI客户端未初始化");
            }
        } catch (Exception e) {
            throw new Exception("AI分类调用失败: " + e.getMessage(), e);
        }
        
        // 从AI返回结果中提取分类
        if (result == null || result.trim().isEmpty()) {
            throw new Exception("AI返回结果为空");
        }
        
        // 清理结果，移除可能的markdown标记等
        result = result.trim();
        result = result.replaceAll("```.*?```", ""); // 移除代码块
        result = result.replaceAll("`", ""); // 移除反引号
        result = result.replaceAll("\\s+", ""); // 移除空白字符
        
        // 匹配分类名称
        if (result.contains("效率工具") || result.contains("效率")) {
            return ToolCategory.EFFICIENCY;
        } else if (result.contains("创造工具") || result.contains("创造")) {
            return ToolCategory.CREATIVE;
        } else if (result.contains("管理工具") || result.contains("管理")) {
            return ToolCategory.MANAGEMENT;
        } else if (result.contains("处理工具") || result.contains("处理")) {
            return ToolCategory.PROCESSING;
        }
        
        // 如果没有匹配到，使用规则匹配作为备用
        System.out.println("⚠ AI分类结果不明确: " + result + "，使用规则匹配");
        return classifyWithRules(userRequest);
    }
    
    /**
     * 使用规则匹配进行分类（备用方案）
     */
    private ToolCategory classifyWithRules(String userRequest) {
        String requestLower = userRequest.toLowerCase();
        
        // 效率工具关键词
        if (containsKeyword(requestLower, new String[]{
            "待办", "todo", "任务清单", "时间管理", "提醒", "日程", "效率", 
            "快捷", "快捷键", "快捷方式", "提高效率", "节省时间"
        })) {
            return ToolCategory.EFFICIENCY;
        }
        
        // 创造工具关键词
        if (containsKeyword(requestLower, new String[]{
            "生成", "创建", "制作", "设计", "编辑", "绘图", "画图", 
            "编辑器", "创作", "代码生成", "图片", "图表", "音乐", "视频"
        })) {
            return ToolCategory.CREATIVE;
        }
        
        // 管理工具关键词
        if (containsKeyword(requestLower, new String[]{
            "管理", "组织", "分析", "统计", "报表", "数据管理", 
            "项目管理", "文件管理", "数据库", "看板"
        })) {
            return ToolCategory.MANAGEMENT;
        }
        
        // 处理工具关键词（默认）
        if (containsKeyword(requestLower, new String[]{
            "处理", "转换", "格式化", "计算", "计算器", "解析", 
            "文本处理", "文件处理", "数据转换", "格式转换"
        })) {
            return ToolCategory.PROCESSING;
        }
        
        // 默认返回处理工具
        return ToolCategory.PROCESSING;
    }
    
    /**
     * 检查文本中是否包含关键词
     */
    private boolean containsKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取分类文件夹路径（相对于output目录）
     */
    public static String getCategoryFolderPath(ToolCategory category) {
        return category.getChineseName();
    }
}

