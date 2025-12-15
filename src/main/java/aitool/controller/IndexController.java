package aitool.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 首页控制器 - 提供前端页面
 */
@RestController
public class IndexController {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    
    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() {
        try {
            Resource resource = new ClassPathResource("web/index.html");
            String html = StreamUtils.copyToString(
                resource.getInputStream(), 
                StandardCharsets.UTF_8
            );
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
        } catch (IOException e) {
            logger.error("加载HTML文件失败: /web/index.html", e);
            String errorHtml = "<!DOCTYPE html><html><head><title>错误</title></head>" +
                "<body><h1>加载页面失败: " + e.getMessage() + "</h1></body></html>";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_HTML)
                .body(errorHtml);
        }
    }
}
