package aitool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Web应用主类
 */
@SpringBootApplication
public class WebApplication {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("HTML工具生成器 - Web服务器");
        System.out.println("=".repeat(60));
        
        SpringApplication.run(WebApplication.class, args);
        
        System.out.println("\n✓ 服务器已启动！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("\n按 Ctrl+C 停止服务器");
        System.out.println("=".repeat(60));
    }
}
