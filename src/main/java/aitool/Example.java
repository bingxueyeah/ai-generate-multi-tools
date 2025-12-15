package aitool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import aitool.service.FilenameGenerator;
import aitool.service.HtmlGenerator;

/**
 * 使用示例：展示如何通过代码直接生成HTML工具
 */
public class Example {
    
    public static void exampleUsage() {
        // 确保输出目录存在
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // 示例需求列表
        String[] examples = {
            "生成一个表格工具",
            "我需要一个计算器",
            "创建一个文本替换工具",
            "JSON格式化工具",
            "CSV数据处理工具",
            "数据格式转换工具"
        };
        
        System.out.println("=".repeat(60));
        System.out.println("HTML工具生成器 - 使用示例");
        System.out.println("=".repeat(60));
        System.out.println();
        
        HtmlGenerator generator = HtmlGenerator.getInstance();
        
        for (int i = 0; i < examples.length; i++) {
            String request = examples[i];
            System.out.println("[" + (i + 1) + "/" + examples.length + "] 生成工具: " + request);
            
            try {
                // 生成HTML
                String htmlContent = generator.generateTool(request);
                
                // 生成文件名
                String filename = FilenameGenerator.generateFilename(request);
                File filepath = new File(outputDir, filename);
                
                // 保存文件（使用UTF-8编码避免乱码）
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(filepath, false), StandardCharsets.UTF_8)) {
                    writer.write(htmlContent);
                }
                
                System.out.println("  ✓ 已保存到: " + filepath.getAbsolutePath());
                
            } catch (Exception e) {
                System.out.println("  ✗ 生成失败: " + e.getMessage());
            }
            
            System.out.println();
        }
        
        System.out.println("=".repeat(60));
        System.out.println("所有示例工具已生成完成！");
        System.out.println("请查看 output 目录中的 HTML 文件");
        System.out.println("=".repeat(60));
    }
    
    public static void main(String[] args) {
        exampleUsage();
    }
}
