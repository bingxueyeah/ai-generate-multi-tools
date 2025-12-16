package aitool.service;

import aitool.config.Config;
import java.util.ArrayList;
import java.util.List;

/**
 * AI客户端工厂类
 * 支持创建多个AI客户端并配置容灾机制
 */
public class AIClientFactory {
    
    /**
     * 创建AI客户端实例（单个，兼容旧代码）
     * @deprecated 建议使用 createFailoverManager() 以获得容灾支持
     */
    @Deprecated
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
    
    /**
     * 创建AI容灾管理器
     * 根据配置自动创建多个AI客户端，支持自动切换
     * @return AI容灾管理器，如果没有任何可用的客户端则返回null
     */
    public static AIFailoverManager createFailoverManager() {
        List<AIClient> clients = new ArrayList<>();
        
        // 1. 尝试创建第一个豆包客户端（优先级1）
        try {
            String doubaoApiKey = Config.get("DOUBAO_API_KEY");
            String doubaoEndpointId = Config.get("DOUBAO_ENDPOINT_ID");
            String doubaoBaseUrl = Config.get("DOUBAO_BASE_URL");
            
            if (doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
                DoubaoClient doubaoClient = new DoubaoClient(doubaoApiKey, doubaoEndpointId, null, null, doubaoBaseUrl);
                doubaoClient.setClientName("豆包(Doubao-主)");
                clients.add(doubaoClient);
                System.out.println("✓ 已配置豆包(Doubao)客户端 - 主接入点");
            }
        } catch (Exception e) {
            System.err.println("⚠ 无法初始化豆包客户端(主): " + e.getMessage());
        }
        
        // 2. 尝试创建第二个豆包客户端（优先级2，备用接入点）
        try {
            String doubaoApiKey2 = Config.get("DOUBAO_API_KEY_2");
            String doubaoEndpointId2 = Config.get("DOUBAO_ENDPOINT_ID_2");
            String doubaoBaseUrl2 = Config.get("DOUBAO_BASE_URL_2");
            
            if (doubaoApiKey2 != null && !doubaoApiKey2.isEmpty()) {
                DoubaoClient doubaoClient2 = new DoubaoClient(doubaoApiKey2, doubaoEndpointId2, null, null, doubaoBaseUrl2);
                doubaoClient2.setClientName("豆包(Doubao-备用)");
                clients.add(doubaoClient2);
                System.out.println("✓ 已配置豆包(Doubao)客户端 - 备用接入点");
            }
        } catch (Exception e) {
            System.err.println("⚠ 无法初始化豆包客户端(备用): " + e.getMessage());
        }
        
        // 3. 尝试创建第三个豆包客户端（优先级3，如果配置了）
        try {
            String doubaoApiKey3 = Config.get("DOUBAO_API_KEY_3");
            String doubaoEndpointId3 = Config.get("DOUBAO_ENDPOINT_ID_3");
            String doubaoBaseUrl3 = Config.get("DOUBAO_BASE_URL_3");
            
            if (doubaoApiKey3 != null && !doubaoApiKey3.isEmpty()) {
                DoubaoClient doubaoClient3 = new DoubaoClient(doubaoApiKey3, doubaoEndpointId3, null, null, doubaoBaseUrl3);
                doubaoClient3.setClientName("豆包(Doubao-备用2)");
                clients.add(doubaoClient3);
                System.out.println("✓ 已配置豆包(Doubao)客户端 - 备用接入点2");
            }
        } catch (Exception e) {
            System.err.println("⚠ 无法初始化豆包客户端(备用2): " + e.getMessage());
        }
        
        // 4. 可以在这里添加更多AI客户端（如通义千问、文心一言等）
        // TODO: 添加更多AI服务支持
        
        if (clients.isEmpty()) {
            System.err.println("警告: 没有可用的AI客户端配置");
            return null;
        }
        
        return new AIFailoverManager(clients);
    }
    
    /**
     * 检查是否有可用的AI配置
     */
    public static boolean hasAvailableConfig() {
        String doubaoApiKey = Config.get("DOUBAO_API_KEY");
        String doubaoApiKey2 = Config.get("DOUBAO_API_KEY_2");
        String doubaoApiKey3 = Config.get("DOUBAO_API_KEY_3");
        
        return (doubaoApiKey != null && !doubaoApiKey.isEmpty()) ||
               (doubaoApiKey2 != null && !doubaoApiKey2.isEmpty()) ||
               (doubaoApiKey3 != null && !doubaoApiKey3.isEmpty());
    }
}
