const local = {
  system: {
    title: 'EnvOps',
    updateTitle: '系统版本更新通知',
    updateContent: '检测到系统有新版本发布，是否立即刷新页面？',
    updateConfirm: '立即刷新',
    updateCancel: '稍后再说'
  },
  common: {
    action: '操作',
    add: '新增',
    addSuccess: '添加成功',
    backToHome: '返回首页',
    batchDelete: '批量删除',
    cancel: '取消',
    close: '关闭',
    check: '勾选',
    selectAll: '全选',
    expandColumn: '展开列',
    columnSetting: '列设置',
    config: '配置',
    confirm: '确认',
    delete: '删除',
    deleteSuccess: '删除成功',
    confirmDelete: '确认删除吗？',
    edit: '编辑',
    warning: '警告',
    error: '错误',
    index: '序号',
    keywordSearch: '请输入关键词搜索',
    logout: '退出登录',
    logoutConfirm: '确认退出登录吗？',
    lookForward: '敬请期待',
    modify: '修改',
    modifySuccess: '修改成功',
    noData: '无数据',
    operate: '操作',
    pleaseCheckValue: '请检查输入的值是否合法',
    refresh: '刷新',
    reset: '重置',
    search: '搜索',
    switch: '切换',
    tip: '提示',
    trigger: '触发',
    update: '更新',
    updateSuccess: '更新成功',
    userCenter: '个人中心',
    yesOrNo: {
      yes: '是',
      no: '否'
    }
  },
  request: {
    logout: '请求失败后登出用户',
    logoutMsg: '用户状态失效，请重新登录',
    logoutWithModal: '请求失败后弹出模态框再登出用户',
    logoutWithModalMsg: '用户状态失效，请重新登录',
    tokenExpired: 'token已过期'
  },
  theme: {
    themeDrawerTitle: '主题配置',
    tabs: {
      appearance: '外观',
      layout: '布局',
      general: '通用',
      preset: '预设'
    },
    appearance: {
      themeSchema: {
        title: '主题模式',
        light: '亮色模式',
        dark: '暗黑模式',
        auto: '跟随系统'
      },
      grayscale: '灰色模式',
      colourWeakness: '色弱模式',
      themeColor: {
        title: '主题颜色',
        primary: '主色',
        info: '信息色',
        success: '成功色',
        warning: '警告色',
        error: '错误色',
        followPrimary: '跟随主色'
      },
      themeRadius: {
        title: '主题圆角'
      },
      recommendColor: '应用推荐算法的颜色',
      recommendColorDesc: '推荐颜色的算法参照',
      preset: {
        title: '主题预设',
        apply: '应用',
        applySuccess: '预设应用成功',
        default: {
          name: '默认预设',
          desc: 'EnvOps 默认主题预设'
        },
        dark: {
          name: '暗色预设',
          desc: '适用于夜间使用的暗色主题预设'
        },
        compact: {
          name: '紧凑型',
          desc: '适用于小屏幕的紧凑布局预设'
        },
        azir: {
          name: 'Azir的预设',
          desc: '是 Azir 比较喜欢的莫兰迪色系冷淡风'
        }
      }
    },
    layout: {
      layoutMode: {
        title: '布局模式',
        vertical: '左侧菜单模式',
        'vertical-mix': '左侧菜单混合模式',
        'vertical-hybrid-header-first': '左侧混合-顶部优先',
        horizontal: '顶部菜单模式',
        'top-hybrid-sidebar-first': '顶部混合-侧边优先',
        'top-hybrid-header-first': '顶部混合-顶部优先',
        vertical_detail: '左侧菜单布局，菜单在左，内容在右。',
        'vertical-mix_detail': '左侧双菜单布局，一级菜单在左侧深色区域，二级菜单在左侧浅色区域。',
        'vertical-hybrid-header-first_detail':
          '左侧混合布局，一级菜单在顶部，二级菜单在左侧深色区域，三级菜单在左侧浅色区域。',
        horizontal_detail: '顶部菜单布局，菜单在顶部，内容在下方。',
        'top-hybrid-sidebar-first_detail': '顶部混合布局，一级菜单在左侧，二级菜单在顶部。',
        'top-hybrid-header-first_detail': '顶部混合布局，一级菜单在顶部，二级菜单在左侧。'
      },
      tab: {
        title: '标签栏设置',
        visible: '显示标签栏',
        cache: '标签栏信息缓存',
        cacheTip: '离开页面后仍然保留标签栏信息',
        height: '标签栏高度',
        mode: {
          title: '标签栏风格',
          slider: '滑块风格',
          chrome: '谷歌风格',
          button: '按钮风格'
        },
        closeByMiddleClick: '鼠标中键关闭标签页',
        closeByMiddleClickTip: '启用后可以使用鼠标中键点击标签页进行关闭'
      },
      header: {
        title: '头部设置',
        height: '头部高度',
        breadcrumb: {
          visible: '显示面包屑',
          showIcon: '显示面包屑图标'
        }
      },
      sider: {
        title: '侧边栏设置',
        inverted: '深色侧边栏',
        width: '侧边栏宽度',
        collapsedWidth: '侧边栏折叠宽度',
        mixWidth: '混合布局侧边栏宽度',
        mixCollapsedWidth: '混合布局侧边栏折叠宽度',
        mixChildMenuWidth: '混合布局子菜单宽度',
        autoSelectFirstMenu: '自动选择第一个子菜单',
        autoSelectFirstMenuTip: '点击一级菜单时，自动选择并导航到第一个子菜单的最深层级'
      },
      footer: {
        title: '底部设置',
        visible: '显示底部',
        fixed: '固定底部',
        height: '底部高度',
        right: '底部居右'
      },
      content: {
        title: '内容区域设置',
        scrollMode: {
          title: '滚动模式',
          tip: '主题滚动仅 main 部分滚动，外层滚动可携带头部底部一起滚动',
          wrapper: '外层滚动',
          content: '主体滚动'
        },
        page: {
          animate: '页面切换动画',
          mode: {
            title: '页面切换动画类型',
            'fade-slide': '滑动',
            fade: '淡入淡出',
            'fade-bottom': '底部消退',
            'fade-scale': '缩放消退',
            'zoom-fade': '渐变',
            'zoom-out': '闪现',
            none: '无'
          }
        },
        fixedHeaderAndTab: '固定头部和标签栏'
      }
    },
    general: {
      title: '通用设置',
      watermark: {
        title: '水印设置',
        visible: '显示全屏水印',
        text: '自定义水印文本',
        enableUserName: '启用用户名水印',
        enableTime: '显示当前时间',
        timeFormat: '时间格式'
      },
      multilingual: {
        title: '多语言设置',
        visible: '显示多语言按钮'
      },
      globalSearch: {
        title: '全局搜索设置',
        visible: '显示全局搜索按钮'
      }
    },
    configOperation: {
      copyConfig: '复制配置',
      copySuccessMsg: '复制成功，请替换 src/theme/settings.ts 中的变量 themeSettings',
      resetConfig: '重置配置',
      resetSuccessMsg: '重置成功'
    }
  },
  route: {
    login: '登录',
    403: '无权限',
    404: '页面不存在',
    500: '服务器错误',
    'iframe-page': '外链页面',
    home: '工作台',
    asset: '资产中心',
    asset_host: '主机管理',
    asset_group: '分组管理',
    asset_tag: '标签管理',
    asset_credential: '凭据管理',
    asset_database: '数据库资源',
    monitor: '巡检监控',
    'monitor_detect-task': '巡检任务',
    monitor_metric: '指标快照',
    app: '应用中心',
    app_definition: '应用定义',
    app_version: '版本管理',
    app_package: '安装包管理',
    'app_config-template': '配置模板',
    'app_script-template': '脚本模板',
    deploy: '发布管理',
    deploy_task: '发布任务',
    task: '任务编排',
    task_center: '任务中心',
    'task_tracking_[id]': '任务追踪',
    traffic: '流量治理',
    traffic_controller: '流量控制',
    system: '系统管理',
    system_user: '用户管理'
  },
  page: {
    login: {
      common: {
        loginOrRegister: '登录 / 注册',
        userNamePlaceholder: '请输入用户名',
        phonePlaceholder: '请输入手机号',
        codePlaceholder: '请输入验证码',
        passwordPlaceholder: '请输入密码',
        confirmPasswordPlaceholder: '请再次输入密码',
        codeLogin: '验证码登录',
        confirm: '确定',
        back: '返回',
        validateSuccess: '验证成功',
        loginSuccess: '登录成功',
        welcomeBack: '欢迎回来，{userName} ！'
      },
      pwdLogin: {
        title: '密码登录',
        rememberMe: '记住我',
        forgetPassword: '忘记密码？',
        register: '注册账号',
        otherAccountLogin: '其他账号登录',
        otherLoginMode: '其他登录方式',
        superAdmin: '超级管理员',
        admin: '管理员',
        user: '普通用户'
      },
      codeLogin: {
        title: '验证码登录',
        getCode: '获取验证码',
        reGetCode: '{time}秒后重新获取',
        sendCodeSuccess: '验证码发送成功',
        demoCodeHint: '演示环境验证码为手机号后 6 位',
        imageCodePlaceholder: '请输入图片验证码'
      },
      register: {
        title: '注册账号',
        agreement: '我已经仔细阅读并接受',
        protocol: '《用户协议》',
        policy: '《隐私权政策》'
      },
      resetPwd: {
        title: '重置密码'
      },
      bindWeChat: {
        title: '绑定微信'
      }
    },
    home: {
      branchDesc:
        '为了方便大家开发和更新合并，我们对main分支的代码进行了精简，只保留了首页菜单，其余内容已移至example分支进行维护。预览地址显示的内容即为example分支的内容。',
      greeting: '早安，{userName}, 今天又是充满活力的一天!',
      weatherDesc: '今日多云转晴，20℃ - 25℃!',
      projectCount: '项目数',
      todo: '待办',
      message: '消息',
      downloadCount: '下载量',
      registerCount: '注册量',
      schedule: '作息安排',
      study: '学习',
      work: '工作',
      rest: '休息',
      entertainment: '娱乐',
      visitCount: '访问量',
      turnover: '成交额',
      dealCount: '成交量',
      projectNews: {
        title: '项目动态',
        moreNews: '更多动态',
        desc1: 'EnvOps 已完成首版前端壳层与导航骨架搭建。',
        desc2: '资产、巡检、应用、发布、流量与系统管理页面已接入动态路由。',
        desc3: '测试环境已切换到当前 EnvOps backend 联调地址。',
        desc4: '前端已移除对后端不存在 isRouteExist 接口的依赖。',
        desc5: 'Task 3 页面文案已接入 i18n，并完成中英文适配。'
      },
      creativity: '创意'
    },
    app: {
      common: {
        save: '保存',
        appCode: '应用编码',
        appName: '应用名称',
        appType: '应用类型',
        runtimeType: '运行时类型',
        deployMode: '部署方式',
        defaultPort: '默认端口',
        healthCheckPath: '健康检查路径',
        description: '描述',
        status: '状态',
        updatedAt: '更新时间'
      },
      definition: {
        desc: '通过真实应用定义接口维护应用基础信息与默认部署约束。',
        listTitle: '应用列表',
        detailTitle: '应用详情',
        formTitleCreate: '新增应用定义',
        formTitleEdit: '编辑应用定义'
      },
      version: {
        desc: '按应用查看版本清单，并维护包、配置模板、脚本模板关联。',
        listTitle: '版本列表',
        currentApp: '当前应用',
        selectApp: '选择应用',
        formTitleCreate: '新增应用版本',
        formTitleEdit: '编辑应用版本',
        versionNo: '版本号',
        packageId: '安装包',
        configTemplateId: '配置模板',
        scriptTemplateId: '脚本模板',
        changelog: '变更说明'
      },
      package: {
        desc: '使用安装包接口完成制品登记、上传与删除。',
        uploadTitle: '上传安装包',
        listTitle: '安装包列表',
        packageName: '安装包名称',
        packageType: '安装包类型',
        storageType: '存储类型',
        file: '文件',
        selectFile: '选择文件',
        uploadAction: '上传',
        uploadFileRequired: '请先选择待上传文件',
        filePath: '文件路径',
        fileSize: '文件大小',
        fileHash: '文件哈希'
      },
      configTemplate: {
        desc: '使用配置模板接口维护部署时可渲染的配置内容。',
        listTitle: '配置模板列表',
        formTitleCreate: '新增配置模板',
        formTitleEdit: '编辑配置模板',
        templateCode: '模板编码',
        templateName: '模板名称',
        templateContent: '模板内容',
        renderEngine: '渲染引擎'
      },
      scriptTemplate: {
        desc: '使用脚本模板接口维护部署脚本或初始化脚本。',
        listTitle: '脚本模板列表',
        formTitleCreate: '新增脚本模板',
        formTitleEdit: '编辑脚本模板',
        templateCode: '模板编码',
        templateName: '模板名称',
        scriptType: '脚本类型',
        scriptContent: '脚本内容'
      }
    },
    envops: {
      common: {
        environment: {
          production: '生产',
          staging: '预发',
          sandbox: '沙箱'
        },
        batch: {
          canary20: '20% 金丝雀',
          fullRelease: '全量发布',
          canary10: '10% 金丝雀'
        },
        owner: {
          envops: 'EnvOps',
          release: '发布团队',
          sre: 'SRE',
          qa: '测试团队'
        },
        team: {
          envops: 'EnvOps',
          release: '发布团队',
          traffic: '流量团队',
          qa: '测试团队',
          fintech: '金融科技',
          platform: '平台团队',
          sre: 'SRE'
        },
        status: {
          running: '进行中',
          pendingApproval: '待审批',
          rollbackRequired: '需要回滚',
          success: '成功',
          warning: '告警',
          timeout: '超时',
          online: '在线',
          offline: '离线',
          pending: '待处理',
          failed: '失败',
          managed: '已纳管',
          draft: '草稿',
          review: '审核中',
          queued: '排队中',
          cancelled: '已取消',
          rejected: '已拒绝',
          enabled: '已启用',
          preview: '预览中',
          standby: '待命',
          active: '活跃',
          disabled: '已禁用'
        },
        schedule: {
          every10Min: '每 10 分钟',
          everyHour: '每小时',
          every5Min: '每 5 分钟',
          every15Min: '每 15 分钟'
        },
        taskType: {
          inspection: '巡检',
          deploy: '发布',
          traffic: '流量',
          assetSync: '资产同步'
        },
        strategy: {
          headerCanary: '请求头金丝雀',
          blueGreen: '蓝绿发布',
          weightedRouting: '权重路由',
          emergencyRollback: '紧急回滚'
        },
        role: {
          superAdmin: '超级管理员',
          platformAdmin: '平台管理员',
          releaseManager: '发布负责人',
          trafficOwner: '流量负责人',
          observer: '观察者'
        },
        loginType: {
          passwordOtp: '密码 + OTP',
          password: '密码',
          sso: '单点登录'
        }
      },
      home: {
        hero: {
          title: 'EnvOps 控制平面',
          description: '这是资产、发布任务、流量策略与平台运维的首版前端壳层。',
          descriptionWithUser: '欢迎回来，{userName}。这是资产、发布任务、流量策略与平台运维的首版前端壳层。'
        },
        tags: {
          dynamicRoutesReady: '动态路由已就绪',
          vitestHarnessEnabled: 'Vitest 测试基座已启用'
        },
        summary: {
          managedApplications: {
            label: '纳管应用',
            desc: '已具备发布编排元数据的应用定义'
          },
          onlineHosts: {
            label: '在线主机',
            desc: '持续上报健康心跳的实例'
          },
          runningTasks: {
            label: '运行中任务',
            desc: '正在平台内执行的跨域作业'
          },
          trafficPolicies: {
            label: '流量策略',
            desc: '已启用的发布规则与控制策略'
          }
        },
        sections: {
          releasePipelineFocus: '发布流水线关注项',
          operatorFocus: '值班关注项',
          inspectionHealth: '巡检健康度',
          platformReadiness: '平台就绪度'
        },
        releaseTable: {
          taskId: '任务 ID',
          application: '应用',
          environment: '环境',
          batch: '批次',
          owner: '负责人',
          status: '状态'
        },
        operatorFocusDesc: '已更新为 EnvOps 首版壳层迭代内容',
        focusList: {
          item1: '复盘 asset-sync 金丝雀失败原因，并准备回滚说明。',
          item2: '确认 payment-gateway 生产发布的审批时间窗。',
          item3: '网关配置同步后，重新执行 traffic-canary-guard 检测。',
          item4: '完成首批 EnvOps 用户与访问角色的开通。'
        },
        inspectionTable: {
          task: '任务',
          target: '目标',
          schedule: '调度周期',
          lastResult: '最近结果'
        },
        readiness: {
          frontendShell: {
            label: '前端壳层',
            value: '资产、巡检、应用、发布、任务、流量与用户页面骨架已完成。'
          },
          routingMode: {
            label: '路由模式',
            value: '已支持动态路由初始化与菜单注入链路。'
          },
          qualityGate: {
            label: '质量门禁',
            value: 'Vitest 单测基座与 route store 测试已接入脚本。'
          },
          nextSlice: {
            label: '下一阶段',
            value: '在现有壳层上继续接入后端数据查询与工作流变更能力。'
          }
        }
      },
      assetHost: {
        hero: {
          title: '主机资产',
          description: '展示资产主机清单，支持新增纳管主机，并可直接进入已有监控快照的主机指标详情。'
        },
        tags: {
          cmdbSynchronized: 'CMDB 已同步',
          maintenanceWindow: '今日 1 个维护窗口'
        },
        summary: {
          managedHosts: {
            label: '纳管主机',
            desc: '当前资产库中已纳入管理的主机总数'
          },
          online: {
            label: '在线主机',
            desc: '当前资产库中状态为在线的主机总数'
          },
          pendingMaintenance: {
            label: '告警主机',
            desc: '当前资产库中状态为告警的主机总数'
          }
        },
        form: {
          title: '新增主机',
          hostName: '主机名',
          ipAddress: 'IP 地址',
          environment: '环境',
          clusterName: '集群名称',
          ownerName: '归属团队',
          status: '状态',
          lastHeartbeat: '最近心跳',
          placeholders: {
            hostName: '例如：host-sbx-01',
            ipAddress: '例如：10.60.1.20',
            clusterName: '例如：cn-shenzhen-a',
            ownerName: '例如：Asset Team',
            lastHeartbeat: '可选，ISO 时间，例如 2026-04-16T11:22:33'
          },
          actions: {
            create: '纳管主机'
          }
        },
        messages: {
          fillRequired: '请先填写主机名、IP、环境、集群、归属团队和状态',
          createSuccess: '主机纳管成功'
        },
        table: {
          title: '主机快照',
          host: '主机',
          ip: 'IP',
          environment: '环境',
          cluster: '集群',
          owner: '归属',
          status: '状态',
          lastHeartbeat: '最近心跳',
          latestMetric: '最新指标快照',
          operation: '操作',
          viewMetrics: '查看指标'
        }
      },
      assetGroup: {
        hero: {
          title: '分组管理',
          description: '按分组视角查看资产归属与纳管范围。'
        },
        tags: {
          groups: '分组 {count}',
          hosts: '主机 {count}'
        },
        table: {
          title: '分组列表',
          group: '分组',
          description: '说明',
          hostCount: '主机数'
        }
      },
      assetTag: {
        hero: {
          title: '标签管理',
          description: '使用统一标签标记操作系统、用途与维护属性。'
        },
        tags: {
          total: '标签 {count}'
        },
        table: {
          title: '标签目录',
          tag: '标签',
          color: '颜色',
          description: '说明'
        }
      },
      assetCredential: {
        hero: {
          title: '凭据管理',
          description: '集中维护主机登录与自动化执行所需的凭据条目。'
        },
        tags: {
          total: '总数 {count}',
          passwords: '口令 {count}',
          keys: '密钥 {count}',
          tokens: 'Token {count}'
        },
        types: {
          sshPassword: 'SSH 口令',
          sshKey: 'SSH 密钥',
          apiToken: 'API Token'
        },
        form: {
          title: '新建凭据',
          name: '名称',
          type: '类型',
          username: '用户名',
          secret: '密文或密钥内容',
          description: '说明',
          placeholders: {
            name: '例如：prod-root-password',
            username: '例如：root / deploy',
            secret: '输入密文、私钥或 token',
            description: '可选，用于备注使用范围'
          },
          actions: {
            create: '创建凭据'
          }
        },
        table: {
          title: '现有凭据',
          name: '名称',
          type: '类型',
          username: '用户名',
          description: '说明',
          createdAt: '创建时间'
        },
        messages: {
          fillNameAndType: '请先填写凭据名称和类型',
          createSuccess: '凭据创建成功'
        }
      },
      assetDatabase: {
        hero: {
          title: '数据库资源',
          description: '完成数据库资产登记、主机关联和凭据关联，提供可查询、可维护的数据库目录。'
        },
        tags: {
          registryReady: '资产登记已就绪',
          connectivityCheckReady: '支持真实连通性检测',
          warningManual: 'warning 仍为人工标记'
        },
        filters: {
          keywordPlaceholder: '按数据库名、实例名、归属团队或主机搜索',
          environmentPlaceholder: '按环境筛选',
          databaseTypePlaceholder: '按数据库类型筛选',
          lifecycleStatusPlaceholder: '按纳管状态筛选',
          connectivityStatusPlaceholder: '按连通性状态筛选'
        },
        summary: {
          managedDatabases: {
            label: '纳管中数据库',
            desc: '当前纳管状态为 managed 的数据库资源数'
          },
          warningDatabases: {
            label: '告警数据库',
            desc: '当前连通性状态为 warning 的数据库资源数'
          },
          onlineDatabases: {
            label: '在线数据库',
            desc: '当前连通性状态为 online 的数据库资源数'
          }
        },
        types: {
          mysql: 'MySQL',
          postgresql: 'PostgreSQL',
          oracle: 'Oracle',
          sqlserver: 'SQL Server',
          mongodb: 'MongoDB',
          redis: 'Redis'
        },
        connectivity: {
          unknown: '未知'
        },
        actions: {
          create: '新建数据库',
          edit: '编辑',
          save: '保存',
          check: '检测',
          checkSelected: '批量检测已选',
          checkCurrentPage: '检测当前页',
          checkAllFiltered: '检测全部筛选结果',
          closeResult: '关闭结果'
        },
        form: {
          titleCreate: '新建数据库资源',
          titleEdit: '编辑数据库资源',
          databaseName: '数据库名',
          databaseType: '数据库类型',
          environment: '环境',
          host: '所属主机',
          port: '端口',
          instanceName: '实例名',
          credential: '关联凭据',
          ownerName: '归属团队',
          lifecycleStatus: '纳管状态',
          connectivityStatus: '连通性状态',
          connectionUsername: '连接用户名',
          connectionPassword: '连接密码',
          lastCheckedAt: '最近检查时间',
          description: '说明',
          placeholders: {
            databaseName: '例如：order_prod',
            host: '请选择主机',
            port: '例如：3306',
            instanceName: '例如：mysql-prd-a',
            credential: '可选，选择数据库登录凭据',
            ownerName: '例如：Platform DBA',
            connectionUsername: '例如：orders_app',
            connectionPassword: '留空表示沿用已保存密码',
            lastCheckedAt: '可选，ISO 时间，例如 2026-04-18T12:00:00',
            description: '可选，补充用途、归属或风险说明'
          }
        },
        table: {
          title: '数据库目录',
          database: '数据库',
          type: '类型',
          environment: '环境',
          host: '主机',
          port: '端口',
          owner: '归属',
          lifecycleStatus: '纳管状态',
          connectivityStatus: '连通性',
          lastCheckedAt: '最近检查',
          operation: '操作'
        },
        messages: {
          fillRequired: '请先填写数据库名、类型、环境、主机、端口、归属团队、纳管状态和连通性状态',
          fillConnectionPair: '如需保存连接凭据，请同时填写连接用户名和连接密码',
          fillConnectionUsername: '修改连接密码前请先填写连接用户名',
          checkFinished: '数据库连通性检测已执行完成',
          createSuccess: '数据库资源创建成功',
          updateSuccess: '数据库资源更新成功'
        },
        result: {
          title: '检测结果',
          total: '总数',
          success: '成功',
          failed: '失败',
          skipped: '跳过',
          message: '结果说明'
        }
      },
      monitorDetectTask: {
        hero: {
          title: '巡检任务',
          description: '接入真实巡检任务接口，支持创建任务、手动执行，并回看最新执行结果。'
        },
        tags: {
          healthyCount: '{count} 个正常',
          attentionCount: '{count} 个待关注'
        },
        summary: {
          scheduledTasks: {
            label: '已注册任务',
            desc: '当前已纳入调度的巡检作业'
          },
          successRate: {
            label: '成功率',
            desc: '根据当前任务列表实时计算'
          },
          needsAttention: {
            label: '待关注',
            desc: '存在失败、超时或部分成功的任务'
          }
        },
        form: {
          title: '新建巡检任务',
          taskName: '任务名称',
          taskNamePlaceholder: '请输入任务名称',
          host: '目标主机',
          hostPlaceholder: '请选择目标主机',
          schedule: '调度周期',
          manualSchedule: '手动执行'
        },
        actions: {
          create: '创建任务',
          execute: '立即执行'
        },
        messages: {
          fillNameAndHost: '请先填写任务名称并选择目标主机',
          createSuccess: '巡检任务创建成功',
          executeSuccess: '巡检任务执行完成'
        },
        table: {
          title: '任务队列',
          task: '任务',
          target: '目标',
          schedule: '调度周期',
          lastRun: '最近执行',
          result: '结果',
          operation: '操作'
        }
      },
      monitorMetric: {
        hero: {
          title: '指标快照',
          description: '展示当前主机最新一次事实上报，承接资产主机到监控详情的查看链路。'
        },
        tags: {
          host: '主机 #{id}'
        },
        summary: {
          cpuCores: {
            label: 'CPU 核心数',
            desc: '来自最新事实快照的 CPU 规格'
          },
          memory: {
            label: '内存容量',
            desc: '来自最新事实快照的内存规模'
          },
          osName: {
            label: '操作系统',
            desc: '当前主机事实上报的系统版本'
          },
          agentVersion: {
            label: 'Agent 版本',
            desc: '主机最近一次上报所使用的 Agent 版本'
          }
        },
        detail: {
          title: '事实详情',
          item: '项目',
          value: '值',
          hostname: '主机名',
          osName: '操作系统',
          kernel: '内核版本',
          cpuCores: 'CPU 核心数',
          memory: '内存容量',
          agentVersion: 'Agent 版本',
          collectedAt: '采集时间'
        }
      },
      appDefinition: {
        hero: {
          title: '应用定义',
          description: '在自动化发布接管前，统一维护应用归属、运行时与交付元数据。'
        },
        tags: {
          definitionSyncEnabled: '定义同步已启用',
          draftsAwaitingReview: '2 个草稿待审核'
        },
        summary: {
          applications: {
            label: '应用数',
            desc: '当前已在 EnvOps 中登记的业务服务'
          },
          runtimeProfiles: {
            label: '运行时模板',
            desc: '可复用的构建与发布模板'
          },
          teamsCovered: {
            label: '覆盖团队',
            desc: '已映射应用归属关系的团队'
          }
        },
        table: {
          title: '定义目录',
          application: '应用',
          owner: '归属团队',
          runtime: '运行时',
          repository: '代码仓库',
          currentVersion: '当前版本',
          status: '状态'
        }
      },
      deployTask: {
        hero: {
          title: '发布任务',
          description: '跟踪审批、分批放量与执行结果，承接首版交付流水线能力。'
        },
        tags: {
          runningCanary: '1 个金丝雀发布中',
          rollbackRequired: '存在需要回滚项'
        },
        summary: {
          pendingApproval: {
            label: '待审批',
            desc: '等待人工确认的发布任务'
          },
          inProgress: {
            label: '执行中',
            desc: '正在目标集群内推进的任务'
          },
          failedIn24h: {
            label: '24 小时失败',
            desc: '需要回滚分析或重试的任务'
          }
        },
        filters: {
          status: '任务状态',
          taskType: '任务类型',
          application: '应用',
          environment: '发布环境',
          keyword: '关键词',
          createdRange: '创建时间范围',
          search: '搜索',
          reset: '重置',
          taskTypeInstall: '安装',
          taskTypeUpgrade: '升级',
          taskTypeRollback: '回滚'
        },
        sorting: {
          createdAt: '创建时间',
          updatedAt: '更新时间',
          taskNo: '任务 ID',
          status: '状态'
        },
        table: {
          title: '发布队列',
          taskId: '任务 ID',
          application: '应用',
          environment: '环境',
          batch: '批次',
          operator: '执行人',
          status: '状态'
        },
        actions: {
          detail: '详情',
          create: '新建任务',
          execute: '执行',
          retry: '重试',
          rollback: '回滚',
          approve: '通过',
          reject: '拒绝',
          cancel: '取消任务'
        },
        create: {
          title: '新建发布任务',
          taskName: '任务名称',
          taskNamePlaceholder: '请输入任务名称',
          taskType: '任务类型',
          app: '应用',
          appPlaceholder: '请选择应用',
          version: '版本',
          versionPlaceholder: '请选择版本',
          environment: '环境',
          hosts: '目标主机',
          hostsPlaceholder: '请选择主机',
          batchStrategy: '批次策略',
          batchStrategyAll: '全量',
          batchStrategyRolling: '滚动',
          batchSize: '批大小',
          batchSizePlaceholder: '滚动发布时请输入批大小',
          deployDir: '部署目录',
          deployDirPlaceholder: '请输入部署目录，例如 /data/apps/order-service',
          sshUser: 'SSH 用户',
          sshUserPlaceholder: '请输入 SSH 登录用户，例如 deploy',
          sshPort: 'SSH 端口',
          sshPortPlaceholder: '请输入 SSH 端口，默认 22',
          privateKeyPath: '私钥路径',
          privateKeyPathPlaceholder: '请输入私钥文件路径，例如 /data/keys/release.pem',
          remoteBaseDir: '远端发布根目录',
          remoteBaseDirPlaceholder: '请输入远端根目录，例如 /opt/envops/releases',
          rollbackCommand: '回滚命令',
          rollbackCommandPlaceholder: '可选，填写后回滚任务将直接执行这条命令',
          validation: {
            taskNameRequired: '请输入任务名称',
            appRequired: '请选择应用',
            versionRequired: '请选择版本',
            environmentRequired: '请选择环境',
            hostsRequired: '请选择至少一台主机',
            deployDirRequired: '请输入部署目录',
            sshUserRequired: '请输入 SSH 用户',
            sshPortInvalid: 'SSH 端口必须大于 0',
            privateKeyPathRequired: '请输入私钥路径',
            remoteBaseDirRequired: '请输入远端发布根目录',
            batchSizeRequired: '滚动发布时批大小必须大于 0'
          }
        },
        approval: {
          approveTitle: '审批通过发布任务',
          rejectTitle: '拒绝发布任务',
          comment: '审批备注',
          commentPlaceholder: '可选，输入审批备注'
        },
        tabs: {
          overview: '概览',
          hosts: '主机',
          logs: '日志'
        },
        progress: {
          totalHosts: '总主机数',
          pendingHosts: '待处理主机',
          runningHosts: '执行中主机',
          successHosts: '成功主机',
          failedHosts: '失败主机',
          cancelledHosts: '已取消主机'
        },
        detail: {
          title: '任务详情',
          manualRefresh: '手动刷新',
          taskId: '任务 ID',
          taskName: '任务名称',
          taskType: '任务类型',
          originTaskId: '源任务 ID',
          application: '应用',
          version: '版本',
          environment: '环境',
          batch: '批次',
          operator: '执行人',
          status: '状态',
          approvalOperator: '审批人',
          approvalComment: '审批备注',
          approvalAt: '审批时间',
          startedAt: '开始时间',
          finishedAt: '结束时间',
          createdAt: '创建时间',
          updatedAt: '更新时间'
        },
        error: {
          detailLoadFailed: '任务详情加载失败，请稍后重试。',
          hostsLoadFailed: '主机明细加载失败，请稍后重试。',
          logsLoadFailed: '执行日志加载失败，请稍后重试。',
          autoRefreshFailed: '自动刷新失败，请稍后手动重试。'
        },
        empty: {
          taskNotFound: '未找到任务详情',
          noHosts: '暂无主机明细',
          noLogs: '暂无执行日志'
        },
        hosts: {
          title: '主机明细',
          hostName: '主机名',
          ipAddress: 'IP 地址',
          status: '状态',
          currentStep: '当前步骤',
          startedAt: '开始时间',
          finishedAt: '结束时间',
          errorMsg: '错误信息'
        },
        logs: {
          title: '执行日志',
          createdAt: '时间',
          taskHostId: '主机任务 ID',
          logLevel: '日志级别',
          content: '日志内容'
        }
      },
      taskCenter: {
        hero: {
          title: '统一任务中心',
          description: '统一查看 Deploy、数据库连通性检测和 Traffic 动作任务。'
        },
        filters: {
          keyword: '关键词',
          status: '状态',
          taskType: '任务类型',
          startedFrom: '开始时间起',
          startedTo: '开始时间止',
          search: '搜索',
          reset: '重置'
        },
        taskTypes: {
          deploy: 'Deploy',
          databaseConnectivity: '数据库连通性检测',
          trafficAction: 'Traffic 动作'
        },
        table: {
          title: '统一任务列表',
          taskName: '任务名称',
          taskType: '任务类型',
          triggeredBy: '发起人',
          startedAt: '开始时间',
          finishedAt: '结束时间',
          summary: '摘要',
          status: '状态'
        },
        actions: {
          openTaskDetail: '查看任务详情',
          openSourceDetail: '查看原始详情',
          openTaskTracking: '查看完整追踪'
        },
        tracking: {
          hero: {
            title: '任务完整追踪',
            description: '查看统一任务的基础信息、状态时间线、日志摘要和原模块入口。'
          },
          basicInfo: { title: '基础信息' },
          timeline: { title: '状态时间线' },
          logSummary: { title: '日志摘要' },
          sourceLinks: { title: '原模块入口' },
          degraded: '该任务按历史数据现状降级展示，部分追踪信息可能不可用。'
        },
        drawer: {
          title: '任务详情',
          taskType: '任务类型',
          taskName: '任务名称',
          triggeredBy: '发起人',
          startedAt: '开始时间',
          finishedAt: '结束时间',
          summary: '摘要',
          errorSummary: '失败原因',
          detailPreview: '详情预览'
        }
      },
      trafficController: {
        hero: {
          title: '流量控制',
          description: '当前页面提供有限可用的 Traffic MVP，支持基于 REST 的 weighted routing 预览、应用和回滚。'
        },
        tags: {
          policiesLive: '3 条策略生效中',
          previewPolicy: '1 条策略预览中'
        },
        summary: {
          policiesEnabled: {
            label: '策略记录',
            desc: '当前控制器加载到的策略记录，包含受支持与不受支持的行'
          },
          canaryReleases: {
            label: '可预览策略',
            desc: '当前处于预览中或满足 MVP 预览边界的策略记录'
          },
          rollbackReady: {
            label: '可回滚覆盖率',
            desc: '当前持有外部流量服务真实 rollbackToken 的记录占比'
          }
        },
        table: {
          title: '策略快照',
          application: '应用',
          strategy: '策略',
          scope: '范围',
          trafficRatio: '流量比例',
          owner: '归属',
          status: '状态',
          operation: '操作'
        },
        actions: {
          preview: '预览',
          apply: '应用',
          rollback: '回滚'
        },
        messages: {
          latestAction: '最近一次流量动作',
          notReadyWarning:
            '当前 Traffic MVP 仅支持 REST 插件和 weighted routing。NGINX、blue-green、header canary 仍不在本次发布范围内。',
          pluginNotReady: '插件未就绪',
          pluginNotSupported: '当前版本暂不支持该插件',
          strategyNotSupported: '当前版本暂不支持该策略',
          rollbackTokenMissing: '缺少回滚令牌',
          actionFailed: '流量动作执行失败',
          previewSuccess: '流量策略预览成功',
          applySuccess: '流量策略应用成功',
          rollbackSuccess: '流量策略回滚成功'
        }
      },
      systemUser: {
        hero: {
          title: '用户管理',
          description: '维护平台操作者、团队权限与登录姿态，支持新增与编辑系统用户。'
        },
        tags: {
          rbacEnabled: 'RBAC 已启用',
          userUnderReview: '1 位用户待审核'
        },
        summary: {
          users: {
            label: '用户数',
            desc: '具备平台访问权限的运维与业务团队成员'
          },
          admins: {
            label: '管理员',
            desc: '具备路由与发布高权限的用户'
          },
          activeToday: {
            label: '今日活跃',
            desc: '最近 24 小时内登录过的用户'
          }
        },
        actions: {
          create: '新增用户',
          edit: '编辑',
          refresh: '刷新',
          save: '保存'
        },
        form: {
          titleCreate: '新增用户',
          titleEdit: '编辑用户',
          userName: '用户名',
          password: '密码',
          phone: '手机号',
          team: '团队',
          loginType: '登录方式',
          status: '状态',
          roles: '角色',
          placeholders: {
            userName: '请输入用户名，例如 ops-manager',
            passwordCreate: '请输入登录密码',
            passwordEdit: '可选，不填写则保持原密码',
            phone: '请输入 11 位手机号',
            roles: '请选择至少一个角色'
          }
        },
        messages: {
          fillRequired: '请先填写用户名、手机号、团队、登录方式、状态和角色',
          phoneInvalid: '手机号格式不正确',
          createSuccess: '用户创建成功',
          updateSuccess: '用户更新成功'
        },
        table: {
          title: '访问快照',
          user: '用户',
          role: '角色',
          team: '团队',
          loginType: '登录方式',
          lastLogin: '最近登录',
          status: '状态',
          operation: '操作'
        }
      }
    }
  },
  form: {
    required: '不能为空',
    userName: {
      required: '请输入用户名',
      invalid: '用户名格式不正确'
    },
    phone: {
      required: '请输入手机号',
      invalid: '手机号格式不正确'
    },
    pwd: {
      required: '请输入密码',
      invalid: "密码格式不正确，6-18位字符，包含字母、数字、下划线、{'@'}"
    },
    confirmPwd: {
      required: '请输入确认密码',
      invalid: '两次输入密码不一致'
    },
    code: {
      required: '请输入验证码',
      invalid: '验证码格式不正确'
    },
    email: {
      required: '请输入邮箱',
      invalid: '邮箱格式不正确'
    }
  },
  dropdown: {
    closeCurrent: '关闭',
    closeOther: '关闭其它',
    closeLeft: '关闭左侧',
    closeRight: '关闭右侧',
    closeAll: '关闭所有',
    pin: '固定标签',
    unpin: '取消固定'
  },
  icon: {
    themeConfig: '主题配置',
    themeSchema: '主题模式',
    lang: '切换语言',
    fullscreen: '全屏',
    fullscreenExit: '退出全屏',
    reload: '刷新页面',
    collapse: '折叠菜单',
    expand: '展开菜单',
    pin: '固定',
    unpin: '取消固定'
  },
  datatable: {
    itemCount: '共 {total} 条',
    fixed: {
      left: '左固定',
      right: '右固定',
      unFixed: '取消固定'
    }
  }
};

export default local;
