package aitool.controller;

import aitool.config.WebConfig;
import aitool.service.FilenameGenerator;
import aitool.service.HtmlGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web控制器 - 提供前端界面和API接口
 */
@RestController
@CrossOrigin(origins = "*")
public class WebController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
    private static final Gson gson = new Gson();
    
    @Autowired
    private HtmlGenerator htmlGenerator;
    
    @Autowired
    private WebConfig webConfig;
    
    /**
     * 生成工具API接口
     */
    @PostMapping("/api/generate")
    public ResponseEntity<String> generate(@RequestBody JsonObject request) {
        try {
            String userRequest = request.get("request").getAsString();
            
            if (userRequest == null || userRequest.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gson.toJson(createErrorResponse("请求不能为空")));
            }
            
            // 生成HTML工具
            String htmlContent = htmlGenerator.generateTool(userRequest);
            
            // 确保HTML内容有效
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(gson.toJson(createErrorResponse("生成的HTML内容为空")));
            }
            
            // 生成文件名（确保是.html格式）
            String filename = FilenameGenerator.generateFilename(userRequest);
            // 确保文件名以.html结尾
            if (!filename.toLowerCase().endsWith(".html")) {
                if (filename.endsWith(".")) {
                    filename = filename.substring(0, filename.length() - 1) + ".html";
                } else {
                    filename = filename + ".html";
                }
            }
            
            // 确保输出目录存在
            File outputDir = webConfig.getOutputDir();
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    throw new IOException("无法创建输出目录: " + outputDir.getAbsolutePath());
                }
            }
            
            File filepath = new File(outputDir, filename);
            
            // 保存文件（使用UTF-8编码避免乱码）
            Files.write(filepath.toPath(), htmlContent.getBytes(StandardCharsets.UTF_8));
            
            // 验证文件是否真的被保存了
            if (!filepath.exists() || filepath.length() == 0) {
                throw new IOException("文件保存失败: " + filepath.getAbsolutePath());
            }
            
            System.out.println("文件已保存: " + filepath.getAbsolutePath() + " (大小: " + filepath.length() + " 字节)");
            
            // 返回成功响应
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("filename", filename);
            response.addProperty("filepath", filepath.getAbsolutePath());
            response.addProperty("htmlContent", htmlContent);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(gson.toJson(response));
                
        } catch (Exception e) {
            logger.error("生成工具失败", e);
            JsonObject response = createErrorResponse("生成失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(gson.toJson(response));
        }
    }
    
    /**
     * 文件下载接口
     */
    @GetMapping("/api/download")
    public ResponseEntity<Resource> download(@RequestParam String file) {
        try {
            // URL解码文件名
            String filename = java.net.URLDecoder.decode(file, StandardCharsets.UTF_8);
            
            // 确保文件名以.html结尾
            if (!filename.toLowerCase().endsWith(".html")) {
                if (filename.endsWith(".")) {
                    filename = filename.substring(0, filename.length() - 1) + ".html";
                } else {
                    filename = filename + ".html";
                }
            }
            
            File outputDir = webConfig.getOutputDir();
            File fileToDownload = new File(outputDir, filename);
            
            if (!fileToDownload.exists() || !fileToDownload.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(fileToDownload);
            
            // 对文件名进行RFC 5987编码（支持中文文件名）
            StringBuilder encodedFilename = new StringBuilder();
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            for (byte b : filenameBytes) {
                if ((b >= 0x30 && b <= 0x39) || // 0-9
                    (b >= 0x41 && b <= 0x5A) || // A-Z
                    (b >= 0x61 && b <= 0x7A) || // a-z
                    b == 0x21 || b == 0x23 || b == 0x24 || b == 0x26 || // ! # $ &
                    b == 0x2B || b == 0x2D || b == 0x2E || // + - .
                    b == 0x5E || b == 0x5F || b == 0x60 || // ^ _ `
                    b == 0x7C || b == 0x7E) { // | ~
                    encodedFilename.append((char) b);
                } else {
                    encodedFilename.append('%');
                    encodedFilename.append(String.format("%02X", b & 0xFF));
                }
            }
            
            // 设置响应头
            String asciiFilename = filename.replaceAll("[^\\x20-\\x7E]", "_");
            asciiFilename = asciiFilename.replace("\\", "\\\\").replace("\"", "\\\"");
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + asciiFilename + "\"; " +
                    "filename*=UTF-8''" + encodedFilename)
                .body(resource);
                
        } catch (Exception e) {
            logger.error("文件下载失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取生成的文件列表
     */
    @GetMapping("/api/files")
    public ResponseEntity<List<String>> getFiles() {
        try {
            File outputDir = webConfig.getOutputDir();
            if (!outputDir.exists()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            File[] files = outputDir.listFiles((dir, name) -> name.endsWith(".html"));
            List<String> fileList = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fileList);
        } catch (Exception e) {
            logger.error("获取文件列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 创建错误响应
     */
    private JsonObject createErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        return response;
    }
}
