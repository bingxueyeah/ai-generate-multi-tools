package aitool.config;

import aitool.service.HtmlGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Web配置类
 */
@Configuration
public class WebConfig {
    
    @Value("${app.output.dir:output}")
    private String outputDirPath;
    
    private File outputDir;
    
    @PostConstruct
    public void init() {
        // 确保输出目录存在（使用绝对路径）
        String projectRoot = System.getProperty("user.dir");
        outputDir = new File(projectRoot, outputDirPath);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                System.err.println("警告: 无法创建输出目录: " + outputDir.getAbsolutePath());
            }
        }
        System.out.println("输出目录: " + outputDir.getAbsolutePath());
    }
    
    @Bean
    public HtmlGenerator htmlGenerator() {
        return HtmlGenerator.getInstance();
    }
    
    public File getOutputDir() {
        return outputDir;
    }
}
