package aitool.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTML工具模板
 * 从资源文件加载HTML模板，便于管理和维护
 */
public class Templates {
    
    private static final String TEMPLATE_DIR = "/web/templates/";
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    
    static {
        // 初始化时加载所有模板
        loadTemplates();
    }
    
    /**
     * 从资源文件加载模板
     */
    private static void loadTemplates() {
        String[] templateNames = {
            "custom_tool",
            "table_generator", 
            "calculator",
            "text_replace",
            "data_converter",
            "json_formatter",
            "csv_processor"
        };
        
        for (String templateName : templateNames) {
            String content = loadTemplateFromResource(templateName + ".html");
            if (content != null) {
                TEMPLATES.put(templateName, content);
            }
        }
        
        // 如果text_replace等模板文件不存在，使用custom_tool作为默认模板
        String customTool = TEMPLATES.get("custom_tool");
        if (customTool != null) {
            if (!TEMPLATES.containsKey("text_replace")) {
                TEMPLATES.put("text_replace", customTool);
            }
            if (!TEMPLATES.containsKey("data_converter")) {
                TEMPLATES.put("data_converter", customTool);
            }
            if (!TEMPLATES.containsKey("json_formatter")) {
                TEMPLATES.put("json_formatter", customTool);
            }
            if (!TEMPLATES.containsKey("csv_processor")) {
                TEMPLATES.put("csv_processor", customTool);
            }
        }
    }
    
    /**
     * 从资源文件读取HTML模板
     */
    private static String loadTemplateFromResource(String filename) {
        String resourcePath = TEMPLATE_DIR + filename;
        try (InputStream is = Templates.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("警告: 无法加载模板文件: " + resourcePath);
                return null;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        } catch (Exception e) {
            System.err.println("加载模板文件失败: " + resourcePath);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取模板内容
     */
    public static String getTemplate(String templateName) {
        String template = TEMPLATES.get(templateName);
        if (template == null) {
            // 如果指定的模板不存在，返回自定义工具模板
            template = TEMPLATES.get("custom_tool");
            if (template == null) {
                System.err.println("警告: 模板 '" + templateName + "' 不存在，且默认模板也不存在");
                return "";
            }
        }
        return template;
    }
    
    /**
     * 格式化模板（替换占位符）
     */
    public static String formatTemplate(String template, String title, String description, String placeholder) {
        return template.replace("{title}", title)
                      .replace("{description}", description)
                      .replace("{placeholder}", placeholder);
    }
    
    /**
     * 重新加载所有模板（用于模板文件更新后）
     */
    public static void reloadTemplates() {
        TEMPLATES.clear();
        loadTemplates();
    }
}
