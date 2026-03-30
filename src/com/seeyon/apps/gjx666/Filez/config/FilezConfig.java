package com.seeyon.apps.gjx666.Filez.config;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.config.manager.ConfigManager;
import com.seeyon.ctp.common.po.config.ConfigItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Filez 文档中台全局配置中心 (支持热更新)
 */
@Slf4j
@Component("filezConfig")
public class FilezConfig {


    // 对应致远配置表（v3x_config）中的 category 字段
    private static final String CONFIG_CATEGORY = "filez_integration";

    // ========================================================================
    // 1. Properties 文件中的“出厂默认值” (可以配置在 application.properties 等)
    // ========================================================================
    @Value("${filez.url:http://43.142.34.184:8001}")
    private String defaultFilezUrl;

    @Value("${filez.secret:dmKMDhYh9ZWtZ3b9WA17UINJHjQgrtwJoz91/B1n1Jpgky4iBtjlVEPMCW29ugkLDPJlr7XytGd1wUeDmxCswA==}")
    private String defaultSecret;

    @Value("${filez.repoId:OA2}")
    private String defaultRepoId;

    @Value("${filez.context:docs/app}")
    private String defaultContext;

    @Value("${oa.url:http://101.35.43.199:80}")
    private String defaultOaUrl;

    // ========================================================================
    // 2. 对外暴露的静态变量 (业务逻辑直接使用这些静态变量)
    // ========================================================================
    public static String FILEZ_URL;
    public static String SECRET;
    public static String REPO_ID;
    public static String CONTEXT;
    public static String OA_URL;

    /**
     * 初始化/热刷新入口
     */
    @PostConstruct
    public void init() {
        ConfigManager configManager = AppContext.getBeanByName("configManager");
        log.info(">>> [FilezConfig] 开始加载或刷新配置...");

        FILEZ_URL = getEffectiveValue(configManager, "filez.url", defaultFilezUrl);
        SECRET = getEffectiveValue(configManager, "filez.secret", defaultSecret);
        REPO_ID = getEffectiveValue(configManager, "filez.repoId", defaultRepoId);
        CONTEXT = getEffectiveValue(configManager, "filez.context", defaultContext);
        OA_URL = getEffectiveValue(configManager, "oa.url", defaultOaUrl);

        log.info(">>> [FilezConfig] 刷新完成。Filez URL: " + FILEZ_URL + ", OA URL: " + OA_URL);
    }

    /**
     * 获取生效值：优先读数据库，读不到则用默认值
     */
    private String getEffectiveValue(ConfigManager cm, String key, String defaultValue) {
        if (cm != null) {
            ConfigItem item = cm.getConfigItem(CONFIG_CATEGORY, key);
            if (item != null && item.getConfigValue() != null && !item.getConfigValue().trim().isEmpty()) {
                return item.getConfigValue();
            }
        }
        return defaultValue;
    }

    /**
     * 获取配置的静态方法，方便未注入的类使用
     */
    public String getFilezUrl() { return FILEZ_URL; }
    public String getSecret() { return SECRET; }
    public String getRepoId() { return REPO_ID; }
    public String getContext() { return CONTEXT; }
    public String getOaUrl() { return OA_URL; }
}