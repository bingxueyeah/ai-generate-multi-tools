package aitool.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * AI配置设置工具
 * 用于交互式配置AI密钥
 */
public class ConfigSetup {
    
    /**
     * 交互式配置AI
     */
    public static void setupConfig(Scanner scanner) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("AI配置向导");
        System.out.println("=".repeat(60));
        System.out.println("\n请选择配置方式：");
        System.out.println("1. 使用代理配置（DOUBAO_API_KEY + DOUBAO_ENDPOINT_ID）");
        System.out.println("2. 使用官方配置（VOLC_ACCESSKEY + VOLC_SECRETKEY）");
        System.out.println("3. 取消配置");
        System.out.print("\n请选择 (1/2/3): ");
        
        String choice = scanner.nextLine().trim();
        
        Map<String, String> config = new HashMap<>();
        
        if (choice.equals("1")) {
            // 代理配置方式
            System.out.println("\n--- 代理配置方式 ---");
            System.out.print("请输入 DOUBAO_API_KEY: ");
            String apiKey = scanner.nextLine().trim();
            if (apiKey.isEmpty()) {
                System.out.println("⚠ API Key 不能为空，配置已取消");
                return;
            }
            
            System.out.print("请输入 DOUBAO_ENDPOINT_ID: ");
            String endpointId = scanner.nextLine().trim();
            if (endpointId.isEmpty()) {
                System.out.println("⚠ Endpoint ID 不能为空，配置已取消");
                return;
            }
            
            config.put("DOUBAO_API_KEY", apiKey);
            config.put("DOUBAO_ENDPOINT_ID", endpointId);
            config.put("USE_AI", "true");
            config.put("AI_FALLBACK_TO_TEMPLATE", "true");
            
        } else if (choice.equals("2")) {
            // 官方配置方式
            System.out.println("\n--- 官方配置方式 ---");
            System.out.print("请输入 VOLC_ACCESSKEY: ");
            String accessKey = scanner.nextLine().trim();
            if (accessKey.isEmpty()) {
                System.out.println("⚠ Access Key 不能为空，配置已取消");
                return;
            }
            
            System.out.print("请输入 VOLC_SECRETKEY: ");
            String secretKey = scanner.nextLine().trim();
            if (secretKey.isEmpty()) {
                System.out.println("⚠ Secret Key 不能为空，配置已取消");
                return;
            }
            
            System.out.print("请输入 DOUBAO_ENDPOINT_ID (可选，按回车跳过): ");
            String endpointId = scanner.nextLine().trim();
            
            config.put("VOLC_ACCESSKEY", accessKey);
            config.put("VOLC_SECRETKEY", secretKey);
            if (!endpointId.isEmpty()) {
                config.put("DOUBAO_ENDPOINT_ID", endpointId);
            }
            config.put("USE_AI", "true");
            config.put("AI_FALLBACK_TO_TEMPLATE", "true");
            
        } else {
            System.out.println("配置已取消");
            return;
        }
        
        // 保存配置
        try {
            Config.saveConfig(config);
            System.out.println("\n✓ AI配置已保存成功！");
            System.out.println("配置已写入 .env 文件");
            
            // 验证配置
            if (Config.checkAiConfig()) {
                System.out.println("✓ 配置验证通过，AI功能已启用");
            } else {
                System.out.println("⚠ 配置验证失败，请检查配置是否正确");
            }
            
        } catch (IOException e) {
            System.out.println("✗ 保存配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查并提示配置
     */
    public static boolean checkAndPrompt(Scanner scanner) {
        if (!Config.checkAiConfig()) {
            System.out.println("\n⚠ 未检测到AI配置，将使用模板模式");
            System.out.print("是否现在配置AI？(Y/n): ");
            String answer = scanner.nextLine().trim().toLowerCase();
            
            if (answer.isEmpty() || answer.equals("y") || answer.equals("yes") || answer.equals("是")) {
                setupConfig(scanner);
                return Config.checkAiConfig();
            } else {
                System.out.println("提示: 您可以稍后通过运行配置向导来设置AI");
                System.out.println("     或在项目根目录创建 .env 文件手动配置");
                return false;
            }
        }
        return true;
    }
}
