package aitool.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理类
 */
public class Config {
    
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.dir"), ".env");
    private static Dotenv dotenv = null;
    
    // 默认配置
    private static final Map<String, String> DEFAULTS = new HashMap<String, String>() {{
        // 豆包配置（主接入点）
        put("DOUBAO_API_KEY", "");
        put("DOUBAO_ENDPOINT_ID", "");
        put("DOUBAO_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3");
        
        // 豆包配置（备用接入点1）
        put("DOUBAO_API_KEY_2", "");
        put("DOUBAO_ENDPOINT_ID_2", "");
        put("DOUBAO_BASE_URL_2", "");
        
        // 豆包配置（备用接入点2）
        put("DOUBAO_API_KEY_3", "");
        put("DOUBAO_ENDPOINT_ID_3", "");
        put("DOUBAO_BASE_URL_3", "");
        
        // 火山引擎官方配置（兼容旧配置）
        put("VOLC_ACCESSKEY", "");
        put("VOLC_SECRETKEY", "");
        
        // 通用配置
        put("USE_AI", "true");
        put("AI_FALLBACK_TO_TEMPLATE", "true");
        put("AI_CONNECT_TIMEOUT", "30");  // 连接超时（秒）
        put("AI_READ_TIMEOUT", "120");     // 读取超时（秒），默认120秒，适合生成大量内容
        put("AI_WRITE_TIMEOUT", "60");     // 写入超时（秒）
    }};
    
    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        if (dotenv == null) {
            File envFile = CONFIG_FILE.toFile();
            if (envFile.exists()) {
                dotenv = Dotenv.configure()
                    .directory(envFile.getParent())
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();
            } else {
                // 如果文件不存在，使用系统环境变量
                dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            }
        }
    }
    
    /**
     * 创建示例配置文件
     */
    public static void createExampleConfig() throws IOException {
        File exampleFile = Paths.get(System.getProperty("user.dir"), "env.example").toFile();
        if (!exampleFile.exists()) {
            try (FileWriter writer = new FileWriter(exampleFile, false)) {
                writer.write("# 豆包AI配置模板\n");
                writer.write("# 使用说明：\n");
                writer.write("# 1. 复制此文件为 .env 文件（在项目根目录）\n");
                writer.write("# 2. 根据你的情况选择配置方式（方式1或方式2，二选一）\n");
                writer.write("# 3. 填写对应的密钥信息，保存后程序会自动读取\n");
                writer.write("\n");
                writer.write("# ============================================\n");
                writer.write("# 方式1：代理配置方式（推荐，更简单）\n");
                writer.write("# ============================================\n");
                writer.write("# 如果你有 DOUBAO_API_KEY，使用此方式\n");
                writer.write("DOUBAO_API_KEY=your_api_key_here\n");
                writer.write("DOUBAO_ENDPOINT_ID=your_endpoint_id_here\n");
                writer.write("\n");
                writer.write("# ============================================\n");
                writer.write("# 方式2：官方配置方式\n");
                writer.write("# ============================================\n");
                writer.write("# 如果你有火山引擎的 AccessKey 和 SecretKey，使用此方式\n");
                writer.write("# VOLC_ACCESSKEY=your_access_key_here\n");
                writer.write("# VOLC_SECRETKEY=your_secret_key_here\n");
                writer.write("# DOUBAO_ENDPOINT_ID=your_endpoint_id_here\n");
                writer.write("\n");
                writer.write("# ============================================\n");
                writer.write("# 其他配置项\n");
                writer.write("# ============================================\n");
                writer.write("# API基础URL（一般不需要修改）\n");
                writer.write("DOUBAO_BASE_URL=https://ark.cn-beijing.volces.com/api/v3\n");
                writer.write("\n");
                writer.write("# 是否启用AI生成（true/false）\n");
                writer.write("USE_AI=true\n");
                writer.write("\n");
                writer.write("# AI生成失败时是否回退到模板模式（true/false）\n");
                writer.write("AI_FALLBACK_TO_TEMPLATE=true\n");
                writer.write("\n");
                writer.write("# 超时配置（秒）- 如果经常遇到超时错误，可以增加这些值\n");
                writer.write("# AI_CONNECT_TIMEOUT=30   # 连接超时（默认30秒）\n");
                writer.write("# AI_READ_TIMEOUT=120     # 读取超时（默认120秒，生成大量内容时可能需要更长时间）\n");
                writer.write("# AI_WRITE_TIMEOUT=60     # 写入超时（默认60秒）\n");
            }
        }
    }
    
    /**
     * 获取配置值
     */
    public static String get(String key) {
        return get(key, null);
    }
    
    /**
     * 获取配置值
     */
    public static String get(String key, String defaultValue) {
        loadConfig();
        
        String value = null;
        if (dotenv != null) {
            value = dotenv.get(key);
        }
        
        if (value == null || value.isEmpty()) {
            value = System.getenv(key);
        }
        
        if (value == null || value.isEmpty()) {
            value = DEFAULTS.getOrDefault(key, defaultValue);
        }
        
        return value != null ? value : defaultValue;
    }
    
    /**
     * 获取布尔配置值
     */
    public static boolean getBool(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        String lowerValue = value.toLowerCase().trim();
        return lowerValue.equals("true") || lowerValue.equals("1") || 
               lowerValue.equals("yes") || lowerValue.equals("on");
    }
    
    /**
     * 保存配置到文件
     */
    public static void saveConfig(Map<String, String> configDict) throws IOException {
        File envFile = CONFIG_FILE.toFile();
        envFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(envFile, false)) {
            writer.write("# 豆包AI配置\n");
            writer.write("# 配置说明请参考README.md\n");
            writer.write("\n");
            
            for (Map.Entry<String, String> entry : configDict.entrySet()) {
                if (DEFAULTS.containsKey(entry.getKey())) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            }
        }
        
        System.out.println("配置已保存到 " + CONFIG_FILE);
        // 重新加载配置
        dotenv = null;
        loadConfig();
    }
    
    /**
     * 检查AI配置是否完整
     */
    public static boolean checkAiConfig() {
        String doubaoApiKey = get("DOUBAO_API_KEY");
        
        // 检查是否有至少一个有效的API密钥配置
        String doubaoApiKey2 = Config.get("DOUBAO_API_KEY_2");
        String doubaoApiKey3 = Config.get("DOUBAO_API_KEY_3");
        return (doubaoApiKey != null && !doubaoApiKey.isEmpty()) ||
               (doubaoApiKey2 != null && !doubaoApiKey2.isEmpty()) ||
               (doubaoApiKey3 != null && !doubaoApiKey3.isEmpty());
    }
}
