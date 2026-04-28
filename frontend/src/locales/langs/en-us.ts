const local: App.I18n.Schema = {
  system: {
    title: 'EnvOps',
    updateTitle: 'System Version Update Notification',
    updateContent: 'A new version of the system has been detected. Do you want to refresh the page immediately?',
    updateConfirm: 'Refresh immediately',
    updateCancel: 'Later'
  },
  common: {
    action: 'Action',
    add: 'Add',
    addSuccess: 'Add Success',
    backToHome: 'Back to home',
    batchDelete: 'Batch Delete',
    cancel: 'Cancel',
    close: 'Close',
    check: 'Check',
    selectAll: 'Select All',
    expandColumn: 'Expand Column',
    columnSetting: 'Column Setting',
    config: 'Config',
    confirm: 'Confirm',
    delete: 'Delete',
    deleteSuccess: 'Delete Success',
    confirmDelete: 'Are you sure you want to delete?',
    edit: 'Edit',
    warning: 'Warning',
    error: 'Error',
    index: 'Index',
    keywordSearch: 'Please enter keyword',
    logout: 'Logout',
    logoutConfirm: 'Are you sure you want to log out?',
    lookForward: 'Coming soon',
    modify: 'Modify',
    modifySuccess: 'Modify Success',
    noData: 'No Data',
    operate: 'Operate',
    pleaseCheckValue: 'Please check whether the value is valid',
    refresh: 'Refresh',
    reset: 'Reset',
    search: 'Search',
    switch: 'Switch',
    tip: 'Tip',
    trigger: 'Trigger',
    update: 'Update',
    updateSuccess: 'Update Success',
    userCenter: 'User Center',
    yesOrNo: {
      yes: 'Yes',
      no: 'No'
    }
  },
  request: {
    logout: 'Logout user after request failed',
    logoutMsg: 'User status is invalid, please log in again',
    logoutWithModal: 'Pop up modal after request failed and then log out user',
    logoutWithModalMsg: 'User status is invalid, please log in again',
    tokenExpired: 'The requested token has expired'
  },
  permission: {
    missingAction: 'Missing permission: {permission}'
  },
  theme: {
    themeDrawerTitle: 'Theme Configuration',
    tabs: {
      appearance: 'Appearance',
      layout: 'Layout',
      general: 'General',
      preset: 'Preset'
    },
    appearance: {
      themeSchema: {
        title: 'Theme Schema',
        light: 'Light',
        dark: 'Dark',
        auto: 'Follow System'
      },
      grayscale: 'Grayscale',
      colourWeakness: 'Colour Weakness',
      themeColor: {
        title: 'Theme Color',
        primary: 'Primary',
        info: 'Info',
        success: 'Success',
        warning: 'Warning',
        error: 'Error',
        followPrimary: 'Follow Primary'
      },
      themeRadius: {
        title: 'Theme Radius'
      },
      recommendColor: 'Apply Recommended Color Algorithm',
      recommendColorDesc: 'The recommended color algorithm refers to',
      preset: {
        title: 'Theme Presets',
        apply: 'Apply',
        applySuccess: 'Preset applied successfully',
        default: {
          name: 'Default Preset',
          desc: 'Default theme preset with balanced settings'
        },
        dark: {
          name: 'Dark Preset',
          desc: 'Dark theme preset for night time usage'
        },
        compact: {
          name: 'Compact Preset',
          desc: 'Compact layout preset for small screens'
        },
        azir: {
          name: "Azir's Preset",
          desc: 'It is a cold and elegant preset that Azir likes'
        }
      }
    },
    layout: {
      layoutMode: {
        title: 'Layout Mode',
        vertical: 'Vertical Mode',
        horizontal: 'Horizontal Mode',
        'vertical-mix': 'Vertical Mix Mode',
        'vertical-hybrid-header-first': 'Left Hybrid Header-First',
        'top-hybrid-sidebar-first': 'Top-Hybrid Sidebar-First',
        'top-hybrid-header-first': 'Top-Hybrid Header-First',
        vertical_detail: 'Vertical menu layout, with the menu on the left and content on the right.',
        'vertical-mix_detail':
          'Vertical mix-menu layout, with the primary menu on the dark left side and the secondary menu on the lighter left side.',
        'vertical-hybrid-header-first_detail':
          'Left hybrid layout, with the primary menu at the top, the secondary menu on the dark left side, and the tertiary menu on the lighter left side.',
        horizontal_detail: 'Horizontal menu layout, with the menu at the top and content below.',
        'top-hybrid-sidebar-first_detail':
          'Top hybrid layout, with the primary menu on the left and the secondary menu at the top.',
        'top-hybrid-header-first_detail':
          'Top hybrid layout, with the primary menu at the top and the secondary menu on the left.'
      },
      tab: {
        title: 'Tab Settings',
        visible: 'Tab Visible',
        cache: 'Tag Bar Info Cache',
        cacheTip: 'Keep the tab bar information after leaving the page',
        height: 'Tab Height',
        mode: {
          title: 'Tab Mode',
          slider: 'Slider',
          chrome: 'Chrome',
          button: 'Button'
        },
        closeByMiddleClick: 'Close Tab by Middle Click',
        closeByMiddleClickTip: 'Enable closing tabs by clicking with the middle mouse button'
      },
      header: {
        title: 'Header Settings',
        height: 'Header Height',
        breadcrumb: {
          visible: 'Breadcrumb Visible',
          showIcon: 'Breadcrumb Icon Visible'
        }
      },
      sider: {
        title: 'Sider Settings',
        inverted: 'Dark Sider',
        width: 'Sider Width',
        collapsedWidth: 'Sider Collapsed Width',
        mixWidth: 'Mix Sider Width',
        mixCollapsedWidth: 'Mix Sider Collapse Width',
        mixChildMenuWidth: 'Mix Child Menu Width',
        autoSelectFirstMenu: 'Auto Select First Submenu',
        autoSelectFirstMenuTip:
          'When a first-level menu is clicked, the first submenu is automatically selected and navigated to the deepest level'
      },
      footer: {
        title: 'Footer Settings',
        visible: 'Footer Visible',
        fixed: 'Fixed Footer',
        height: 'Footer Height',
        right: 'Right Footer'
      },
      content: {
        title: 'Content Area Settings',
        scrollMode: {
          title: 'Scroll Mode',
          tip: 'The theme scroll only scrolls the main part, the outer scroll can carry the header and footer together',
          wrapper: 'Wrapper',
          content: 'Content'
        },
        page: {
          animate: 'Page Animate',
          mode: {
            title: 'Page Animate Mode',
            fade: 'Fade',
            'fade-slide': 'Slide',
            'fade-bottom': 'Fade Zoom',
            'fade-scale': 'Fade Scale',
            'zoom-fade': 'Zoom Fade',
            'zoom-out': 'Zoom Out',
            none: 'None'
          }
        },
        fixedHeaderAndTab: 'Fixed Header And Tab'
      }
    },
    general: {
      title: 'General Settings',
      watermark: {
        title: 'Watermark Settings',
        visible: 'Watermark Full Screen Visible',
        text: 'Custom Watermark Text',
        enableUserName: 'Enable User Name Watermark',
        enableTime: 'Show Current Time',
        timeFormat: 'Time Format'
      },
      multilingual: {
        title: 'Multilingual Settings',
        visible: 'Display multilingual button'
      },
      globalSearch: {
        title: 'Global Search Settings',
        visible: 'Display GlobalSearch button'
      }
    },
    configOperation: {
      copyConfig: 'Copy Config',
      copySuccessMsg: 'Copy Success, Please replace the variable "themeSettings" in "src/theme/settings.ts"',
      resetConfig: 'Reset Config',
      resetSuccessMsg: 'Reset Success'
    }
  },
  route: {
    login: 'Login',
    403: 'No Permission',
    404: 'Page Not Found',
    500: 'Server Error',
    'iframe-page': 'Iframe',
    home: 'Dashboard',
    asset: 'Asset Center',
    asset_host: 'Host Management',
    asset_group: 'Group Management',
    asset_tag: 'Tag Management',
    asset_credential: 'Credential Management',
    asset_database: 'Database Resources',
    monitor: 'Monitoring',
    'monitor_detect-task': 'Detection Tasks',
    monitor_metric: 'Metric Snapshot',
    app: 'Applications',
    app_definition: 'Application Definitions',
    app_version: 'Versions',
    app_package: 'Packages',
    'app_config-template': 'Config Templates',
    'app_script-template': 'Script Templates',
    deploy: 'Deployment',
    deploy_task: 'Deployment Tasks',
    task: 'Task Orchestration',
    task_center: 'Task Center',
    'task_tracking_[id]': 'Task Tracking',
    traffic: 'Traffic Governance',
    traffic_controller: 'Traffic Controller',
    system: 'System',
    system_user: 'User Management',
    system_rbac: 'Permission Management'
  },
  page: {
    login: {
      common: {
        loginOrRegister: 'Login / Register',
        userNamePlaceholder: 'Please enter user name',
        phonePlaceholder: 'Please enter phone number',
        codePlaceholder: 'Please enter verification code',
        passwordPlaceholder: 'Please enter password',
        confirmPasswordPlaceholder: 'Please enter password again',
        codeLogin: 'Verification code login',
        confirm: 'Confirm',
        back: 'Back',
        validateSuccess: 'Verification passed',
        loginSuccess: 'Login successfully',
        welcomeBack: 'Welcome back, {userName} !'
      },
      pwdLogin: {
        title: 'Password Login',
        rememberMe: 'Remember me',
        forgetPassword: 'Forget password?',
        register: 'Register',
        otherAccountLogin: 'Other Account Login',
        otherLoginMode: 'Other Login Mode',
        superAdmin: 'Super Admin',
        admin: 'Admin',
        user: 'User'
      },
      codeLogin: {
        title: 'Verification Code Login',
        getCode: 'Get verification code',
        reGetCode: 'Reacquire after {time}s',
        sendCodeSuccess: 'Verification code sent successfully',
        demoCodeHint: 'In demo mode, use the last 6 digits of the phone number',
        imageCodePlaceholder: 'Please enter image verification code'
      },
      register: {
        title: 'Register',
        agreement: 'I have read and agree to',
        protocol: '《User Agreement》',
        policy: '《Privacy Policy》'
      },
      resetPwd: {
        title: 'Reset Password'
      },
      bindWeChat: {
        title: 'Bind WeChat'
      }
    },
    home: {
      branchDesc:
        'For the convenience of everyone in developing and updating the merge, we have streamlined the code of the main branch, only retaining the homepage menu, and the rest of the content has been moved to the example branch for maintenance. The preview address displays the content of the example branch.',
      greeting: 'Good morning, {userName}, today is another day full of vitality!',
      weatherDesc: 'Today is cloudy to clear, 20℃ - 25℃!',
      projectCount: 'Project Count',
      todo: 'Todo',
      message: 'Message',
      downloadCount: 'Download Count',
      registerCount: 'Register Count',
      schedule: 'Work and rest Schedule',
      study: 'Study',
      work: 'Work',
      rest: 'Rest',
      entertainment: 'Entertainment',
      visitCount: 'Visit Count',
      turnover: 'Turnover',
      dealCount: 'Deal Count',
      projectNews: {
        title: 'Project News',
        moreNews: 'More News',
        desc1: 'EnvOps has completed the first frontend shell and navigation scaffold.',
        desc2: 'Asset, monitor, app, deploy, traffic and system pages now load through dynamic routes.',
        desc3: 'The test environment is now wired to the current EnvOps backend address.',
        desc4: 'The frontend no longer depends on the missing isRouteExist backend endpoint.',
        desc5: 'Task 3 page copy is now localized for both Chinese and English.'
      },
      creativity: 'Creativity'
    },
    app: {
      common: {
        save: 'Save',
        appCode: 'App Code',
        appName: 'App Name',
        appType: 'App Type',
        runtimeType: 'Runtime Type',
        deployMode: 'Deploy Mode',
        defaultPort: 'Default Port',
        healthCheckPath: 'Health Check Path',
        description: 'Description',
        status: 'Status',
        updatedAt: 'Updated At'
      },
      definition: {
        desc: 'Maintain application definitions against the real backend contracts.',
        listTitle: 'Application List',
        detailTitle: 'Application Detail',
        formTitleCreate: 'Create Application Definition',
        formTitleEdit: 'Edit Application Definition'
      },
      version: {
        desc: 'Browse versions by application and maintain package and template bindings.',
        listTitle: 'Version List',
        currentApp: 'Current App',
        selectApp: 'Select App',
        formTitleCreate: 'Create App Version',
        formTitleEdit: 'Edit App Version',
        versionNo: 'Version No.',
        packageId: 'Package',
        configTemplateId: 'Config Template',
        scriptTemplateId: 'Script Template',
        changelog: 'Changelog'
      },
      package: {
        desc: 'Register, upload and delete artifacts through the package APIs.',
        uploadTitle: 'Upload Package',
        listTitle: 'Package List',
        packageName: 'Package Name',
        packageType: 'Package Type',
        storageType: 'Storage Type',
        file: 'File',
        selectFile: 'Select File',
        uploadAction: 'Upload',
        uploadFileRequired: 'Please select a file to upload first.',
        filePath: 'File Path',
        fileSize: 'File Size',
        fileHash: 'File Hash'
      },
      configTemplate: {
        desc: 'Maintain deploy-time configuration content through the config template APIs.',
        listTitle: 'Config Template List',
        formTitleCreate: 'Create Config Template',
        formTitleEdit: 'Edit Config Template',
        templateCode: 'Template Code',
        templateName: 'Template Name',
        templateContent: 'Template Content',
        renderEngine: 'Render Engine'
      },
      scriptTemplate: {
        desc: 'Maintain install and bootstrap scripts through the script template APIs.',
        listTitle: 'Script Template List',
        formTitleCreate: 'Create Script Template',
        formTitleEdit: 'Edit Script Template',
        templateCode: 'Template Code',
        templateName: 'Template Name',
        scriptType: 'Script Type',
        scriptContent: 'Script Content'
      }
    },
    envops: {
      common: {
        environment: {
          production: 'Production',
          staging: 'Staging',
          sandbox: 'Sandbox'
        },
        batch: {
          canary20: '20% canary',
          fullRelease: 'full release',
          canary10: '10% canary'
        },
        owner: {
          envops: 'EnvOps',
          release: 'Release Team',
          sre: 'SRE',
          qa: 'QA Team'
        },
        team: {
          envops: 'EnvOps',
          release: 'Release Team',
          traffic: 'Traffic',
          qa: 'QA',
          fintech: 'FinTech',
          platform: 'Platform',
          sre: 'SRE'
        },
        status: {
          running: 'Running',
          pendingApproval: 'Pending approval',
          rollbackRequired: 'Rollback required',
          success: 'Success',
          warning: 'Warning',
          timeout: 'Timeout',
          online: 'Online',
          offline: 'Offline',
          pending: 'Pending',
          failed: 'Failed',
          managed: 'Managed',
          draft: 'Draft',
          review: 'Review',
          queued: 'Queued',
          cancelled: 'Cancelled',
          rejected: 'Rejected',
          enabled: 'Enabled',
          preview: 'Preview',
          standby: 'Standby',
          active: 'Active',
          disabled: 'Disabled'
        },
        schedule: {
          every10Min: 'Every 10 min',
          everyHour: 'Every hour',
          every5Min: 'Every 5 min',
          every15Min: 'Every 15 min'
        },
        taskType: {
          inspection: 'Inspection',
          deploy: 'Deploy',
          traffic: 'Traffic',
          assetSync: 'Asset Sync'
        },
        strategy: {
          headerCanary: 'Header canary',
          blueGreen: 'Blue-green',
          weightedRouting: 'Weighted routing',
          emergencyRollback: 'Emergency rollback'
        },
        role: {
          superAdmin: 'Super Admin',
          platformAdmin: 'Platform Admin',
          releaseManager: 'Release Manager',
          trafficOwner: 'Traffic Owner',
          observer: 'Observer'
        },
        loginType: {
          passwordOtp: 'Password + OTP',
          password: 'Password',
          sso: 'SSO'
        }
      },
      home: {
        hero: {
          title: 'EnvOps Control Plane',
          description:
            'This is the first frontend shell for assets, release tasks, traffic policies and platform operations.',
          descriptionWithUser:
            'Welcome back, {userName}. This is the first frontend shell for assets, release tasks, traffic policies and platform operations.'
        },
        tags: {
          dynamicRoutesReady: 'Dynamic routes ready',
          vitestHarnessEnabled: 'Vitest harness enabled'
        },
        summary: {
          managedApplications: {
            label: 'Managed Applications',
            desc: 'Application definitions ready for delivery orchestration'
          },
          onlineHosts: {
            label: 'Online Hosts',
            desc: 'Instances continuously reporting healthy heartbeats'
          },
          runningTasks: {
            label: 'Running Tasks',
            desc: 'Cross-domain jobs executing across the platform'
          },
          trafficPolicies: {
            label: 'Traffic Policies',
            desc: 'Release rules and control strategies currently enabled'
          }
        },
        sections: {
          releasePipelineFocus: 'Release pipeline focus',
          operatorFocus: 'Operator focus',
          inspectionHealth: 'Inspection health',
          platformReadiness: 'Platform readiness'
        },
        releaseTable: {
          taskId: 'Task ID',
          application: 'Application',
          environment: 'Environment',
          batch: 'Batch',
          owner: 'Owner',
          status: 'Status'
        },
        operatorFocusDesc: 'Updated for the first EnvOps shell iteration',
        focusList: {
          item1: 'Review the asset-sync canary failure and prepare rollback notes.',
          item2: 'Confirm the approval window for the payment-gateway production release.',
          item3: 'Re-run the traffic-canary-guard detector after gateway config sync.',
          item4: 'Complete access provisioning for the first EnvOps users and roles.'
        },
        inspectionTable: {
          task: 'Task',
          target: 'Target',
          schedule: 'Schedule',
          lastResult: 'Last Result'
        },
        readiness: {
          frontendShell: {
            label: 'Frontend shell',
            value: 'Asset, monitor, app, deploy, task, traffic and user page shells are in place.'
          },
          routingMode: {
            label: 'Routing mode',
            value: 'Dynamic route initialization and menu injection are wired end to end.'
          },
          qualityGate: {
            label: 'Quality gate',
            value: 'Vitest route-store tests are wired into the project scripts.'
          },
          nextSlice: {
            label: 'Next slice',
            value: 'Continue by connecting backend queries and workflow mutations on top of the current shell.'
          }
        }
      },
      assetHost: {
        hero: {
          title: 'Host Inventory',
          description:
            'Show managed hosts, add newly onboarded hosts and jump straight into metric details for hosts with monitor snapshots.'
        },
        tags: {
          cmdbSynchronized: 'CMDB synchronized',
          maintenanceWindow: '1 maintenance window today'
        },
        summary: {
          managedHosts: {
            label: 'Managed Hosts',
            desc: 'Total hosts currently managed in the asset inventory'
          },
          online: {
            label: 'Online Hosts',
            desc: 'Total hosts in the asset inventory whose status is online'
          },
          pendingMaintenance: {
            label: 'Warning Hosts',
            desc: 'Total hosts in the asset inventory whose status is warning'
          }
        },
        form: {
          title: 'Add Host',
          hostName: 'Host Name',
          ipAddress: 'IP Address',
          environment: 'Environment',
          clusterName: 'Cluster Name',
          ownerName: 'Owner Team',
          status: 'Status',
          lastHeartbeat: 'Last Heartbeat',
          placeholders: {
            hostName: 'Example: host-sbx-01',
            ipAddress: 'Example: 10.60.1.20',
            clusterName: 'Example: cn-shenzhen-a',
            ownerName: 'Example: Asset Team',
            lastHeartbeat: 'Optional ISO timestamp, e.g. 2026-04-16T11:22:33'
          },
          actions: {
            create: 'Onboard Host'
          }
        },
        messages: {
          fillRequired: 'Please enter host name, IP address, environment, cluster, owner team and status first',
          createSuccess: 'Host onboarded successfully'
        },
        table: {
          title: 'Host Snapshot',
          host: 'Host',
          ip: 'IP',
          environment: 'Environment',
          cluster: 'Cluster',
          owner: 'Owner',
          status: 'Status',
          lastHeartbeat: 'Last Heartbeat',
          latestMetric: 'Latest Metric Snapshot',
          operation: 'Action',
          viewMetrics: 'View Metrics'
        }
      },
      assetGroup: {
        hero: {
          title: 'Group Management',
          description: 'Review asset ownership and managed scope from the group perspective.'
        },
        tags: {
          groups: 'Groups {count}',
          hosts: 'Hosts {count}'
        },
        table: {
          title: 'Group List',
          group: 'Group',
          description: 'Description',
          hostCount: 'Host Count'
        }
      },
      assetTag: {
        hero: {
          title: 'Tag Management',
          description: 'Use standardized tags to classify operating systems, usage and maintenance attributes.'
        },
        tags: {
          total: 'Tags {count}'
        },
        table: {
          title: 'Tag Catalog',
          tag: 'Tag',
          color: 'Color',
          description: 'Description'
        }
      },
      assetCredential: {
        hero: {
          title: 'Credential Management',
          description: 'Maintain credential entries for host access and automated execution in one place.'
        },
        tags: {
          total: 'Total {count}',
          passwords: 'Passwords {count}',
          keys: 'Keys {count}',
          tokens: 'Tokens {count}'
        },
        types: {
          sshPassword: 'SSH Password',
          sshKey: 'SSH Key',
          apiToken: 'API Token'
        },
        form: {
          title: 'Create Credential',
          name: 'Name',
          type: 'Type',
          username: 'Username',
          secret: 'Secret or Key Content',
          description: 'Description',
          placeholders: {
            name: 'Example: prod-root-password',
            username: 'Example: root / deploy',
            secret: 'Enter the secret, private key or token',
            description: 'Optional notes about the usage scope'
          },
          actions: {
            create: 'Create Credential'
          }
        },
        table: {
          title: 'Existing Credentials',
          name: 'Name',
          type: 'Type',
          username: 'Username',
          description: 'Description',
          createdAt: 'Created At'
        },
        messages: {
          fillNameAndType: 'Please enter the credential name and type first',
          createSuccess: 'Credential created successfully'
        }
      },
      assetDatabase: {
        hero: {
          title: 'Database Resources',
          description:
            'Register database assets, bind them to hosts and credentials, and maintain a searchable catalog.'
        },
        tags: {
          registryReady: 'Registry ready',
          connectivityCheckReady: 'Live connectivity checks available',
          warningManual: 'warning remains manual'
        },
        filters: {
          keywordPlaceholder: 'Search by database, instance, owner or host',
          environmentPlaceholder: 'Filter by environment',
          databaseTypePlaceholder: 'Filter by database type',
          lifecycleStatusPlaceholder: 'Filter by lifecycle status',
          connectivityStatusPlaceholder: 'Filter by connectivity status'
        },
        summary: {
          managedDatabases: {
            label: 'Managed Databases',
            desc: 'Database assets whose lifecycle status is managed'
          },
          warningDatabases: {
            label: 'Warning Databases',
            desc: 'Database assets whose connectivity status is warning'
          },
          onlineDatabases: {
            label: 'Online Databases',
            desc: 'Database assets whose connectivity status is online'
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
          unknown: 'Unknown'
        },
        actions: {
          create: 'Create Database',
          edit: 'Edit',
          save: 'Save',
          check: 'Check',
          checkSelected: 'Check selected',
          checkCurrentPage: 'Check current page',
          checkAllFiltered: 'Check all filtered results',
          closeResult: 'Close result'
        },
        form: {
          titleCreate: 'Create Database Resource',
          titleEdit: 'Edit Database Resource',
          databaseName: 'Database Name',
          databaseType: 'Database Type',
          environment: 'Environment',
          host: 'Host',
          port: 'Port',
          instanceName: 'Instance Name',
          credential: 'Credential',
          ownerName: 'Owner Team',
          lifecycleStatus: 'Lifecycle Status',
          connectivityStatus: 'Connectivity Status',
          connectionUsername: 'Connection Username',
          connectionPassword: 'Connection Password',
          lastCheckedAt: 'Last Checked At',
          description: 'Description',
          placeholders: {
            databaseName: 'Example: order_prod',
            host: 'Select a host',
            port: 'Example: 3306',
            instanceName: 'Example: mysql-prd-a',
            credential: 'Optional. Select a database login credential',
            ownerName: 'Example: Platform DBA',
            connectionUsername: 'Example: orders_app',
            connectionPassword: 'Leave blank to keep the saved password',
            lastCheckedAt: 'Optional ISO timestamp, e.g. 2026-04-18T12:00:00',
            description: 'Optional notes for ownership, purpose or risk'
          }
        },
        table: {
          title: 'Database Catalog',
          database: 'Database',
          type: 'Type',
          environment: 'Environment',
          host: 'Host',
          port: 'Port',
          owner: 'Owner',
          lifecycleStatus: 'Lifecycle',
          connectivityStatus: 'Connectivity',
          lastCheckedAt: 'Last Checked',
          operation: 'Action'
        },
        messages: {
          fillRequired:
            'Please enter database name, type, environment, host, port, owner team, lifecycle status and connectivity status first',
          fillConnectionPair: 'Enter both connection username and connection password when saving credentials',
          fillConnectionUsername: 'Enter connection username before changing the connection password',
          checkFinished: 'Database connectivity check completed',
          createSuccess: 'Database resource created successfully',
          updateSuccess: 'Database resource updated successfully'
        },
        result: {
          title: 'Check Result',
          total: 'Total',
          success: 'Success',
          failed: 'Failed',
          skipped: 'Skipped',
          message: 'Message'
        }
      },
      monitorDetectTask: {
        hero: {
          title: 'Detection Tasks',
          description: 'Use the live monitor task API to create tasks, run them manually and review the latest results.'
        },
        tags: {
          healthyCount: '{count} healthy',
          attentionCount: '{count} need attention'
        },
        summary: {
          scheduledTasks: {
            label: 'Scheduled Tasks',
            desc: 'Inspection jobs currently registered'
          },
          successRate: {
            label: 'Success Rate',
            desc: 'Calculated live from the current task list'
          },
          needsAttention: {
            label: 'Needs Attention',
            desc: 'Tasks with failed, timed-out or partial-success results'
          }
        },
        form: {
          title: 'Create Detection Task',
          taskName: 'Task Name',
          taskNamePlaceholder: 'Enter task name',
          host: 'Target Host',
          hostPlaceholder: 'Select target host',
          schedule: 'Schedule',
          manualSchedule: 'Manual Run'
        },
        actions: {
          create: 'Create Task',
          execute: 'Run Now'
        },
        messages: {
          fillNameAndHost: 'Enter the task name and select a target host first',
          createSuccess: 'Detection task created successfully',
          executeSuccess: 'Detection task finished running'
        },
        table: {
          title: 'Task Queue',
          task: 'Task',
          target: 'Target',
          schedule: 'Schedule',
          lastRun: 'Last Run',
          result: 'Result',
          operation: 'Action'
        }
      },
      monitorMetric: {
        hero: {
          title: 'Metric Snapshot',
          description:
            'Show the latest reported facts for the selected host and complete the asset-to-monitor detail flow.'
        },
        tags: {
          host: 'Host #{id}'
        },
        summary: {
          cpuCores: {
            label: 'CPU Cores',
            desc: 'CPU capacity from the latest fact snapshot'
          },
          memory: {
            label: 'Memory',
            desc: 'Memory capacity from the latest fact snapshot'
          },
          osName: {
            label: 'Operating System',
            desc: 'OS version reported by the latest host fact snapshot'
          },
          agentVersion: {
            label: 'Agent Version',
            desc: 'Agent version used by the latest host report'
          }
        },
        detail: {
          title: 'Fact Details',
          item: 'Item',
          value: 'Value',
          hostname: 'Hostname',
          osName: 'Operating System',
          kernel: 'Kernel Version',
          cpuCores: 'CPU Cores',
          memory: 'Memory',
          agentVersion: 'Agent Version',
          collectedAt: 'Collected At'
        }
      },
      appDefinition: {
        hero: {
          title: 'Application Definitions',
          description:
            'Define application ownership, runtimes and delivery metadata before release automation takes over.'
        },
        tags: {
          definitionSyncEnabled: 'Definition sync enabled',
          draftsAwaitingReview: '2 drafts awaiting review'
        },
        summary: {
          applications: {
            label: 'Applications',
            desc: 'Business services currently registered in EnvOps'
          },
          runtimeProfiles: {
            label: 'Runtime Profiles',
            desc: 'Reusable build and release templates'
          },
          teamsCovered: {
            label: 'Teams Covered',
            desc: 'Owning teams mapped to app definitions'
          }
        },
        table: {
          title: 'Definition Catalog',
          application: 'Application',
          owner: 'Owner',
          runtime: 'Runtime',
          repository: 'Repository',
          currentVersion: 'Current Version',
          status: 'Status'
        }
      },
      deployTask: {
        hero: {
          title: 'Deployment Tasks',
          description: 'Track approvals, rollout batches and execution results for the first delivery pipeline slice.'
        },
        tags: {
          runningCanary: '1 running canary',
          rollbackRequired: 'Rollback required'
        },
        summary: {
          pendingApproval: {
            label: 'Pending Approval',
            desc: 'Release tasks waiting for manual confirmation'
          },
          inProgress: {
            label: 'In Progress',
            desc: 'Tasks currently executing across target clusters'
          },
          failedIn24h: {
            label: 'Failed in 24h',
            desc: 'Tasks that need rollback analysis or retry'
          }
        },
        filters: {
          status: 'Task Status',
          taskType: 'Task Type',
          application: 'Application',
          environment: 'Environment',
          keyword: 'Keyword',
          createdRange: 'Created Range',
          search: 'Search',
          reset: 'Reset',
          taskTypeInstall: 'Install',
          taskTypeUpgrade: 'Upgrade',
          taskTypeRollback: 'Rollback'
        },
        sorting: {
          createdAt: 'Created At',
          updatedAt: 'Updated At',
          taskNo: 'Task ID',
          status: 'Status'
        },
        table: {
          title: 'Release Queue',
          taskId: 'Task ID',
          application: 'Application',
          environment: 'Environment',
          batch: 'Batch',
          operator: 'Operator',
          status: 'Status'
        },
        actions: {
          detail: 'Detail',
          create: 'Create Task',
          execute: 'Execute',
          retry: 'Retry',
          rollback: 'Rollback',
          approve: 'Approve',
          reject: 'Reject',
          cancel: 'Cancel Task'
        },
        create: {
          title: 'Create Deploy Task',
          taskName: 'Task Name',
          taskNamePlaceholder: 'Enter task name',
          taskType: 'Task Type',
          app: 'Application',
          appPlaceholder: 'Select application',
          version: 'Version',
          versionPlaceholder: 'Select version',
          environment: 'Environment',
          hosts: 'Target Hosts',
          hostsPlaceholder: 'Select hosts',
          batchStrategy: 'Batch Strategy',
          batchStrategyAll: 'All',
          batchStrategyRolling: 'Rolling',
          batchSize: 'Batch Size',
          batchSizePlaceholder: 'Enter batch size for rolling deployments',
          deployDir: 'Deploy Directory',
          deployDirPlaceholder: 'Enter deploy directory, e.g. /data/apps/order-service',
          sshUser: 'SSH User',
          sshUserPlaceholder: 'Enter the SSH login user, e.g. deploy',
          sshPort: 'SSH Port',
          sshPortPlaceholder: 'Enter the SSH port, default 22',
          privateKeyPath: 'Private Key Path',
          privateKeyPathPlaceholder: 'Enter the private key path, e.g. /data/keys/release.pem',
          remoteBaseDir: 'Remote Release Root',
          remoteBaseDirPlaceholder: 'Enter the remote root, e.g. /opt/envops/releases',
          rollbackCommand: 'Rollback Command',
          rollbackCommandPlaceholder: 'Optional. When set, rollback tasks run this command directly',
          validation: {
            taskNameRequired: 'Please enter the task name',
            appRequired: 'Please select an application',
            versionRequired: 'Please select a version',
            environmentRequired: 'Please select an environment',
            hostsRequired: 'Please select at least one host',
            deployDirRequired: 'Please enter the deploy directory',
            sshUserRequired: 'Please enter the SSH user',
            sshPortInvalid: 'SSH port must be greater than 0',
            privateKeyPathRequired: 'Please enter the private key path',
            remoteBaseDirRequired: 'Please enter the remote release root',
            batchSizeRequired: 'Batch size must be greater than 0 for rolling deployments'
          }
        },
        approval: {
          approveTitle: 'Approve Deploy Task',
          rejectTitle: 'Reject Deploy Task',
          comment: 'Comment',
          commentPlaceholder: 'Optional approval comment'
        },
        tabs: {
          overview: 'Overview',
          hosts: 'Hosts',
          logs: 'Logs'
        },
        progress: {
          totalHosts: 'Total Hosts',
          pendingHosts: 'Pending Hosts',
          runningHosts: 'Running Hosts',
          successHosts: 'Succeeded Hosts',
          failedHosts: 'Failed Hosts',
          cancelledHosts: 'Cancelled Hosts'
        },
        detail: {
          title: 'Task Detail',
          manualRefresh: 'Manual Refresh',
          taskId: 'Task ID',
          taskName: 'Task Name',
          taskType: 'Task Type',
          originTaskId: 'Origin Task ID',
          application: 'Application',
          version: 'Version',
          environment: 'Environment',
          batch: 'Batch',
          operator: 'Operator',
          status: 'Status',
          approvalOperator: 'Approval Operator',
          approvalComment: 'Approval Comment',
          approvalAt: 'Approval Time',
          startedAt: 'Started At',
          finishedAt: 'Finished At',
          createdAt: 'Created At',
          updatedAt: 'Updated At'
        },
        error: {
          detailLoadFailed: 'Failed to load task detail. Please try again later.',
          hostsLoadFailed: 'Failed to load host details. Please try again later.',
          logsLoadFailed: 'Failed to load execution logs. Please try again later.',
          autoRefreshFailed: 'Auto refresh failed. Please retry manually later.'
        },
        empty: {
          taskNotFound: 'Task detail was not found',
          noHosts: 'No host details available',
          noLogs: 'No execution logs available'
        },
        hosts: {
          title: 'Host Details',
          hostName: 'Host Name',
          ipAddress: 'IP Address',
          status: 'Status',
          currentStep: 'Current Step',
          startedAt: 'Started At',
          finishedAt: 'Finished At',
          errorMsg: 'Error Message'
        },
        logs: {
          title: 'Execution Logs',
          createdAt: 'Time',
          taskHostId: 'Task Host ID',
          logLevel: 'Log Level',
          content: 'Log Content'
        }
      },
      taskCenter: {
        hero: {
          title: 'Unified Task Center',
          description: 'View Deploy, Database Connectivity, and Traffic Action tasks in one place.'
        },
        filters: {
          keyword: 'Keyword',
          status: 'Status',
          taskType: 'Task Type',
          startedFrom: 'Started From',
          startedTo: 'Started To',
          search: 'Search',
          reset: 'Reset'
        },
        taskTypes: {
          deploy: 'Deploy',
          databaseConnectivity: 'Database Connectivity',
          trafficAction: 'Traffic Action'
        },
        table: {
          title: 'Unified Task List',
          taskName: 'Task Name',
          taskType: 'Task Type',
          triggeredBy: 'Triggered By',
          startedAt: 'Started At',
          finishedAt: 'Finished At',
          summary: 'Summary',
          status: 'Status'
        },
        actions: {
          openTaskDetail: 'View Task Detail',
          openSourceDetail: 'View Source Detail',
          openTaskTracking: 'View full tracking'
        },
        tracking: {
          hero: {
            title: 'Task Tracking',
            description: 'View basic information, status timeline, log summary, and source module entries.'
          },
          basicInfo: { title: 'Basic information' },
          timeline: { title: 'Status timeline' },
          logSummary: { title: 'Log summary' },
          sourceLinks: { title: 'Source module entries' },
          degraded: 'This task is shown in degraded mode based on available historical data.'
        },
        drawer: {
          title: 'Task Detail',
          taskType: 'Task Type',
          taskName: 'Task Name',
          triggeredBy: 'Triggered By',
          startedAt: 'Started At',
          finishedAt: 'Finished At',
          summary: 'Summary',
          errorSummary: 'Failure Reason',
          detailPreview: 'Detail Preview'
        }
      },
      trafficController: {
        hero: {
          title: 'Traffic Controller',
          description:
            'Execute a limited Traffic MVP with real REST-based preview, apply, and rollback for weighted routing policies.'
        },
        tags: {
          policiesLive: '3 policies live',
          previewPolicy: '1 preview policy'
        },
        summary: {
          policiesEnabled: {
            label: 'Policy Records',
            desc: 'Traffic policy records loaded into the controller, including supported and unsupported rows'
          },
          canaryReleases: {
            label: 'Preview-ready Policies',
            desc: 'Policies currently in preview or ready for preview within the MVP boundary'
          },
          rollbackReady: {
            label: 'Rollback-ready Coverage',
            desc: 'Share of records that currently hold a usable rollback token from the external traffic service'
          }
        },
        table: {
          title: 'Policy Snapshot',
          application: 'Application',
          strategy: 'Strategy',
          scope: 'Scope',
          trafficRatio: 'Traffic Ratio',
          owner: 'Owner',
          status: 'Status',
          operation: 'Action'
        },
        actions: {
          preview: 'Preview',
          apply: 'Apply',
          rollback: 'Rollback'
        },
        messages: {
          latestAction: 'Latest Traffic Action',
          notReadyWarning:
            'Traffic MVP currently supports REST plugin and weighted routing only. NGINX, blue-green, and header canary remain outside this release.',
          pluginNotReady: 'Plugin not ready',
          pluginNotSupported: 'Plugin not supported in v0.0.5',
          strategyNotSupported: 'Strategy not supported in v0.0.5',
          rollbackTokenMissing: 'Rollback token required',
          actionFailed: 'Traffic action failed',
          previewSuccess: 'Traffic policy previewed successfully',
          applySuccess: 'Traffic policy applied successfully',
          rollbackSuccess: 'Traffic policy rolled back successfully'
        }
      },
      systemUser: {
        hero: {
          title: 'User Management',
          description: 'Maintain platform operators, team permissions and login posture, with create and edit actions.'
        },
        tags: {
          rbacEnabled: 'RBAC enabled',
          userUnderReview: '1 user under review'
        },
        summary: {
          users: {
            label: 'Users',
            desc: 'Platform operators and product teams with access'
          },
          admins: {
            label: 'Admins',
            desc: 'Users with elevated routing and release permissions'
          },
          activeToday: {
            label: 'Active Today',
            desc: 'Users who logged in during the last 24 hours'
          }
        },
        actions: {
          create: 'Create User',
          edit: 'Edit',
          refresh: 'Refresh',
          save: 'Save'
        },
        roleAssignment: {
          title: 'Assign Roles',
          roles: 'Roles',
          placeholder: 'Select roles to assign',
          save: 'Save Roles',
          saveSuccess: 'User roles updated',
          loadFailed: 'Failed to load user roles. Please try again later.'
        },
        form: {
          titleCreate: 'Create User',
          titleEdit: 'Edit User',
          userName: 'User Name',
          password: 'Password',
          phone: 'Phone',
          team: 'Team',
          loginType: 'Login Type',
          status: 'Status',
          roles: 'Roles',
          placeholders: {
            userName: 'Enter a user name, for example ops-manager',
            passwordCreate: 'Enter the login password',
            passwordEdit: 'Optional. Leave blank to keep the current password',
            phone: 'Enter an 11-digit phone number',
            roles: 'Select at least one role'
          }
        },
        messages: {
          fillRequired: 'Please fill in user name, phone, team, login type, status, and roles',
          phoneInvalid: 'Phone number format is invalid',
          createSuccess: 'User created successfully',
          updateSuccess: 'User updated successfully'
        },
        table: {
          title: 'Access Snapshot',
          user: 'User',
          role: 'Role',
          team: 'Team',
          loginType: 'Login Type',
          lastLogin: 'Last Login',
          status: 'Status',
          operation: 'Action'
        }
      },
      systemRbac: {
        hero: {
          title: 'Permission Management',
          description: 'Manage roles and fixed menu/action permissions for EnvOps RBAC.'
        },
        actions: {
          createRole: 'Create Role',
          refresh: 'Refresh',
          saveRole: 'Save Role',
          savePermissions: 'Save Permissions'
        },
        roleList: {
          title: 'Roles',
          searchPlaceholder: 'Search role name or key',
          builtIn: 'Built-in',
          enabled: 'Enabled',
          disabled: 'Disabled',
          empty: 'No roles found'
        },
        detail: {
          title: 'Role Details',
          roleKey: 'Role Key',
          roleName: 'Role Name',
          description: 'Description',
          enabled: 'Enabled',
          builtInHint: 'Built-in roles cannot be deleted in this version.'
        },
        permissions: {
          title: 'Permission Tree',
          menu: 'Menu',
          action: 'Action',
          empty: 'No permissions available',
          menuRequired: 'Select the menu permission before assigning its actions.'
        },
        messages: {
          loadFailed: 'Failed to load RBAC data',
          createSuccess: 'Role created',
          updateSuccess: 'Role updated',
          permissionSaveSuccess: 'Role permissions saved',
          missingRoleManagePermission: 'Missing permission: system:role:manage'
        }
      }
    }
  },
  form: {
    required: 'Cannot be empty',
    userName: {
      required: 'Please enter user name',
      invalid: 'User name format is incorrect'
    },
    phone: {
      required: 'Please enter phone number',
      invalid: 'Phone number format is incorrect'
    },
    pwd: {
      required: 'Please enter password',
      invalid: "6-18 characters, including letters, numbers, underscores, and {'@'}"
    },
    confirmPwd: {
      required: 'Please enter password again',
      invalid: 'The two passwords are inconsistent'
    },
    code: {
      required: 'Please enter verification code',
      invalid: 'Verification code format is incorrect'
    },
    email: {
      required: 'Please enter email',
      invalid: 'Email format is incorrect'
    }
  },
  dropdown: {
    closeCurrent: 'Close Current',
    closeOther: 'Close Other',
    closeLeft: 'Close Left',
    closeRight: 'Close Right',
    closeAll: 'Close All',
    pin: 'Pin Tab',
    unpin: 'Unpin Tab'
  },
  icon: {
    themeConfig: 'Theme Configuration',
    themeSchema: 'Theme Schema',
    lang: 'Switch Language',
    fullscreen: 'Fullscreen',
    fullscreenExit: 'Exit Fullscreen',
    reload: 'Reload Page',
    collapse: 'Collapse Menu',
    expand: 'Expand Menu',
    pin: 'Pin',
    unpin: 'Unpin'
  },
  datatable: {
    itemCount: 'Total {total} items',
    fixed: {
      left: 'Left Fixed',
      right: 'Right Fixed',
      unFixed: 'Unfixed'
    }
  }
};

export default local;
