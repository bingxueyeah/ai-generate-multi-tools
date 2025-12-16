package aitool.service;

import com.volcengine.ark.runtime.model.responses.item.*;
import com.volcengine.ark.runtime.model.responses.request.*;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.*;
import com.volcengine.ark.runtime.service.ArkService;
import aitool.config.Config;

/**
 * AI客户端基类
 */
public abstract class AIClient {
    
    /**
     * 根据用户需求生成HTML工具
     */
    public abstract String generateHtmlTool(String userRequest, String systemPrompt) throws Exception;
    
    /**
     * 关闭客户端资源
     */
    public abstract void shutdown();
    
    /**
     * 获取客户端名称
     */
    public abstract String getClientName();
    
    /**
     * 获取默认系统提示词
     */
    protected String getDefaultSystemPrompt() {
        return "你是一个专业的HTML工具生成专家。根据用户的需求，生成一个完整、可用的HTML工具页面。\n\n" +
               "要求：\n" +
               "1. 生成完整的HTML代码，包括<!DOCTYPE html>、<head>、<body>等所有必要的标签\n" +
               "2. 使用现代化的CSS样式，界面美观、响应式设计\n" +
               "3. 包含必要的JavaScript代码实现功能\n" +
               "4. HTML应该是自包含的，可以直接在浏览器中打开使用\n" +
               "5. 代码要规范、易读，有适当的注释\n" +
               "6. 确保功能完整可用，能够直接运行\n\n" +
               "请直接输出HTML代码，不要包含任何额外的说明文字或markdown代码块标记。只返回纯HTML代码。";
    }
    
    /**
     * 从AI返回的内容中提取HTML代码
     */
    protected String extractHtml(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        content = content.trim();
        
        // 移除可能的markdown代码块标记
        if (content.contains("```html")) {
            int start = content.indexOf("```html") + 7;
            int end = content.indexOf("```", start);
            if (end != -1) {
                content = content.substring(start, end).trim();
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            if (end != -1) {
                content = content.substring(start, end).trim();
            }
        }
        
        // 确保以<!DOCTYPE开头
        if (!content.startsWith("<!DOCTYPE") && !content.startsWith("<html")) {
            int htmlStart = content.indexOf("<html");
            if (htmlStart == -1) {
                htmlStart = content.indexOf("<!DOCTYPE");
            }
            if (htmlStart != -1) {
                content = content.substring(htmlStart);
            }
        }
        
        return content.trim();
    }
}

/**
 * 豆包API客户端
 */
class DoubaoClient extends AIClient {
    
    private final String apiKey;
    private final String endpointId;
    private final String baseUrl;
    private final ArkService arkService;
    
    public DoubaoClient(String apiKey, String endpointId, String accessKey, String secretKey) {
        this(apiKey, endpointId, accessKey, secretKey, null);
    }
    
    public DoubaoClient(String apiKey, String endpointId, String accessKey, String secretKey, String customBaseUrl) {
        this.apiKey = apiKey;
        this.endpointId = endpointId;
        // 如果提供了自定义baseUrl，使用自定义的；否则使用配置的；最后使用默认值
        if (customBaseUrl != null && !customBaseUrl.isEmpty()) {
            this.baseUrl = customBaseUrl;
        } else {
            this.baseUrl = Config.get("DOUBAO_BASE_URL", "https://ark.cn-beijing.volces.com/api/v3");
        }
        
        // 验证API密钥
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("请配置API密钥");
        }
        
        // 创建ArkService实例
        this.arkService = ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }
    
    @Override
    public String generateHtmlTool(String userRequest, String systemPrompt) throws Exception {
        if (systemPrompt == null) {
            systemPrompt = getDefaultSystemPrompt();
        }
        
        try {
            return generateWithArkSdk(userRequest, systemPrompt);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "AI请求失败";
            }
            throw new Exception("AI生成失败: " + errorMsg, e);
        }
    }
    
    /**
     * 使用ARK SDK生成
     */
    private String generateWithArkSdk(String userRequest, String systemPrompt) throws Exception {
        // 打印输入提示词，便于调试
        System.out.println("========== AI 请求调试信息 ==========");
        System.out.println("系统提示词 (System Prompt):");
        System.out.println(systemPrompt != null ? systemPrompt : "(未设置)");
        System.out.println("\n用户请求 (User Request):");
        System.out.println(userRequest != null ? userRequest : "(空)");
        System.out.println("=====================================\n");
        
        // 验证endpoint ID
        String model = endpointId != null && !endpointId.isEmpty() ? endpointId : "doubao-pro-32k";
        
        // 构建请求输入
        ResponsesInput.Builder inputBuilder = ResponsesInput.builder();
        
        // 添加系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            MessageContent systemContent = MessageContent.builder()
                    .addListItem(InputContentItemText.builder().text(systemPrompt).build())
                    .build();
            ItemEasyMessage systemMessage = ItemEasyMessage.builder()
                    .role(ResponsesConstants.MESSAGE_ROLE_SYSTEM)
                    .content(systemContent)
                    .build();
            inputBuilder.addListItem(systemMessage);
        }
        
        // 添加用户消息
        MessageContent userContent = MessageContent.builder()
                .addListItem(InputContentItemText.builder().text(userRequest).build())
                .build();
        ItemEasyMessage userMessage = ItemEasyMessage.builder()
                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                .content(userContent)
                .build();
        inputBuilder.addListItem(userMessage);
        
        // 构建请求
        CreateResponsesRequest request = CreateResponsesRequest.builder()
                .model(model)
                .input(inputBuilder.build())
                .build();
        
        // 发送请求
        ResponseObject response = arkService.createResponse(request);
        ItemOutputMessage message = (ItemOutputMessage) response.getOutput().get(0);
        OutputContentItemText out  = (OutputContentItemText) message.getContent().get(0);
        return out.getText();
    }
    
    @Override
    public void shutdown() {
        if (arkService != null) {
            try {
                arkService.shutdownExecutor();
            } catch (Exception e) {
                // 忽略关闭时的异常
            }
        }
    }
    
    private String clientName = "豆包(Doubao)";
    
    /**
     * 设置客户端名称（用于区分不同的接入点）
     */
    public void setClientName(String name) {
        this.clientName = name;
    }
    
    @Override
    public String getClientName() {
        return clientName;
    }
}
