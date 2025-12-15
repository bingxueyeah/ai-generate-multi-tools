package aitool.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import aitool.config.Config;
import aitool.model.CheckResult;
import aitool.model.DiagnosticResult;

/**
 * AI连接诊断工具
 * 用于诊断是认证失败还是连接失败
 */
public class AIConnectionDiagnostic {
    
    private static final int TEST_TIMEOUT = 10; // 测试超时时间（秒）
    
    /**
     * 执行完整的连接诊断
     */
    public static DiagnosticResult diagnose() {
        DiagnosticResult result = new DiagnosticResult();
        
        System.out.println("=".repeat(60));
        System.out.println("AI连接诊断工具");
        System.out.println("=".repeat(60));
        System.out.println();
        
        // 1. 检查配置
        System.out.println("[1/4] 检查配置...");
        result.configCheck = checkConfig();
        printResult("配置检查", result.configCheck);
        System.out.println();
        
        if (!result.configCheck.success) {
            result.overallStatus = "配置不完整";
            result.summary = "请先完成配置后再进行诊断";
            printSummary(result);
            return result;
        }
        
        // 2. 测试网络连接
        System.out.println("[2/4] 测试网络连接...");
        result.networkCheck = testNetworkConnection();
        printResult("网络连接", result.networkCheck);
        System.out.println();
        
        if (!result.networkCheck.success) {
            result.overallStatus = "连接失败";
            result.summary = "无法连接到API服务器，请检查网络连接";
            printSummary(result);
            return result;
        }
        
        // 3. 测试API端点可达性
        System.out.println("[3/4] 测试API端点可达性...");
        result.endpointCheck = testEndpointReachability();
        printResult("API端点", result.endpointCheck);
        System.out.println();
        
        if (!result.endpointCheck.success) {
            result.overallStatus = "端点不可达";
            result.summary = "API端点无法访问，请检查DOUBAO_BASE_URL配置";
            printSummary(result);
            return result;
        }
        
        // 4. 测试API认证
        System.out.println("[4/4] 测试API认证...");
        result.authCheck = testAuthentication();
        printResult("API认证", result.authCheck);
        System.out.println();
        
        // 总结
        if (result.authCheck.success) {
            result.overallStatus = "连接正常";
            result.summary = "所有检查通过，AI连接正常";
        } else {
            result.overallStatus = "认证失败";
            result.summary = "网络连接正常，但API认证失败，请检查API密钥";
        }
        
        printSummary(result);
        return result;
    }
    
    /**
     * 检查配置
     */
    private static CheckResult checkConfig() {
        CheckResult result = new CheckResult();
        
        String apiKey = Config.get("DOUBAO_API_KEY");
        String endpointId = Config.get("DOUBAO_ENDPOINT_ID");
        String accessKey = Config.get("VOLC_ACCESSKEY");
        String secretKey = Config.get("VOLC_SECRETKEY");
        String baseUrl = Config.get("DOUBAO_BASE_URL");
        
        if (apiKey == null || apiKey.isEmpty()) {
            if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
                result.success = false;
                result.message = "未配置API密钥（需要DOUBAO_API_KEY或VOLC_ACCESSKEY+VOLC_SECRETKEY）";
                return result;
            }
        }
        
        if (baseUrl == null || baseUrl.isEmpty()) {
            result.success = false;
            result.message = "未配置API基础URL（DOUBAO_BASE_URL）";
            return result;
        }
        
        result.success = true;
        result.message = String.format("配置完整 - API地址: %s", baseUrl);
        if (apiKey != null && !apiKey.isEmpty()) {
            result.message += String.format(", API密钥: %s***", apiKey.substring(0, Math.min(8, apiKey.length())));
        }
        return result;
    }
    
    /**
     * 测试网络连接（DNS解析和基本连接）
     */
    private static CheckResult testNetworkConnection() {
        CheckResult result = new CheckResult();
        
        String baseUrl = Config.get("DOUBAO_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3");
        String host;
        
        try {
            java.net.URL url = new java.net.URL(baseUrl);
            host = url.getHost();
        } catch (Exception e) {
            result.success = false;
            result.message = "无法解析API地址: " + e.getMessage();
            return result;
        }
        
        // 测试DNS解析
        try {
            java.net.InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            result.success = false;
            result.message = "DNS解析失败，无法解析主机名: " + host;
            return result;
        } catch (Exception e) {
            result.success = false;
            result.message = "网络连接测试失败: " + e.getMessage();
            return result;
        }
        
        result.success = true;
        result.message = String.format("网络连接正常 - 主机: %s", host);
        return result;
    }
    
    /**
     * 测试API端点可达性（不包含认证）
     */
    private static CheckResult testEndpointReachability() {
        CheckResult result = new CheckResult();
        
        String baseUrl = Config.get("DOUBAO_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3");
        String url = baseUrl + "/chat/completions";
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
            .build();
        
        // 发送一个简单的请求（不带认证，预期会返回401或403，但能证明端点可达）
        Request request = new Request.Builder()
            .url(url)
            .get()  // 使用GET方法，即使不支持也会返回错误，证明端点可达
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            // 如果能收到响应（即使是错误），说明端点可达
            if (code == 401 || code == 403 || code == 404 || code == 405) {
                result.success = true;
                result.message = String.format("API端点可达 - HTTP状态码: %d", code);
            } else if (code >= 200 && code < 500) {
                result.success = true;
                result.message = String.format("API端点可达 - HTTP状态码: %d", code);
            } else {
                result.success = false;
                result.message = String.format("API端点响应异常 - HTTP状态码: %d", code);
            }
        } catch (SocketTimeoutException e) {
            result.success = false;
            result.message = "连接超时：无法在" + TEST_TIMEOUT + "秒内连接到API服务器";
        } catch (ConnectException e) {
            result.success = false;
            result.message = "连接被拒绝：无法连接到API服务器，请检查网络和防火墙设置";
        } catch (UnknownHostException e) {
            result.success = false;
            result.message = "无法解析主机名：请检查DOUBAO_BASE_URL配置";
        } catch (IOException e) {
            result.success = false;
            result.message = "网络错误: " + e.getMessage();
        } catch (Exception e) {
            result.success = false;
            result.message = "未知错误: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * 测试API认证
     */
    private static CheckResult testAuthentication() {
        CheckResult result = new CheckResult();
        
        String apiKey = Config.get("DOUBAO_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            result.success = false;
            result.message = "未配置API密钥（DOUBAO_API_KEY）";
            return result;
        }
        
        String baseUrl = Config.get("DOUBAO_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3");
        String url = baseUrl + "/chat/completions";
        
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TEST_TIMEOUT, TimeUnit.SECONDS)
            .build();
        
        // 构建一个最小的测试请求
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", Config.get("DOUBAO_ENDPOINT_ID", "doubao-pro-32k"));
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "test");
        messages.add(message);
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 10);
        
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (code == 200) {
                result.success = true;
                result.message = "API认证成功";
            } else if (code == 401) {
                result.success = false;
                result.message = "认证失败：API密钥无效或已过期（HTTP 401）";
                if (responseBody.contains("error")) {
                    try {
                        JsonObject json = new Gson().fromJson(responseBody, JsonObject.class);
                        if (json.has("error") && json.getAsJsonObject("error").has("message")) {
                            result.message += " - " + json.getAsJsonObject("error").get("message").getAsString();
                        }
                    } catch (Exception e) {
                        // 忽略JSON解析错误
                    }
                }
            } else if (code == 403) {
                result.success = false;
                result.message = "访问被拒绝：API密钥权限不足或endpoint配置错误（HTTP 403）";
            } else if (code == 404) {
                result.success = false;
                result.message = "端点不存在：请检查DOUBAO_BASE_URL和DOUBAO_ENDPOINT_ID配置（HTTP 404）";
            } else if (code == 429) {
                result.success = false;
                result.message = "请求频率过高：请稍后重试（HTTP 429）";
            } else {
                result.success = false;
                result.message = String.format("API请求失败 - HTTP状态码: %d, 响应: %s", code, 
                    responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
            }
        } catch (SocketTimeoutException e) {
            result.success = false;
            result.message = "请求超时：API服务器响应时间过长";
        } catch (ConnectException e) {
            result.success = false;
            result.message = "连接失败：无法连接到API服务器";
        } catch (IOException e) {
            result.success = false;
            result.message = "网络错误: " + e.getMessage();
        } catch (Exception e) {
            result.success = false;
            result.message = "未知错误: " + e.getMessage();
        }
        
        return result;
    }
    
    private static void printResult(String name, CheckResult result) {
        if (result.success) {
            System.out.println("  ✓ " + name + ": " + result.message);
        } else {
            System.out.println("  ✗ " + name + ": " + result.message);
        }
    }
    
    private static void printSummary(DiagnosticResult result) {
        System.out.println("=".repeat(60));
        System.out.println("诊断总结");
        System.out.println("=".repeat(60));
        System.out.println("状态: " + result.overallStatus);
        System.out.println("说明: " + result.summary);
        System.out.println();
        
        if ("认证失败".equals(result.overallStatus)) {
            System.out.println("建议解决方案：");
            System.out.println("  1. 检查.env文件中的DOUBAO_API_KEY是否正确");
            System.out.println("  2. 确认API密钥没有多余的空格或引号");
            System.out.println("  3. 验证API密钥是否有效且未过期");
            System.out.println("  4. 检查DOUBAO_ENDPOINT_ID配置是否正确");
        } else if ("连接失败".equals(result.overallStatus) || "端点不可达".equals(result.overallStatus)) {
            System.out.println("建议解决方案：");
            System.out.println("  1. 检查网络连接是否正常");
            System.out.println("  2. 检查防火墙设置，确保可以访问API服务器");
            System.out.println("  3. 验证DOUBAO_BASE_URL配置是否正确");
            System.out.println("  4. 如果使用代理，请配置代理设置");
        } else if ("配置不完整".equals(result.overallStatus)) {
            System.out.println("建议解决方案：");
            System.out.println("  1. 在项目根目录创建.env文件");
            System.out.println("  2. 配置DOUBAO_API_KEY和DOUBAO_ENDPOINT_ID");
            System.out.println("  3. 参考env.example文件了解配置格式");
        }
        System.out.println("=".repeat(60));
    }
    
    /**
     * 主方法，用于直接运行诊断
     */
    public static void main(String[] args) {
        diagnose();
    }
}
