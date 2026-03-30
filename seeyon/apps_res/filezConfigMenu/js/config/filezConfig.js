(function (global, factory) {
  global.FilezConfigModule = factory();
})(window, function () {

  var FilezConfig = {
    isEditing: false, // 状态标记：是否处于编辑模式

    init: function (initData) {
      // 防止后端传来的数据为空
      this.data = typeof initData === 'string' ? (initData ? JSON.parse(initData) : {}) : initData;

      this.cacheDom();
      this.bindEvents();
      this.render();

      // 初始化完毕，默认进入只读模式
      this.toggleEditMode(false);
    },

    cacheDom: function () {
      this.dom = {
        // 按钮
        btnEdit: document.getElementById('btnEdit'),
        btnSave: document.getElementById('btnSave'),
        btnCancel: document.getElementById('btnCancel'),

        // Inputs (用于批量禁用/启用)
        allInputs: document.querySelectorAll('.form-input'),

        // 具体字段
        filez_url: document.getElementById('filez_url'),
        filez_secret: document.getElementById('filez_secret'),
        filez_repoId: document.getElementById('filez_repoId'),
        filez_context: document.getElementById('filez_context'),
        oa_url: document.getElementById('oa_url')
      };
    },

    bindEvents: function () {
      var self = this;

      // 1. 点击“修改配置”
      if (this.dom.btnEdit) {
        this.dom.btnEdit.onclick = function () {
          self.toggleEditMode(true);
        };
      }

      // 2. 点击“取消”
      if (this.dom.btnCancel) {
        this.dom.btnCancel.onclick = function () {
          var isSure = window.confirm("确定要取消修改吗？\n未保存的内容将会丢失。");
          if (isSure) {
            self.render(); // 恢复原数据
            self.toggleEditMode(false); // 变回只读
          }
        };
      }

      // 3. 点击“保存”
      if (this.dom.btnSave) {
        this.dom.btnSave.onclick = function () {
          self.doSave();
        };
      }
    },

    // 切换 编辑/只读 状态
    toggleEditMode: function (isEdit) {
      this.isEditing = isEdit;

      // 控制输入框是否可写
      for (var i = 0; i < this.dom.allInputs.length; i++) {
        this.dom.allInputs[i].disabled = !isEdit;
      }

      // 控制按钮显示/隐藏
      if (isEdit) {
        if(this.dom.btnEdit) this.dom.btnEdit.classList.add('hidden');
        if(this.dom.btnSave) this.dom.btnSave.classList.remove('hidden');
        if(this.dom.btnCancel) this.dom.btnCancel.classList.remove('hidden');
      } else {
        if(this.dom.btnEdit) this.dom.btnEdit.classList.remove('hidden');
        if(this.dom.btnSave) this.dom.btnSave.classList.add('hidden');
        if(this.dom.btnCancel) this.dom.btnCancel.classList.add('hidden');
      }
    },

    // 将数据渲染到页面
    render: function () {
      if (!this.data) return;
      var d = this.data;

      var val = function(v) { return v ? v : ''; };

      if(this.dom.filez_url) this.dom.filez_url.value = val(d['filez.url']);
      if(this.dom.filez_secret) this.dom.filez_secret.value = val(d['filez.secret']);
      if(this.dom.filez_repoId) this.dom.filez_repoId.value = val(d['filez.repoId']);
      if(this.dom.filez_context) this.dom.filez_context.value = val(d['filez.context']);
      if(this.dom.oa_url) this.dom.oa_url.value = val(d['oa.url']);
    },

    // 获取表单最新的数据
    getFormData: function () {
      return {
        'filez.url': this.dom.filez_url ? this.dom.filez_url.value : '',
        'filez.secret': this.dom.filez_secret ? this.dom.filez_secret.value : '',
        'filez.repoId': this.dom.filez_repoId ? this.dom.filez_repoId.value : '',
        'filez.context': this.dom.filez_context ? this.dom.filez_context.value : '',
        'oa.url': this.dom.oa_url ? this.dom.oa_url.value : ''
      };
    },

    // 提交保存
    doSave: async function () {
      var self = this;
      var data = this.getFormData();

      if(this.dom.btnSave) {
        this.dom.btnSave.innerText = '保存中...';
        this.dom.btnSave.disabled = true;
      }

      try {
        var response = await fetch('/seeyon/filezConfig/save.do', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        });

        var res = await response.json();

        if (res.success) {
          alert("✅ " + res.message);
          self.data = data; // 更新本地缓存数据
          self.toggleEditMode(false); // 保存成功后退出编辑模式
        } else {
          alert("❌ " + res.message);
        }
      } catch (e) {
        console.error(e);
        alert("系统异常或保存失败: " + e.message);
      } finally {
        if(this.dom.btnSave) {
          this.dom.btnSave.innerText = '保存并热更新';
          this.dom.btnSave.disabled = false;
        }
      }
    }
  };

  return FilezConfig;
});