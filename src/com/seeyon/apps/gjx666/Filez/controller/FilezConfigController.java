package com.seeyon.apps.gjx666.Filez.controller;
import com.seeyon.apps.gjx666.Filez.config.FilezConfig;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.config.manager.ConfigManager;
import com.seeyon.ctp.common.controller.BaseController;
import com.seeyon.ctp.common.po.config.ConfigItem;
import com.seeyon.ctp.util.UUIDLong;
import com.seeyon.ctp.util.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Controller
@RequestMapping("/filezConfig")
public class FilezConfigController extends BaseController {


    // 必须与 FilezConfig.java 中的 CATEGORY 保持绝对一致
    private static final String CONFIG_CATEGORY = "filez_integration";

    // 定义 Filez 需要的 5 个核心配置 Key
    private static final String[] KEYS = {
            "filez.url", "filez.secret", "filez.repoId", "filez.context", "oa.url"
    };

    /**
     * 打开配置页面
     */
    @GetMapping("/index.do")
    public ModelAndView index(HttpServletRequest request) {
        // 指向我们稍后要写的 JSP 页面
        ModelAndView mav = new ModelAndView("apps/filezConfig/pub/filezConfig");

        ConfigManager configManager = AppContext.getBeanByName("configManager");
        FilezConfig filezConfig = AppContext.getBeanByName("filezConfig");

        Map<String, Object> configData = new HashMap<>();

        // 遍历读取数据库，如果数据库为空，则尝试拉取代码里的默认值
        for (String key : KEYS) {
            String dbVal = getConfigValue(configManager, key);
            if ("".equals(dbVal) && filezConfig != null) {
                dbVal = getFallbackValue(filezConfig, key);
            }
            configData.put(key, dbVal);
        }

        mav.addObject("initData", JSONUtil.toJSONString(configData));
        return mav;
    }

    /**
     * 保存配置并触发热更新
     */
    @PostMapping(value = "/save.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> save(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        ConfigManager configManager = AppContext.getBeanByName("configManager");

        try {
            // 1. 遍历并保存到致远 v3x_config 表
            for (String key : KEYS) {
                if (params.containsKey(key)) {
                    saveConfigValue(configManager, key, (String) params.get(key));
                }
            }

            // 2. 核心：触发热更新，让内存中的静态变量立刻变成最新值
            FilezConfig filezConfig = AppContext.getBeanByName("filezConfig");
            if (filezConfig != null) {
                filezConfig.init();
            }

            response.put("success", true);
            response.put("message", "Filez 文档中台配置保存成功，并已热更新生效！");
        } catch (Exception e) {
            log.error("保存 Filez 配置失败", e);
            response.put("success", false);
            response.put("message", "保存失败: " + e.getMessage());
        }
        return response;
    }

    // --- 辅助工具方法 ---

    private String getConfigValue(ConfigManager configManager, String key) {
        ConfigItem item = configManager.getConfigItem(CONFIG_CATEGORY, key);
        return (item != null && item.getConfigValue() != null) ? item.getConfigValue() : "";
    }

    private void saveConfigValue(ConfigManager configManager, String key, String value) {
        if (value == null) value = "";
        ConfigItem item = configManager.getConfigItem(CONFIG_CATEGORY, key);
        if (item == null) {
            item = new ConfigItem();
            item.setId(UUIDLong.longUUID());
            item.setConfigCategory(CONFIG_CATEGORY);
            item.setConfigItem(key);
            item.setConfigValue(value);
            configManager.addConfigItem(item);
        } else {
            item.setConfigValue(value);
            configManager.updateConfigItem(item);
        }
    }

    private String getFallbackValue(FilezConfig config, String key) {
        switch (key) {
            case "filez.url": return config.getFilezUrl();
            case "filez.secret": return config.getSecret();
            case "filez.repoId": return config.getRepoId();
            case "filez.context": return config.getContext();
            case "oa.url": return config.getOaUrl();
            default: return "";
        }
    }
}