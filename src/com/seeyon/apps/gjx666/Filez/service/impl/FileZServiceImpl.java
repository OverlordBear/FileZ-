package com.seeyon.apps.gjx666.Filez.service.impl;
import com.seeyon.apps.gjx666.Filez.config.FilezConfig;
import com.seeyon.apps.gjx666.Filez.service.FileZService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Base64;

@Slf4j
@Service("fileZService")
public class FileZServiceImpl implements FileZService {
    @Override
    public String generateFileZUrl(String fileId, String fileName, String userId, String userName, String action) {
        try {
            // 1. 获取全局配置
            String filezUrl = FilezConfig.FILEZ_URL;
            String secret = FilezConfig.SECRET;
            String repoId = FilezConfig.REPO_ID;
            String context = FilezConfig.CONTEXT;
            String oaUrl = FilezConfig.OA_URL;

            // 2. 动态构造回调地址
            String downloadUrl = oaUrl + "/seeyon/gjxFileZ.do?method=download&fileId=" + fileId;

            // 【核心改变】：如果是编辑模式，必须提供上传地址（还货）
            String uploadUrl = "";
            boolean canWrite = false;
            if ("edit".equalsIgnoreCase(action)) {
                canWrite = true;
                uploadUrl = oaUrl + "/seeyon/gjxFileZ.do?method=uploadFile&fileId=" + fileId + "&memberId=" + userId;
                log.info(">>> 编辑模式开启，注入上传回调地址: {}", uploadUrl);
            }

            // 3. 构造 UserInfo
            String userInfoJson = "{\"id\":\"" + userId + "\",\"display_name\":\"" + userName + "\",\"email\":\"\"}";
            String userInfo = URLEncoder.encode(Base64.getEncoder().encodeToString(userInfoJson.getBytes(StandardCharsets.UTF_8)), "UTF-8");

            // 4. 动态构造 Meta (注意 permissions 里面的 write 权限由 canWrite 变量控制)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String modifiedAt = sdf.format(new Date());

            String metaJson = "{\"id\":\"" + fileId + "\",\"name\":\"" + fileName + "\",\"modified_at\":\"" + modifiedAt + "\"," +
                    "\"created_by\":{\"id\":\"" + userId + "\",\"name\":\"" + userName + "\"}," +
                    "\"permissions\":{\"write\":" + canWrite + ",\"read\":true,\"download\":true,\"print\":true}}";
            String meta = URLEncoder.encode(Base64.getEncoder().encodeToString(metaJson.getBytes(StandardCharsets.UTF_8)), "UTF-8");

            // 5. 拼接核心参数 (注意传给 FileZ 的 action 参数)
            String pathPrefix = context.startsWith("/") ? context : "/" + context;
            String rawPath = pathPrefix + "/driver-callback?repoId=" + repoId + "&docId=" + fileId +
                    "&action=" + action + "&userinfo=" + userInfo + "&meta=" + meta +
                    "&downloadUrl=" + URLEncoder.encode(downloadUrl, "UTF-8") +
                    "&uploadUrl=" + URLEncoder.encode(uploadUrl, "UTF-8") + "&params=fileId=" + fileId;

            // 6. 生成签名并拼装最终链接
            return filezUrl + getPathWithHmac(rawPath, secret);

        } catch (Exception e) {
            log.error("生成 Filez 动态链接失败", e);
            return null;
        }
    }

    // --- 签名工具方法 (原封不动搬过来) ---
    private String getPathWithHmac(String path, String secret) throws Exception {
        StringBuilder builder = new StringBuilder(path);
        builder.append("&ts=").append(System.currentTimeMillis());
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        mac.update(builder.toString().getBytes(StandardCharsets.UTF_8));
        String hmac = String.format("%064x", new BigInteger(1, mac.doFinal()));
        builder.append("&HMAC=").append(hmac);
        return builder.toString();
    }

}