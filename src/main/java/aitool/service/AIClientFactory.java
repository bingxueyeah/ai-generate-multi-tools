package aitool.service;

import aitool.config.Config;

/**
 * AI客户端工厂类
 */
public class AIClientFactory {
    
    /**
     * 创建AI客户端实例
     */
    public static AIClient createAIClient() {
        try {
            String apiKey = Config.get("DOUBAO_API_KEY");
            String endpointId = Config.get("DOUBAO_ENDPOINT_ID");
            
            if (apiKey != null && !apiKey.isEmpty()) {
                return new DoubaoClient(apiKey, endpointId, null, null);
            } else {
                System.err.println("警告: DOUBAO_API_KEY未配置，无法初始化AI客户端");
            }
        } catch (Exception e) {
            System.err.println("警告: 无法初始化AI客户端: " + e.getMessage());
        }
        
        return null;
    }
}
