package aitool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import aitool.config.ConfigSetup;
import aitool.service.AIConnectionDiagnostic;
import aitool.service.FilenameGenerator;
import aitool.service.HtmlGenerator;

/**
 * 通用HTML工具生成器
 * 根据用户需求自动生成可用的HTML小工具
 */
public class Main {
    
    public static void main(String[] args) {
        // 检查是否启动Web服务器模式
        if (args.length > 0 && ("--web".equals(args[0]) || "-w".equals(args[0]))) {
            int port = 8080;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.out.println("无效的端口号，使用默认端口: 8080");
                }
            }
            WebServer.main(new String[]{String.valueOf(port)});
            return;
        }
        
        // 原有的命令行模式
        System.out.println("=".repeat(60));
        System.out.println("欢迎使用 HTML 工具生成器");
        System.out.println("=".repeat(60));
        System.out.println("\n我可以帮你生成各种实用的HTML小工具，例如：");
        System.out.println("  - 数据处理和转换工具");
        System.out.println("  - 表格生成和编辑工具");
        System.out.println("  - 数据计算工具");
        System.out.println("  - 文本处理工具");
        System.out.println("  - 更多...");
        System.out.println("\n" + "-".repeat(60));
        
        // 自动加载配置文件（.env文件）
        // 如果配置文件不存在，会在启动时提示，但不强制配置
        HtmlGenerator generator = HtmlGenerator.getInstance();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n请输入你的需求（输入 'quit' 或 'exit' 退出，输入 'config' 配置AI，输入 'diagnose' 诊断连接）：");
            System.out.print("> ");
            
            String userInput = scanner.nextLine().trim();
            
            if (userInput.equalsIgnoreCase("quit") || 
                userInput.equalsIgnoreCase("exit") || 
                userInput.equals("退出")) {
                System.out.println("\n感谢使用！再见！");
                break;
            }
            
            if (userInput.equalsIgnoreCase("config") || 
                userInput.equals("配置")) {
                ConfigSetup.setupConfig(scanner);
                // 重新初始化生成器以加载新配置
                generator = HtmlGenerator.reloadInstance();
                continue;
            }
            
            if (userInput.equalsIgnoreCase("diagnose") || 
                userInput.equalsIgnoreCase("test") ||
                userInput.equals("诊断") || 
                userInput.equals("测试")) {
                AIConnectionDiagnostic.diagnose();
                continue;
            }
            
            if (userInput.isEmpty()) {
                continue;
            }
            
            try {
                // 生成HTML工具
                String htmlContent = generator.generateTool(userInput);
                
                // 生成文件名
                String filename = FilenameGenerator.generateFilename(userInput);
                File outputDir = new File("output");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }
                File filepath = new File(outputDir, filename);
                
                // 保存文件（使用UTF-8编码避免乱码）
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(filepath, false), StandardCharsets.UTF_8)) {
                    writer.write(htmlContent);
                }
                
                System.out.println("\n✓ 工具已生成！");
                System.out.println("文件保存位置: " + filepath.getAbsolutePath());
                System.out.println("可以直接在浏览器中打开使用");
                
                // 询问是否继续
                System.out.print("\n是否继续生成其他工具？(Y/n): ");
                String continueInput = scanner.nextLine().trim().toLowerCase();
                if (continueInput.equals("n") || continueInput.equals("no") || continueInput.equals("否")) {
                    break;
                }
                
            } catch (Exception e) {
                System.out.println("\n✗ 生成失败: " + e.getMessage());
                System.out.println("请重新输入需求或输入 'quit' 退出");
            }
        }
        
        scanner.close();
    }
}
