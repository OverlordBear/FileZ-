package com.seeyon.apps.gjx666.Filez.controller;
import com.seeyon.apps.gjx666.Filez.service.FileZService;
import com.seeyon.apps.gjx666.Filez.util.FilezAuthUtil;
import com.seeyon.ctp.common.AppContext;
import com.seeyon.ctp.common.authenticate.domain.User;
import com.seeyon.ctp.common.controller.BaseController;
import com.seeyon.ctp.common.filemanager.manager.FileManager;
import com.seeyon.ctp.common.filemanager.manager.PartitionManager;
import com.seeyon.ctp.common.po.filemanager.V3XFile;
import com.seeyon.ctp.util.annotation.CheckRoleAccess;
import com.seeyon.ctp.util.annotation.NeedlessCheckLogin;
import com.seeyon.ctp.util.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Slf4j
@Controller
@RequestMapping("/gjxFileZ.do")
public class GjxFileZController extends BaseController {

    private final FileManager fileManager = AppContext.getBeanByName("fileManager");
    private final FileZService fileZService = AppContext.getBeanByName("fileZService");

    /**
     * 1. 提货大门：给 FileZ 服务器提供文件流
     */
    @RequestMapping(params = "method=download")
    public ModelAndView download(HttpServletRequest request, HttpServletResponse response) {
        log.info(">>> 1. 提货大门：给 FileZ 服务器提供文件流");
        try {
            // ================= 1. 开启特权模式 =================
//            FilezAuthUtil.mockAdminContext("gjx");
            // ================= 2. 核心下载逻辑 =================
            String fileIdStr = request.getParameter("fileId");
            Long fileId = Long.parseLong(fileIdStr);
            V3XFile v3xFile = fileManager.getV3XFile(fileId);
            if (v3xFile != null) {
                File file = fileManager.getFile(v3xFile.getId(), v3xFile.getCreateDate());
                if (file != null && file.exists()) {
                    response.setContentType("application/octet-stream");
                    try (FileInputStream fis = new FileInputStream(file);
                         OutputStream os = response.getOutputStream()) {
                        byte[] buffer = new byte[1024 * 8];
                        int len;
                        while ((len = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        os.flush();
                    }
                }
            }
        } catch (Exception e) {
            log.error("极简中台：给 FileZ 提供下载流失败！", e);
        } finally {
            // ================= 3. 关闭特权模式 =================
            FilezAuthUtil.clearContext();
        }
        return null;
    }


    /**
     * 2. 统一初始化接口 (总调度中心)
     */
    @RequestMapping(params = "method=fileZInit")
    public ModelAndView fileZInit(HttpServletRequest request, HttpServletResponse response) {
        log.info(">>> 2. 进入 FileZ 统一调度中心 (fileZInit)");
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();

        try {
            // ================= 1. 获取真实业务上下文 =================
            // 直接从致远底层安全上下文中获取当前登录人！
            User currentUser = AppContext.getCurrentUser();
            if (currentUser == null) {
                log.warn(">>> 生成 FileZ 链接失败：未获取到登录信息，可能 Session 已过期");
                result.put("success", false);
                result.put("msg", "登录已过期，请刷新页面重新登录");
                writeJsonResponse(response, result);
                return null;
            }

            String userId = String.valueOf(currentUser.getId());
            String userName = currentUser.getName();
            log.info(">>> 当前真实操作人: {} ({})", userName, userId);

            // ================= 2. 接收前端传递的业务参数 =================
            String fileId = request.getParameter("fileId");
            String fileName = request.getParameter("fileName");

            // 获取前端传来的动作指令，默认为 view
            String state = request.getParameter("state");
            if (state == null || state.trim().isEmpty()) {
                state = "view";
            }

            // ================= 3. 呼叫核心算签引擎 =================
            String finalUrl = fileZService.generateFileZUrl(fileId, fileName, userId, userName, state);

            if (finalUrl != null) {
                result.put("success", true);
                result.put("url", finalUrl);
            } else {
                result.put("success", false);
                result.put("msg", "生成 FileZ 链接失败，请联系管理员");
            }

            writeJsonResponse(response, result);

        } catch (Exception e) {
            log.error("fileZInit 处理失败", e);
        }
        return null;
    }

    // 抽离一个小方法用来写 JSON，保持代码整洁
    private void writeJsonResponse(HttpServletResponse response, Map<String, Object> result) throws Exception {
        try (PrintWriter out = response.getWriter()) {
            out.write(JSONUtil.toJSONString(result));
            out.flush();
        }
    }
    /**
     * 3. 还货大门：接收 FileZ 编辑后的文件并保存
     */
    @RequestMapping(params = "method=uploadFile")
    public ModelAndView uploadFile(HttpServletRequest request, HttpServletResponse response) {
        log.info(">>> 3. 还货大门被敲响：FileZ 正在推送修改后的文件...");
        try {
            // ================= 1. 开启特权模式 (防止更新数据库报错) =================
//            FilezAuthUtil.mockAdminContext("gjx");暂时不需要伪造信息

            // ================= 2. 接收文件流 =================
            // 强转为包含附件的请求对象
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            MultipartFile uploadFile = multipartRequest.getFile("file"); // FileZ 默认传过来的文件域叫 "file"

            String fileIdStr = request.getParameter("fileId");
            Long fileId = Long.parseLong(fileIdStr);

            if (uploadFile != null && !uploadFile.isEmpty()) {
                V3XFile v3xFile = fileManager.getV3XFile(fileId);
                if (v3xFile != null) {
                    Date now = new Date();

                    // ================= 3. 物理覆盖旧文件 =================
                    File localFile = fileManager.getFile(v3xFile.getId(), v3xFile.getCreateDate());
                    if (localFile != null) {
                        // 3.1 先覆盖第一层路径 (V3XFile 预览路径)
                        FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), localFile);

                        // 3.2 覆盖第二层路径 (CtpFile 原生下载路径) - 填补致远双路径巨坑！
                        try {
                            // 动态获取分区管理器
                            PartitionManager partitionManager = AppContext.getBeanByName("partitionManager");
                            if (partitionManager != null) {
                                String ctpFilePath = partitionManager.getFolder(v3xFile.getCreateDate(), true) + File.separator + fileId;
                                File ctpFile = new File(ctpFilePath);

                                // ⚠️ 绝招：因为 uploadFile 的输入流在上面已经被读空了，不能再读第二次！
                                // 所以我们直接把刚刚写好的 localFile 复制到 ctpFile 路径去
                                if (!localFile.getAbsolutePath().equals(ctpFile.getAbsolutePath())) {
                                    FileUtils.copyFile(localFile, ctpFile);
                                    log.info(">>> 🕳️ 填坑成功：原生下载路径也已同步覆写！路径: {}", ctpFilePath);
                                }
                            }
                        } catch (Exception e) {
                            log.error(">>> 填坑失败：同步覆盖原生下载路径异常", e);
                        }
                    }

                    // ================= 4. 更新数据库记录 =================
                    v3xFile.setUpdateDate(now);
                    v3xFile.setSize(uploadFile.getSize());
                    fileManager.save(v3xFile); // 保存更新到 v3x_file 表

                    log.info(">>> 🎉 文件闭环保存成功！ID: {}, 更新时间: {}, 新大小: {} bytes", fileId, now, uploadFile.getSize());

                    // ================= 5. 返回 FileZ 要求的标准回执 =================
                    // ⚠️ 注意：如果这里不返回标准的 JSON，FileZ 的前端页面会一直显示“保存失败”或者转圈
                    Map<String, Object> metaMap = new HashMap<>();
                    metaMap.put("id", fileIdStr);
                    metaMap.put("name", v3xFile.getFilename());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    metaMap.put("modified_at", sdf.format(now));


                    Map<String, Object> createdBy = new HashMap<>();
                    // 从你组装的 uploadUrl 参数里把 memberId 拿出来
                    String memberId = request.getParameter("memberId");
                    createdBy.put("id", memberId != null ? memberId : "u001");
                    createdBy.put("name", "编辑者"); // 名字无所谓，但节点结构必须有
                    metaMap.put("created_by", createdBy);

                    Map<String, Object> permissions = new HashMap<>();
                    permissions.put("write", true);
                    permissions.put("read", true);
                    metaMap.put("permissions", permissions);

                    response.setContentType("application/json;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.write(JSONUtil.toJSONString(metaMap));
                        out.flush();
                    }
                } else {
                    log.error(">>> 保存失败：数据库中不存在对应的 V3XFile");
                }
            } else {
                log.error(">>> 保存失败：FileZ 推送的文件流为空");
            }
        } catch (Exception e) {
            log.error(">>> 接收 FileZ 推送文件异常", e);
            response.setStatus(500); // 告诉 FileZ 保存失败
        } finally {
            // ================= 6. 关闭特权模式 =================
            FilezAuthUtil.clearContext();
        }
        return null;
    }
}