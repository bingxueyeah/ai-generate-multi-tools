package aitool.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI容灾管理器
 * 支持多个AI客户端，当某个客户端失败时自动切换到下一个
 */
public class AIFailoverManager {
    
    private final List<AIClient> clients;
    private final AtomicInteger currentIndex;
    private final String clientNames;
    
    /**
     * 创建容灾管理器
     * @param clients AI客户端列表（按优先级排序）
     */
    public AIFailoverManager(List<AIClient> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalArgumentException("AI客户端列表不能为空");
        }
        this.clients = new ArrayList<>(clients);
        this.currentIndex = new AtomicInteger(0);
        
        // 构建客户端名称列表（用于日志）
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            if (i > 0) {
                names.append(" -> ");
            }
            names.append(clients.get(i).getClientName());
        }
        this.clientNames = names.toString();
        
        System.out.println("✓ AI容灾管理器已初始化，客户端优先级: " + clientNames);
    }
    
    /**
     * 根据用户需求生成HTML工具（带容灾机制）
     * @param userRequest 用户请求
     * @param systemPrompt 系统提示词（可选）
     * @return 生成的HTML内容
     * @throws Exception 所有客户端都失败时抛出异常
     */
    public String generateHtmlTool(String userRequest, String systemPrompt) throws Exception {
        List<Exception> errors = new ArrayList<>();
        int startIndex = currentIndex.get();
        int attempts = 0;
        
        // 尝试所有客户端（从当前索引开始，循环一圈）
        while (attempts < clients.size()) {
            int index = (startIndex + attempts) % clients.size();
            AIClient client = clients.get(index);
            
            try {
                System.out.println("🔄 尝试使用 " + client.getClientName() + " 生成内容...");
                String result = client.generateHtmlTool(userRequest, systemPrompt);
                
                // 验证结果
                if (result != null && result.length() > 100) {
                    if (result.contains("<!DOCTYPE") || result.contains("<html")) {
                        System.out.println("✓ " + client.getClientName() + " 生成成功！");
                        // 更新当前索引，下次优先使用成功的客户端
                        currentIndex.set(index);
                        return result;
                    } else {
                        throw new Exception("生成的内容格式不正确，缺少必要的HTML标签");
                    }
                } else {
                    throw new Exception("生成的内容过短，可能生成失败");
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                System.out.println("⚠ " + client.getClientName() + " 调用失败: " + errorMsg);
                
                // 分析失败原因
                String reason = analyzeFailureReason(errorMsg, e);
                System.out.println("   失败原因: " + reason);
                
                errors.add(new Exception(client.getClientName() + ": " + errorMsg, e));
                
                // 如果是临时性错误（如网络问题），继续尝试下一个
                // 如果是永久性错误（如认证失败、配额不足），也继续尝试下一个（可能其他服务可用）
                attempts++;
                
                if (attempts < clients.size()) {
                    System.out.println("🔄 自动切换到下一个AI服务...");
                }
            }
        }
        
        // 所有客户端都失败了
        StringBuilder errorSummary = new StringBuilder();
        errorSummary.append("所有AI服务调用均失败。已尝试的客户端: ").append(clientNames).append("\n");
        errorSummary.append("失败详情:\n");
        for (int i = 0; i < errors.size(); i++) {
            errorSummary.append("  ").append(i + 1).append(". ").append(errors.get(i).getMessage()).append("\n");
        }
        
        throw new Exception(errorSummary.toString());
    }
    
    /**
     * 分析失败原因
     */
    private String analyzeFailureReason(String errorMsg, Exception e) {
        if (errorMsg == null) {
            errorMsg = "";
        }
        String lowerMsg = errorMsg.toLowerCase();
        
        // 连接相关错误
        if (lowerMsg.contains("连接") || lowerMsg.contains("connect") || 
            lowerMsg.contains("timeout") || lowerMsg.contains("超时") ||
            e instanceof java.net.ConnectException || 
            e instanceof java.net.SocketTimeoutException) {
            return "连接失败或超时";
        }
        
        // 认证相关错误
        if (lowerMsg.contains("认证") || lowerMsg.contains("401") || 
            lowerMsg.contains("unauthorized") || lowerMsg.contains("invalid") ||
            lowerMsg.contains("api key") || lowerMsg.contains("密钥")) {
            return "认证失败：API密钥无效或已过期";
        }
        
        // 配额/频率限制错误
        if (lowerMsg.contains("429") || lowerMsg.contains("quota") || 
            lowerMsg.contains("配额") || lowerMsg.contains("limit") ||
            lowerMsg.contains("rate limit") || lowerMsg.contains("频率限制")) {
            return "配额不足或请求频率超限";
        }
        
        // 服务不可用
        if (lowerMsg.contains("503") || lowerMsg.contains("500") ||
            lowerMsg.contains("service unavailable") || lowerMsg.contains("服务不可用")) {
            return "服务暂时不可用";
        }
        
        // 欠费相关
        if (lowerMsg.contains("payment") || lowerMsg.contains("billing") ||
            lowerMsg.contains("欠费") || lowerMsg.contains("余额不足")) {
            return "账户欠费或余额不足";
        }
        
        return "未知错误";
    }
    
    /**
     * 获取当前使用的客户端索引
     */
    public int getCurrentIndex() {
        return currentIndex.get();
    }
    
    /**
     * 获取客户端数量
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * 获取所有客户端名称
     */
    public String getClientNames() {
        return clientNames;
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        for (AIClient client : clients) {
            try {
                client.shutdown();
            } catch (Exception e) {
                // 忽略关闭时的异常
            }
        }
    }
}
