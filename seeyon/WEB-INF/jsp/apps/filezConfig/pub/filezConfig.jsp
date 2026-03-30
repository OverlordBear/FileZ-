<%@ page contentType="text/html; charset=UTF-8" isELIgnored="false" %>
<%@ include file="/WEB-INF/jsp/common/common.jsp" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <title>联想 Filez 文档中台集成配置</title>

    <link rel="stylesheet" type="text/css" href="${path}/common/css/default.css${ctp:resSuffix()}"/>
    <link rel="stylesheet" type="text/css" href="${path}/apps_res/filezConfigMenu/css/filezConfig.css${ctp:resSuffix()}"/>

    <style>
        .config-card { margin: 20px; background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .form-group { margin-bottom: 15px; }
        .form-label { display: block; font-weight: bold; margin-bottom: 5px; color: #333; }
        .form-input { width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
        .bottom-bar { padding: 15px 20px; background: #f8f9fa; text-align: right; border-top: 1px solid #eee; position: fixed; bottom: 0; width: 100%; }
        .btn { padding: 8px 20px; border-radius: 4px; cursor: pointer; border: none; font-size: 14px; margin-left: 10px; }
        .btn-primary { background: #007bff; color: #fff; }
        .btn-edit { background: #28a745; color: #fff; }
        .btn-default { background: #6c757d; color: #fff; }
        .hidden { display: none; }
    </style>
</head>
<body class="h100b over_hidden">

<div class="scroll-container" style="padding-bottom: 60px;">
    <div class="config-card">
        <h2 style="border-bottom: 1px solid #eee; padding-bottom: 10px; margin-bottom: 20px;">📁 联想 Filez 核心参数配置</h2>

        <div class="form-group">
            <label class="form-label">Filez 服务器地址 (filez.url)</label>
            <input type="text" id="filez_url" class="form-input" placeholder="例如: http://43.142.34.184:8001" disabled/>
        </div>

        <div class="form-group">
            <label class="form-label">安全密钥 (filez.secret)</label>
            <input type="text" id="filez_secret" class="form-input" placeholder="填入控制台获取的 Secret" disabled/>
        </div>

        <div class="form-group">
            <label class="form-label">仓库/应用 ID (filez.repoId)</label>
            <input type="text" id="filez_repoId" class="form-input" placeholder="例如: OA1 或 publicApi" disabled/>
        </div>

        <div class="form-group">
            <label class="form-label">API 路由前缀 (filez.context)</label>
            <input type="text" id="filez_context" class="form-input" placeholder="通常为: docs/publicapi/v1" disabled/>
        </div>

        <div class="form-group" style="margin-top: 30px; border-top: 1px dashed #ccc; padding-top: 20px;">
            <label class="form-label">致远 OA 回调拉取地址 (oa.url)</label>
            <input type="text" id="oa_url" class="form-input" placeholder="例如: http://101.35.43.199:80 (云端服务器必须能访问此地址)" disabled/>
            <span style="font-size: 12px; color: #e74c3c;">* 注意：此处配置的是 OA 暴露给公网的地址，Filez 将通过此地址回调保存文件。</span>
        </div>
    </div>
</div>

<div class="bottom-bar">
    <button id="btnEdit" class="btn btn-edit">✎ 修改配置</button>
    <button id="btnSave" class="btn btn-primary hidden">保存并热更新</button>
    <button id="btnCancel" class="btn btn-default hidden">取消</button>
</div>

<script type="text/javascript" src="${path}/apps_res/filezConfigMenu/js/config/filezConfig.js${ctp:resSuffix()}"></script>

<script type="text/javascript">
    console.log("1. 页面内联脚本开始解析...");

    window.onload = function () {
        console.log("2. DOM 与静态资源加载完毕！");

        var serverData = '${initData}';
        console.log("3. 接收到的后端数据:", serverData);

        // 检查模块是否成功挂载到全局
        if (typeof FilezConfigModule !== 'undefined') {
            console.log("4. 成功找到 FilezConfigModule，准备执行 init()...");
            try {
                FilezConfigModule.init(serverData ? serverData : {});
                console.log("5. init() 执行成功，事件绑定完毕！");
            } catch (e) {
                console.error("❌ init() 执行时发生异常:", e);
                alert("JS 模块初始化失败，请查看控制台报错！");
            }
        } else {
            console.error("❌ 严重错误：未找到 FilezConfigModule！");
            alert("错误：未能加载 filezConfig.js 文件！\n请检查文件是否已放到 Tomcat 的 apps_res/gjx666/filez/js/ 目录下。");
        }
    };
</script>

</body>
</html>