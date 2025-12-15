package aitool;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import aitool.service.FilenameGenerator;
import aitool.service.HtmlGenerator;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * Web服务器 - 提供前端界面和API接口
 */
public class WebServer {
    
    private static final int DEFAULT_PORT = 8080;
    private static final String OUTPUT_DIR = "output";
    private static final Gson gson = new Gson();
    private static HtmlGenerator generator;
    private static File outputDir;
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("无效的端口号，使用默认端口: " + DEFAULT_PORT);
            }
        }
        
        System.out.println("=".repeat(60));
        System.out.println("HTML工具生成器 - Web服务器");
        System.out.println("=".repeat(60));
        
        // 初始化生成器
        generator = HtmlGenerator.getInstance();
        
        // 确保输出目录存在（使用绝对路径）
        String projectRoot = System.getProperty("user.dir");
        outputDir = new File(projectRoot, OUTPUT_DIR);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                System.err.println("警告: 无法创建输出目录: " + outputDir.getAbsolutePath());
            }
        }
        System.out.println("输出目录: " + outputDir.getAbsolutePath());
        
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // 设置线程池
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            // 静态文件服务 - 前端页面
            server.createContext("/", new StaticFileHandler());
            
            // API接口 - 生成工具
            server.createContext("/api/generate", new GenerateHandler());
            
            // API接口 - 下载文件
            server.createContext("/api/download", new DownloadHandler());
            
            // API接口 - 获取生成的文件列表
            server.createContext("/api/files", new FileListHandler());
            
            server.start();
            
            System.out.println("\n✓ 服务器已启动！");
            System.out.println("访问地址: http://localhost:" + port);
            System.out.println("\n按 Ctrl+C 停止服务器");
            System.out.println("=".repeat(60));
            
        } catch (IOException e) {
            System.err.println("启动服务器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 静态文件处理器 - 提供前端页面
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/") || path.equals("/index.html")) {
                // 返回前端页面
                String html = getIndexHtml();
                sendResponse(exchange, 200, "text/html; charset=utf-8", html);
            } else {
                // 404
                sendResponse(exchange, 404, "text/plain", "Not Found");
            }
        }
        
        private String getIndexHtml() {
            return loadHtmlFromResource("/web/index.html");
        }
        
        /**
         * 从资源文件加载HTML内容
         */
        private String loadHtmlFromResource(String resourcePath) {
            try (InputStream is = WebServer.class.getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.err.println("警告: 无法加载HTML文件: " + resourcePath);
                    return "<!DOCTYPE html><html><head><title>错误</title></head><body><h1>无法加载页面</h1></body></html>";
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
                System.err.println("加载HTML文件失败: " + resourcePath);
                e.printStackTrace();
                return "<!DOCTYPE html><html><head><title>错误</title></head><body><h1>加载页面失败: " + e.getMessage() + "</h1></body></html>";
            }
        }
        
    }
    
    /**
     * 生成工具API处理器
     */
    static class GenerateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", 
                    gson.toJson(createErrorResponse("Method not allowed")));
                return;
            }
            
            try {
                // 读取请求体
                String requestBody = readRequestBody(exchange);
                JsonObject json = gson.fromJson(requestBody, JsonObject.class);
                String userRequest = json.get("request").getAsString();
                
                if (userRequest == null || userRequest.trim().isEmpty()) {
                    sendResponse(exchange, 400, "application/json", 
                        gson.toJson(createErrorResponse("请求不能为空")));
                    return;
                }
                
                // 生成HTML工具
                String htmlContent = generator.generateTool(userRequest);
                
                // 确保HTML内容有效
                if (htmlContent == null || htmlContent.trim().isEmpty()) {
                    sendResponse(exchange, 500, "application/json", 
                        gson.toJson(createErrorResponse("生成的HTML内容为空")));
                    return;
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
                if (!outputDir.exists()) {
                    boolean created = outputDir.mkdirs();
                    if (!created) {
                        throw new IOException("无法创建输出目录: " + outputDir.getAbsolutePath());
                    }
                }
                
                File filepath = new File(outputDir, filename);
                
                // 保存文件（使用UTF-8编码避免乱码）
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(filepath, false), StandardCharsets.UTF_8)) {
                    writer.write(htmlContent);
                    writer.flush();
                }
                
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
                
                sendResponse(exchange, 200, "application/json; charset=utf-8", 
                    gson.toJson(response));
                
            } catch (Exception e) {
                e.printStackTrace();
                JsonObject response = createErrorResponse("生成失败: " + e.getMessage());
                sendResponse(exchange, 500, "application/json; charset=utf-8", 
                    gson.toJson(response));
            }
        }
    }
    
    /**
     * 文件下载处理器
     */
    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query == null) {
                sendResponse(exchange, 400, "text/plain", "Missing file parameter");
                return;
            }
            
            // 正确解析查询参数
            String filename = null;
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("file=")) {
                    filename = param.substring(5); // 跳过 "file="
                    break;
                }
            }
            
            if (filename == null || filename.isEmpty()) {
                sendResponse(exchange, 400, "text/plain", "Missing file parameter");
                return;
            }
            
            // URL解码文件名
            filename = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8);
            
            // 确保文件名以.html结尾
            if (!filename.toLowerCase().endsWith(".html")) {
                if (filename.endsWith(".")) {
                    filename = filename.substring(0, filename.length() - 1) + ".html";
                } else {
                    filename = filename + ".html";
                }
            }
            
            File file = new File(OUTPUT_DIR, filename);
            if (!file.exists() || !file.isFile()) {
                sendResponse(exchange, 404, "text/plain", "File not found: " + filename);
                return;
            }
            
            // 对文件名进行RFC 5987编码（支持中文文件名）
            // RFC 5987要求：filename*=charset'lang'value，其中value是百分号编码
            StringBuilder encodedFilename = new StringBuilder();
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            for (byte b : filenameBytes) {
                // RFC 5987允许的字符：字母、数字、!、#、$、&、+、-、.、^、_、`、|、~
                // 其他字符需要百分号编码
                if ((b >= 0x30 && b <= 0x39) || // 0-9
                    (b >= 0x41 && b <= 0x5A) || // A-Z
                    (b >= 0x61 && b <= 0x7A) || // a-z
                    b == 0x21 || b == 0x23 || b == 0x24 || b == 0x26 || // ! # $ &
                    b == 0x2B || b == 0x2D || b == 0x2E || // + - .
                    b == 0x5E || b == 0x5F || b == 0x60 || // ^ _ `
                    b == 0x7C || b == 0x7E) { // | ~
                    encodedFilename.append((char) b);
                } else {
                    // 需要编码的字符
                    encodedFilename.append('%');
                    encodedFilename.append(String.format("%02X", b & 0xFF));
                }
            }
            
            // 设置响应头
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            // 使用RFC 5987格式，同时提供filename和filename*以兼容不同浏览器
            // filename用于兼容旧浏览器（将非ASCII字符替换为下划线）
            String asciiFilename = filename.replaceAll("[^\\x20-\\x7E]", "_");
            // 转义双引号和反斜杠
            asciiFilename = asciiFilename.replace("\\", "\\\\").replace("\"", "\\\"");
            exchange.getResponseHeaders().set("Content-Disposition", 
                "attachment; filename=\"" + asciiFilename + "\"; " +
                "filename*=UTF-8''" + encodedFilename.toString());
            
            // 发送文件内容
            byte[] fileContent = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, fileContent.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
        }
    }
    
    /**
     * 文件列表处理器
     */
    static class FileListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                sendResponse(exchange, 200, "application/json; charset=utf-8", 
                    gson.toJson(new java.util.ArrayList<>()));
                return;
            }
            
            File[] files = outputDir.listFiles((dir, name) -> name.endsWith(".html"));
            java.util.List<String> fileList = new java.util.ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
            }
            
            sendResponse(exchange, 200, "application/json; charset=utf-8", 
                gson.toJson(fileList));
        }
    }
    
    /**
     * 读取请求体
     */
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    /**
     * 发送响应
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, 
                                     String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    /**
     * 创建错误响应
     */
    private static JsonObject createErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        return response;
    }
}
