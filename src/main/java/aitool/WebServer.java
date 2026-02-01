package aitool;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import aitool.service.FilenameGenerator;
import aitool.service.HtmlGenerator;
import aitool.service.ToolCategoryClassifier;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static ToolCategoryClassifier categoryClassifier;
    private static File outputDir;
    
    // 自定义线程池：用于并发执行分类和HTML生成任务
    // 核心线程数：2（分类和HTML生成各一个）
    // 最大线程数：10（支持多个并发请求）
    // 线程空闲时间：60秒
    private static ExecutorService taskExecutor;
    
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
        
        // 初始化分类器
        categoryClassifier = new ToolCategoryClassifier();
        
        // 初始化自定义线程池
        TimeUnit  liveTime =  TimeUnit.MILLISECONDS;
        taskExecutor = new ThreadPoolExecutor(
                5,
                10,
                3000,
                liveTime,
                new ArrayBlockingQueue<>(300),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "AI-Task-Thread-" + threadNumber.getAndIncrement());
                        t.setDaemon(false); // 非守护线程
                        return t;
                    }
            });
        System.out.println("✓ 自定义线程池已初始化（核心线程数: 5，最大线程数: 10）");
        
        // 添加关闭钩子，确保线程池正确关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (taskExecutor != null && !taskExecutor.isShutdown()) {
                System.out.println("正在关闭线程池...");
                taskExecutor.shutdown();
                try {
                    if (!taskExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        taskExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    taskExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                System.out.println("✓ 线程池已关闭");
            }
        }));
        
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
        
        // 创建4个分类文件夹
        createCategoryFolders();
        
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
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
     * 创建4个分类文件夹
     */
    private static void createCategoryFolders() {
        ToolCategoryClassifier.ToolCategory[] categories = ToolCategoryClassifier.ToolCategory.values();
        for (ToolCategoryClassifier.ToolCategory category : categories) {
            File categoryDir = new File(outputDir, category.getChineseName());
            if (!categoryDir.exists()) {
                boolean created = categoryDir.mkdirs();
                if (created) {
                    System.out.println("✓ 已创建分类文件夹: " + category.getChineseName());
                } else {
                    System.err.println("警告: 无法创建分类文件夹: " + category.getChineseName());
                }
            }
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
                
                // 步骤1: 先检查是否是简单示例模板（优先检查，避免不必要的文件查找和AI调用）
                System.out.println("🔍 步骤1: 检查是否是简单示例模板...");
                long templateStartTime = System.currentTimeMillis();
                String simpleTemplate = generator.checkSimpleExampleTemplate(userRequest);
                long templateEndTime = System.currentTimeMillis();
                
                ToolCategoryClassifier.ToolCategory category;
                String htmlContent;
                
                if (simpleTemplate != null) {
                    // 如果是简单示例模板，直接返回
                    System.out.println("✓ 检测到简单示例需求，直接返回模板 (检查耗时: " + (templateEndTime - templateStartTime) + "ms)");
                    htmlContent = simpleTemplate;
                    
                    // 对于简单示例，需要确定其分类（用于返回正确的分类信息）
                    category = determineCategoryFromSimpleExample(userRequest);
                } else {
                    // 步骤2: 如果不是简单示例，检查是否已生成过
                    System.out.println("✓ 不是简单示例模板，检查是否已生成过...");
                    long checkStartTime = System.currentTimeMillis();
                    String existingHtml = generator.findExistingFile(userRequest);
                    long checkEndTime = System.currentTimeMillis();
                    
                    if (existingHtml != null) {
                        // 如果已存在，直接返回，跳过分类和生成
                        System.out.println("✓ 找到已生成的文件 (检查耗时: " + (checkEndTime - checkStartTime) + "ms)");
                        htmlContent = existingHtml;
                        
                        // 对于已存在的文件，需要确定其分类（用于返回正确的分类信息）
                        // 可以通过文件名或文件路径来判断分类
                        category = determineCategoryFromExistingFile(userRequest);
                    } else {
                        // 步骤3: 如果没找到，再执行分类和生成（可以并发执行）
                        System.out.println("✓ 未找到已生成的文件，开始分类和生成...");
                    long initTime = System.currentTimeMillis();
                    
                    // 创建两个并发任务（使用自定义线程池）
                    CompletableFuture<ToolCategoryClassifier.ToolCategory> categoryFuture = 
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("🔍 [线程1] 正在分析用户需求，进行分类...");
                            long cStartTime = System.currentTimeMillis();
                            try {
                                ToolCategoryClassifier.ToolCategory result = categoryClassifier.classify(userRequest);
                                long cEndTime = System.currentTimeMillis();
                                System.out.println("✓ [线程1] 分类结果: " + result.getChineseName() + " (耗时: " + (cEndTime - cStartTime) + "ms)");
                                return result;
                            } catch (Exception e) {
                                System.err.println("⚠ [线程1] 分类失败: " + e.getMessage() + "，使用默认分类");
                                return ToolCategoryClassifier.ToolCategory.PROCESSING; // 默认使用处理工具
                            }
                        }, taskExecutor);
                    
                    CompletableFuture<String> htmlFuture = 
                        CompletableFuture.supplyAsync(() -> {
                            System.out.println("🤖 [线程2] 正在生成HTML工具...");
                            long hStartTime = System.currentTimeMillis();
                            try {
                                // 跳过已存在文件的检查，因为我们已经检查过了
                                String result = generator.generateTool(userRequest, true);
                                long hEndTime = System.currentTimeMillis();
                                System.out.println("✓ [线程2] HTML生成完成 (耗时: " + (hEndTime - hStartTime) + "ms)");
                                return result;
                            } catch (Exception e) {
                                System.err.println("⚠ [线程2] HTML生成失败: " + e.getMessage());
                                throw new RuntimeException("HTML生成失败: " + e.getMessage(), e);
                            }
                        }, taskExecutor);
                    
                    // 等待两个任务都完成
                    try {
                        category = categoryFuture.get();
                        htmlContent = htmlFuture.get();
                        long endTime = System.currentTimeMillis();
                        long allTime = endTime - initTime;
                        System.out.println("✓ 并发处理完成，总耗时: " + allTime + "ms");
                    } catch (InterruptedException | ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        }
                        throw new Exception("并发处理失败: " + e.getMessage(), e);
                    }
                    }
                }
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
                
                // 根据分类结果，将文件保存到对应的分类文件夹
                File categoryDir = new File(outputDir, category.getChineseName());
                if (!categoryDir.exists()) {
                    boolean created = categoryDir.mkdirs();
                    if (!created) {
                        throw new IOException("无法创建分类文件夹: " + categoryDir.getAbsolutePath());
                    }
                }
                
                File filepath = new File(categoryDir, filename);
                
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
                response.addProperty("category", category.getChineseName());
                response.addProperty("categoryEn", category.getEnglishName());
                
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
                sendResponse(exchange, 400, "text/plain; charset=utf-8", "Missing file parameter");
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
                sendResponse(exchange, 400, "text/plain; charset=utf-8", "Missing file parameter");
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
            
            // 在所有分类文件夹下递归查找文件
            File file = findFileInAllCategories(filename);
            if (file == null || !file.exists() || !file.isFile()) {
                // 使用UTF-8编码错误信息，避免乱码
                String errorMsg = "File not found: " + filename;
                sendResponse(exchange, 404, "text/plain; charset=utf-8", errorMsg);
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
        
        /**
         * 在所有分类文件夹下递归查找文件
         */
        private File findFileInAllCategories(String filename) {
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists() || !outputDir.isDirectory()) {
                return null;
            }
            
            // 在所有子目录（包括分类文件夹）中查找文件
            return findFileRecursively(outputDir, filename);
        }
        
        /**
         * 递归查找文件
         */
        private File findFileRecursively(File directory, String filename) {
            if (!directory.exists() || !directory.isDirectory()) {
                return null;
            }
            
            File[] items = directory.listFiles();
            if (items == null) {
                return null;
            }
            
            // 先在当前目录查找
            for (File item : items) {
                if (item.isFile() && item.getName().equals(filename)) {
                    return item;
                }
            }
            
            // 递归在子目录中查找
            for (File item : items) {
                if (item.isDirectory()) {
                    File found = findFileRecursively(item, filename);
                    if (found != null) {
                        return found;
                    }
                }
            }
            
            return null;
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
    
    /**
     * 从简单示例中确定分类
     */
    private static ToolCategoryClassifier.ToolCategory determineCategoryFromSimpleExample(String userRequest) {
        String requestLower = userRequest.trim().toLowerCase();
        
        // 根据简单示例类型确定分类
        if (requestLower.contains("计算器") || requestLower.contains("calculator")) {
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        } else if (requestLower.contains("表格") || requestLower.contains("table")) {
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        } else if (requestLower.contains("文本替换") || requestLower.contains("replace")) {
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        } else if (requestLower.contains("json") || requestLower.contains("格式化")) {
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        } else if (requestLower.contains("数据转换") || requestLower.contains("data converter")) {
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        }
        
        // 默认使用处理工具分类
        return ToolCategoryClassifier.ToolCategory.PROCESSING;
    }
    
    /**
     * 从已存在的文件中确定分类
     * 通过查找文件所在的分类文件夹来判断
     */
    private static ToolCategoryClassifier.ToolCategory determineCategoryFromExistingFile(String userRequest) {
        // 尝试在所有分类文件夹中查找匹配的文件
        ToolCategoryClassifier.ToolCategory[] categories = ToolCategoryClassifier.ToolCategory.values();
        for (ToolCategoryClassifier.ToolCategory category : categories) {
            File categoryDir = new File(outputDir, category.getChineseName());
            if (categoryDir.exists() && categoryDir.isDirectory()) {
                // 生成可能的文件名
                String generatedFilename = FilenameGenerator.generateFilename(userRequest);
                // 确保文件名以.html结尾（创建final变量供lambda使用）
                final String filename = generatedFilename.toLowerCase().endsWith(".html") 
                    ? generatedFilename 
                    : generatedFilename + ".html";
                
                // 检查该分类文件夹下是否有匹配的文件
                File[] files = categoryDir.listFiles((dir, name) -> {
                    // 检查文件名是否匹配（忽略时间戳）
                    String cleanName = name.replaceAll("_\\d{8}_\\d{6}\\.html$", "").replaceAll("\\.html$", "");
                    String cleanFilename = filename.replaceAll("_\\d{8}_\\d{6}\\.html$", "").replaceAll("\\.html$", "");
                    return cleanName.equals(cleanFilename) || name.equals(filename);
                });
                
                if (files != null && files.length > 0) {
                    return category;
                }
            }
        }
        
        // 如果找不到，使用规则匹配作为备用
        try {
            return categoryClassifier.classify(userRequest);
        } catch (Exception e) {
            // 如果分类也失败，返回默认分类
            return ToolCategoryClassifier.ToolCategory.PROCESSING;
        }
    }
}
