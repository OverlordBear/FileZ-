package com.seeyon.apps.gjx666.Filez.util;

import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.GlobalNames;
import com.seeyon.ctp.common.authenticate.domain.User;
import com.seeyon.ctp.organization.bo.V3xOrgMember;
import com.seeyon.ctp.organization.manager.OrgManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Filez 权限与上下文工具类
 */
@Slf4j
public class FilezAuthUtil {

    /**
     * 临时伪造管理员上下文，用于绕过底层权限校验
     * @param adminLoginName 管理员的登录名，例如 "gjx"
     */
    public static void mockAdminContext(String adminLoginName) {
        try {
            // 从 Spring 容器获取 OrgManager
            OrgManager orgManager =AppContext.getBeanByName("orgManager");
            if (orgManager == null) {
                log.error(">>> [FilezAuthUtil] 获取 orgManager 失败！");
                return;
            }

            V3xOrgMember admin = orgManager.getMemberByLoginName(adminLoginName);
            if (admin != null) {
                User mockUser = new User();
                mockUser.setId(admin.getId());
                mockUser.setName(admin.getName());
                mockUser.setLoginName(admin.getLoginName());
                mockUser.setAccountId(admin.getOrgAccountId());
                mockUser.setLoginAccount(admin.getOrgAccountId());

                // 塞入致远全局线程上下文
                AppContext.putThreadContext(GlobalNames.SESSION_CONTEXT_USERINFO_KEY, mockUser);
                log.info(">>> [FilezAuthUtil] 已成功伪造管理员 [{}] 的上下文", admin.getName());
            } else {
                log.error(">>> [FilezAuthUtil] 严重错误：未找到配置的管理员用户 [{}]！", adminLoginName);
            }
        } catch (Exception e) {
            log.error(">>> [FilezAuthUtil] 伪造管理员上下文时发生异常", e);
        }
    }

    /**
     * 清理线程上下文（极其重要，必须在 finally 中调用）
     */
    public static void clearContext() {
        try {
            AppContext.removeThreadContext(GlobalNames.SESSION_CONTEXT_USERINFO_KEY);
            log.debug(">>> [FilezAuthUtil] 已清理线程上下文");
        } catch (Exception e) {
            log.error(">>> [FilezAuthUtil] 清理上下文异常", e);
        }
    }
}