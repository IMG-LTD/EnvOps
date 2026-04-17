/** The global namespace for the app */
declare namespace App {
  /** Theme namespace */
  namespace Theme {
    type ColorPaletteNumber = import('@sa/color').ColorPaletteNumber;

    /** NaiveUI theme overrides that can be specified in preset */
    type NaiveUIThemeOverride = import('naive-ui').GlobalThemeOverrides;

    /** Theme setting */
    interface ThemeSetting {
      /** Theme scheme */
      themeScheme: UnionKey.ThemeScheme;
      /** grayscale mode */
      grayscale: boolean;
      /** colour weakness mode */
      colourWeakness: boolean;
      /** Whether to recommend color */
      recommendColor: boolean;
      /** Theme color */
      themeColor: string;
      /** Theme radius */
      themeRadius: number;
      /** Other color */
      otherColor: OtherColor;
      /** Whether info color is followed by the primary color */
      isInfoFollowPrimary: boolean;
      /** Layout */
      layout: {
        /** Layout mode */
        mode: UnionKey.ThemeLayoutMode;
        /** Scroll mode */
        scrollMode: UnionKey.ThemeScrollMode;
      };
      /** Page */
      page: {
        /** Whether to show the page transition */
        animate: boolean;
        /** Page animate mode */
        animateMode: UnionKey.ThemePageAnimateMode;
      };
      /** Header */
      header: {
        /** Header height */
        height: number;
        /** Header breadcrumb */
        breadcrumb: {
          /** Whether to show the breadcrumb */
          visible: boolean;
          /** Whether to show the breadcrumb icon */
          showIcon: boolean;
        };
        /** Multilingual */
        multilingual: {
          /** Whether to show the multilingual */
          visible: boolean;
        };
        globalSearch: {
          /** Whether to show the GlobalSearch */
          visible: boolean;
        };
      };
      /** Tab */
      tab: {
        /** Whether to show the tab */
        visible: boolean;
        /**
         * Whether to cache the tab
         *
         * If cache, the tabs will get from the local storage when the page is refreshed
         */
        cache: boolean;
        /** Tab height */
        height: number;
        /** Tab mode */
        mode: UnionKey.ThemeTabMode;
        /** Whether to close tab by middle click */
        closeTabByMiddleClick: boolean;
      };
      /** Fixed header and tab */
      fixedHeaderAndTab: boolean;
      /** Sider */
      sider: {
        /** Inverted sider */
        inverted: boolean;
        /** Sider width */
        width: number;
        /** Collapsed sider width */
        collapsedWidth: number;
        /** Sider width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or 'top-hybrid-header-first' */
        mixWidth: number;
        /**
         * Collapsed sider width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or
         * 'top-hybrid-header-first'
         */
        mixCollapsedWidth: number;
        /** Child menu width when the layout is 'vertical-mix', 'top-hybrid-sidebar-first', or 'top-hybrid-header-first' */
        mixChildMenuWidth: number;
        /** Whether to auto select the first submenu */
        autoSelectFirstMenu: boolean;
      };
      /** Footer */
      footer: {
        /** Whether to show the footer */
        visible: boolean;
        /** Whether fixed the footer */
        fixed: boolean;
        /** Footer height */
        height: number;
        /**
         * Whether float the footer to the right when the layout is 'top-hybrid-sidebar-first' or
         * 'top-hybrid-header-first'
         */
        right: boolean;
      };
      /** Watermark */
      watermark: {
        /** Whether to show the watermark */
        visible: boolean;
        /** Watermark text */
        text: string;
        /** Whether to use user name as watermark text */
        enableUserName: boolean;
        /** Whether to use current time as watermark text */
        enableTime: boolean;
        /** Time format for watermark text */
        timeFormat: string;
      };
      /** define some theme settings tokens, will transform to css variables */
      tokens: {
        light: ThemeSettingToken;
        dark?: {
          [K in keyof ThemeSettingToken]?: Partial<ThemeSettingToken[K]>;
        };
      };
    }

    interface OtherColor {
      info: string;
      success: string;
      warning: string;
      error: string;
    }

    interface ThemeColor extends OtherColor {
      primary: string;
    }

    type ThemeColorKey = keyof ThemeColor;

    type ThemePaletteColor = {
      [key in ThemeColorKey | `${ThemeColorKey}-${ColorPaletteNumber}`]: string;
    };

    type BaseToken = Record<string, Record<string, string>>;

    interface ThemeSettingTokenColor {
      /** the progress bar color, if not set, will use the primary color */
      nprogress?: string;
      container: string;
      layout: string;
      inverted: string;
      'base-text': string;
    }

    interface ThemeSettingTokenBoxShadow {
      header: string;
      sider: string;
      tab: string;
    }

    interface ThemeSettingToken {
      colors: ThemeSettingTokenColor;
      boxShadow: ThemeSettingTokenBoxShadow;
    }

    type ThemeTokenColor = ThemePaletteColor & ThemeSettingTokenColor;

    /** Theme token CSS variables */
    type ThemeTokenCSSVars = {
      colors: ThemeTokenColor & { [key: string]: string };
      boxShadow: ThemeSettingTokenBoxShadow & { [key: string]: string };
    };
  }

  /** Global namespace */
  namespace Global {
    type VNode = import('vue').VNode;
    type RouteLocationNormalizedLoaded = import('vue-router').RouteLocationNormalizedLoaded;
    type RouteKey = import('@elegant-router/types').RouteKey;
    type RouteMap = import('@elegant-router/types').RouteMap;
    type RoutePath = import('@elegant-router/types').RoutePath;
    type LastLevelRouteKey = import('@elegant-router/types').LastLevelRouteKey;

    /** The router push options */
    type RouterPushOptions = {
      query?: Record<string, string>;
      params?: Record<string, string>;
      force?: boolean;
    };

    /** The global header props */
    interface HeaderProps {
      /** Whether to show the logo */
      showLogo?: boolean;
      /** Whether to show the menu toggler */
      showMenuToggler?: boolean;
      /** Whether to show the menu */
      showMenu?: boolean;
    }

    /** The global menu */
    type Menu = {
      /**
       * The menu key
       *
       * Equal to the route key
       */
      key: string;
      /** The menu label */
      label: string;
      /** The menu i18n key */
      i18nKey?: I18n.I18nKey | null;
      /** The route key */
      routeKey: RouteKey;
      /** The route path */
      routePath: RoutePath;
      /** The menu icon */
      icon?: () => VNode;
      /** The menu children */
      children?: Menu[];
    };

    type Breadcrumb = Omit<Menu, 'children'> & {
      options?: Breadcrumb[];
    };

    /** Tab route */
    type TabRoute = Pick<RouteLocationNormalizedLoaded, 'name' | 'path' | 'meta'> &
      Partial<Pick<RouteLocationNormalizedLoaded, 'fullPath' | 'query' | 'matched'>>;

    /** The global tab */
    type Tab = {
      /** The tab id */
      id: string;
      /** The tab label */
      label: string;
      /**
       * The new tab label
       *
       * If set, the tab label will be replaced by this value
       */
      newLabel?: string;
      /**
       * The old tab label
       *
       * when reset the tab label, the tab label will be replaced by this value
       */
      oldLabel?: string;
      /** The tab route key */
      routeKey: LastLevelRouteKey;
      /** The tab route path */
      routePath: RouteMap[LastLevelRouteKey];
      /** The tab route full path */
      fullPath: string;
      /** The tab fixed index */
      fixedIndex?: number | null;
      /**
       * Tab icon
       *
       * Iconify icon
       */
      icon?: string;
      /**
       * Tab local icon
       *
       * Local icon
       */
      localIcon?: string;
      /** I18n key */
      i18nKey?: I18n.I18nKey | null;
    };

    /** Form rule */
    type FormRule = import('naive-ui').FormItemRule;

    /** The global dropdown key */
    type DropdownKey = 'closeCurrent' | 'closeOther' | 'closeLeft' | 'closeRight' | 'closeAll' | 'pin' | 'unpin';
  }

  /**
   * I18n namespace
   *
   * Locales type
   */
  namespace I18n {
    type RouteKey = import('@elegant-router/types').RouteKey;

    type LangType = 'en-US' | 'zh-CN';

    type LangOption = {
      label: string;
      key: LangType;
    };

    type I18nRouteKey = Exclude<RouteKey, 'root' | 'not-found'>;

    type FormMsg = {
      required: string;
      invalid: string;
    };

    type Schema = {
      system: {
        title: string;
        updateTitle: string;
        updateContent: string;
        updateConfirm: string;
        updateCancel: string;
      };
      common: {
        action: string;
        add: string;
        addSuccess: string;
        backToHome: string;
        batchDelete: string;
        cancel: string;
        close: string;
        check: string;
        selectAll: string;
        expandColumn: string;
        columnSetting: string;
        config: string;
        confirm: string;
        delete: string;
        deleteSuccess: string;
        confirmDelete: string;
        edit: string;
        warning: string;
        error: string;
        index: string;
        keywordSearch: string;
        logout: string;
        logoutConfirm: string;
        lookForward: string;
        modify: string;
        modifySuccess: string;
        noData: string;
        operate: string;
        pleaseCheckValue: string;
        refresh: string;
        reset: string;
        search: string;
        switch: string;
        tip: string;
        trigger: string;
        update: string;
        updateSuccess: string;
        userCenter: string;
        yesOrNo: {
          yes: string;
          no: string;
        };
      };
      request: {
        logout: string;
        logoutMsg: string;
        logoutWithModal: string;
        logoutWithModalMsg: string;
        tokenExpired: string;
      };
      theme: {
        themeDrawerTitle: string;
        tabs: {
          appearance: string;
          layout: string;
          general: string;
          preset: string;
        };
        appearance: {
          themeSchema: { title: string } & Record<UnionKey.ThemeScheme, string>;
          grayscale: string;
          colourWeakness: string;
          themeColor: {
            title: string;
            followPrimary: string;
          } & Record<Theme.ThemeColorKey, string>;
          recommendColor: string;
          recommendColorDesc: string;
          themeRadius: {
            title: string;
          };
          preset: {
            title: string;
            apply: string;
            applySuccess: string;
            [key: string]:
              | {
                  name: string;
                  desc: string;
                }
              | string;
          };
        };
        layout: {
          layoutMode: { title: string } & Record<UnionKey.ThemeLayoutMode, string> & {
              [K in `${UnionKey.ThemeLayoutMode}_detail`]: string;
            };
          tab: {
            title: string;
            visible: string;
            cache: string;
            cacheTip: string;
            height: string;
            mode: { title: string } & Record<UnionKey.ThemeTabMode, string>;
            closeByMiddleClick: string;
            closeByMiddleClickTip: string;
          };
          header: {
            title: string;
            height: string;
            breadcrumb: {
              visible: string;
              showIcon: string;
            };
          };
          sider: {
            title: string;
            inverted: string;
            width: string;
            collapsedWidth: string;
            mixWidth: string;
            mixCollapsedWidth: string;
            mixChildMenuWidth: string;
            autoSelectFirstMenu: string;
            autoSelectFirstMenuTip: string;
          };
          footer: {
            title: string;
            visible: string;
            fixed: string;
            height: string;
            right: string;
          };
          content: {
            title: string;
            scrollMode: { title: string; tip: string } & Record<UnionKey.ThemeScrollMode, string>;
            page: {
              animate: string;
              mode: { title: string } & Record<UnionKey.ThemePageAnimateMode, string>;
            };
            fixedHeaderAndTab: string;
          };
        };
        general: {
          title: string;
          watermark: {
            title: string;
            visible: string;
            text: string;
            enableUserName: string;
            enableTime: string;
            timeFormat: string;
          };
          multilingual: {
            title: string;
            visible: string;
          };
          globalSearch: {
            title: string;
            visible: string;
          };
        };
        configOperation: {
          copyConfig: string;
          copySuccessMsg: string;
          resetConfig: string;
          resetSuccessMsg: string;
        };
      };
      route: Record<I18nRouteKey, string>;
      page: {
        login: {
          common: {
            loginOrRegister: string;
            userNamePlaceholder: string;
            phonePlaceholder: string;
            codePlaceholder: string;
            passwordPlaceholder: string;
            confirmPasswordPlaceholder: string;
            codeLogin: string;
            confirm: string;
            back: string;
            validateSuccess: string;
            loginSuccess: string;
            welcomeBack: string;
          };
          pwdLogin: {
            title: string;
            rememberMe: string;
            forgetPassword: string;
            register: string;
            otherAccountLogin: string;
            otherLoginMode: string;
            superAdmin: string;
            admin: string;
            user: string;
          };
          codeLogin: {
            title: string;
            getCode: string;
            reGetCode: string;
            sendCodeSuccess: string;
            imageCodePlaceholder: string;
          };
          register: {
            title: string;
            agreement: string;
            protocol: string;
            policy: string;
          };
          resetPwd: {
            title: string;
          };
          bindWeChat: {
            title: string;
          };
        };
        home: {
          branchDesc: string;
          greeting: string;
          weatherDesc: string;
          projectCount: string;
          todo: string;
          message: string;
          downloadCount: string;
          registerCount: string;
          schedule: string;
          study: string;
          work: string;
          rest: string;
          entertainment: string;
          visitCount: string;
          turnover: string;
          dealCount: string;
          projectNews: {
            title: string;
            moreNews: string;
            desc1: string;
            desc2: string;
            desc3: string;
            desc4: string;
            desc5: string;
          };
          creativity: string;
        };
        app: {
          common: {
            save: string;
            appCode: string;
            appName: string;
            appType: string;
            runtimeType: string;
            deployMode: string;
            defaultPort: string;
            healthCheckPath: string;
            description: string;
            status: string;
            updatedAt: string;
          };
          definition: {
            desc: string;
            listTitle: string;
            detailTitle: string;
            formTitleCreate: string;
            formTitleEdit: string;
          };
          version: {
            desc: string;
            listTitle: string;
            currentApp: string;
            selectApp: string;
            formTitleCreate: string;
            formTitleEdit: string;
            versionNo: string;
            packageId: string;
            configTemplateId: string;
            scriptTemplateId: string;
            changelog: string;
          };
          package: {
            desc: string;
            uploadTitle: string;
            listTitle: string;
            packageName: string;
            packageType: string;
            storageType: string;
            file: string;
            selectFile: string;
            uploadAction: string;
            uploadFileRequired: string;
            filePath: string;
            fileSize: string;
            fileHash: string;
          };
          configTemplate: {
            desc: string;
            listTitle: string;
            formTitleCreate: string;
            formTitleEdit: string;
            templateCode: string;
            templateName: string;
            templateContent: string;
            renderEngine: string;
          };
          scriptTemplate: {
            desc: string;
            listTitle: string;
            formTitleCreate: string;
            formTitleEdit: string;
            templateCode: string;
            templateName: string;
            scriptType: string;
            scriptContent: string;
          };
        };
        envops: {
          common: {
            environment: {
              production: string;
              staging: string;
              sandbox: string;
            };
            batch: {
              canary20: string;
              fullRelease: string;
              canary10: string;
            };
            owner: {
              envops: string;
              release: string;
              sre: string;
              qa: string;
            };
            team: {
              envops: string;
              release: string;
              traffic: string;
              qa: string;
              fintech: string;
              platform: string;
              sre: string;
            };
            status: {
              running: string;
              pendingApproval: string;
              rollbackRequired: string;
              success: string;
              warning: string;
              timeout: string;
              online: string;
              offline: string;
              pending: string;
              failed: string;
              managed: string;
              draft: string;
              review: string;
              queued: string;
              cancelled: string;
              rejected: string;
              enabled: string;
              preview: string;
              standby: string;
              active: string;
              disabled: string;
            };
            schedule: {
              every10Min: string;
              everyHour: string;
              every5Min: string;
              every15Min: string;
            };
            taskType: {
              inspection: string;
              deploy: string;
              traffic: string;
              assetSync: string;
            };
            strategy: {
              headerCanary: string;
              blueGreen: string;
              weightedRouting: string;
              emergencyRollback: string;
            };
            role: {
              platformAdmin: string;
              releaseManager: string;
              trafficOwner: string;
              observer: string;
            };
            loginType: {
              passwordOtp: string;
              password: string;
              sso: string;
            };
          };
          home: {
            hero: {
              title: string;
              description: string;
              descriptionWithUser: string;
            };
            tags: {
              dynamicRoutesReady: string;
              vitestHarnessEnabled: string;
            };
            summary: {
              managedApplications: {
                label: string;
                desc: string;
              };
              onlineHosts: {
                label: string;
                desc: string;
              };
              runningTasks: {
                label: string;
                desc: string;
              };
              trafficPolicies: {
                label: string;
                desc: string;
              };
            };
            sections: {
              releasePipelineFocus: string;
              operatorFocus: string;
              inspectionHealth: string;
              platformReadiness: string;
            };
            releaseTable: {
              taskId: string;
              application: string;
              environment: string;
              batch: string;
              owner: string;
              status: string;
            };
            operatorFocusDesc: string;
            focusList: {
              item1: string;
              item2: string;
              item3: string;
              item4: string;
            };
            inspectionTable: {
              task: string;
              target: string;
              schedule: string;
              lastResult: string;
            };
            readiness: {
              frontendShell: {
                label: string;
                value: string;
              };
              routingMode: {
                label: string;
                value: string;
              };
              qualityGate: {
                label: string;
                value: string;
              };
              nextSlice: {
                label: string;
                value: string;
              };
            };
          };
          assetHost: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              cmdbSynchronized: string;
              maintenanceWindow: string;
            };
            summary: {
              managedHosts: {
                label: string;
                desc: string;
              };
              online: {
                label: string;
                desc: string;
              };
              pendingMaintenance: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              host: string;
              ip: string;
              environment: string;
              cluster: string;
              owner: string;
              status: string;
              lastHeartbeat: string;
            };
          };
          assetGroup: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              groups: string;
              hosts: string;
            };
            table: {
              title: string;
              group: string;
              description: string;
              hostCount: string;
            };
          };
          assetTag: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              total: string;
            };
            table: {
              title: string;
              tag: string;
              color: string;
              description: string;
            };
          };
          assetCredential: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              total: string;
              passwords: string;
              keys: string;
              tokens: string;
            };
            types: {
              sshPassword: string;
              sshKey: string;
              apiToken: string;
            };
            form: {
              title: string;
              name: string;
              type: string;
              username: string;
              secret: string;
              description: string;
              placeholders: {
                name: string;
                username: string;
                secret: string;
                description: string;
              };
              actions: {
                create: string;
              };
            };
            table: {
              title: string;
              name: string;
              type: string;
              username: string;
              description: string;
              createdAt: string;
            };
            messages: {
              fillNameAndType: string;
              createSuccess: string;
            };
          };
          monitorDetectTask: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              healthyCount: string;
              timedOutCount: string;
            };
            summary: {
              scheduledTasks: {
                label: string;
                desc: string;
              };
              successRate: {
                label: string;
                desc: string;
              };
              needsAttention: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              task: string;
              target: string;
              schedule: string;
              lastRun: string;
              result: string;
            };
          };
          monitorMetric: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              host: string;
            };
            summary: {
              cpuCores: {
                label: string;
                desc: string;
              };
              memory: {
                label: string;
                desc: string;
              };
              osName: {
                label: string;
                desc: string;
              };
              agentVersion: {
                label: string;
                desc: string;
              };
            };
            detail: {
              title: string;
              item: string;
              value: string;
              hostname: string;
              osName: string;
              kernel: string;
              cpuCores: string;
              memory: string;
              agentVersion: string;
              collectedAt: string;
            };
          };
          appDefinition: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              definitionSyncEnabled: string;
              draftsAwaitingReview: string;
            };
            summary: {
              applications: {
                label: string;
                desc: string;
              };
              runtimeProfiles: {
                label: string;
                desc: string;
              };
              teamsCovered: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              application: string;
              owner: string;
              runtime: string;
              repository: string;
              currentVersion: string;
              status: string;
            };
          };
          deployTask: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              runningCanary: string;
              rollbackRequired: string;
            };
            summary: {
              pendingApproval: {
                label: string;
                desc: string;
              };
              inProgress: {
                label: string;
                desc: string;
              };
              failedIn24h: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              taskId: string;
              application: string;
              environment: string;
              batch: string;
              operator: string;
              status: string;
            };
            actions: {
              detail: string;
              execute: string;
              retry: string;
              rollback: string;
              cancel: string;
            };
            detail: {
              title: string;
              manualRefresh: string;
              taskId: string;
              taskName: string;
              taskType: string;
              originTaskId: string;
              application: string;
              version: string;
              environment: string;
              batch: string;
              operator: string;
              status: string;
              approvalOperator: string;
              approvalComment: string;
              approvalAt: string;
              startedAt: string;
              finishedAt: string;
              createdAt: string;
              updatedAt: string;
            };
            hosts: {
              title: string;
              hostName: string;
              ipAddress: string;
              status: string;
              currentStep: string;
              startedAt: string;
              finishedAt: string;
              errorMsg: string;
            };
            logs: {
              title: string;
              createdAt: string;
              taskHostId: string;
              logLevel: string;
              content: string;
            };
          };
          taskCenter: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              queueBalanced: string;
              jobsNearingSla: string;
            };
            summary: {
              queued: {
                label: string;
                desc: string;
              };
              running: {
                label: string;
                desc: string;
              };
              slaBreachRisk: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              taskId: string;
              type: string;
              source: string;
              executor: string;
              priority: string;
              status: string;
            };
          };
          trafficController: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              policiesLive: string;
              previewPolicy: string;
            };
            summary: {
              policiesEnabled: {
                label: string;
                desc: string;
              };
              canaryReleases: {
                label: string;
                desc: string;
              };
              rollbackReady: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              application: string;
              strategy: string;
              scope: string;
              trafficRatio: string;
              owner: string;
              status: string;
            };
          };
          systemUser: {
            hero: {
              title: string;
              description: string;
            };
            tags: {
              rbacEnabled: string;
              userUnderReview: string;
            };
            summary: {
              users: {
                label: string;
                desc: string;
              };
              admins: {
                label: string;
                desc: string;
              };
              activeToday: {
                label: string;
                desc: string;
              };
            };
            table: {
              title: string;
              user: string;
              role: string;
              team: string;
              loginType: string;
              lastLogin: string;
              status: string;
            };
          };
        };
      };
      form: {
        required: string;
        userName: FormMsg;
        phone: FormMsg;
        pwd: FormMsg;
        confirmPwd: FormMsg;
        code: FormMsg;
        email: FormMsg;
      };
      dropdown: Record<Global.DropdownKey, string>;
      icon: {
        themeConfig: string;
        themeSchema: string;
        lang: string;
        fullscreen: string;
        fullscreenExit: string;
        reload: string;
        collapse: string;
        expand: string;
        pin: string;
        unpin: string;
      };
      datatable: {
        itemCount: string;
        fixed: {
          left: string;
          right: string;
          unFixed: string;
        };
      };
    };

    type GetI18nKey<T extends Record<string, unknown>, K extends keyof T = keyof T> = K extends string
      ? T[K] extends Record<string, unknown>
        ? `${K}.${GetI18nKey<T[K]>}`
        : K
      : never;

    type I18nKey = GetI18nKey<Schema>;

    type TranslateOptions<Locales extends string> = import('vue-i18n').TranslateOptions<Locales>;

    interface $T {
      (key: I18nKey): string;
      (key: I18nKey, plural: number, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, defaultMsg: string, options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], options?: TranslateOptions<I18nKey>): string;
      (key: I18nKey, list: unknown[], plural: number): string;
      (key: I18nKey, list: unknown[], defaultMsg: string): string;
      (key: I18nKey, named: Record<string, unknown>, options?: TranslateOptions<LangType>): string;
      (key: I18nKey, named: Record<string, unknown>, plural: number): string;
      (key: I18nKey, named: Record<string, unknown>, defaultMsg: string): string;
    }
  }

  /** Service namespace */
  namespace Service {
    /** Other baseURL key */
    type OtherBaseURLKey = 'demo';

    interface ServiceConfigItem {
      /** The backend service base url */
      baseURL: string;
      /** The proxy pattern of the backend service base url */
      proxyPattern: string;
    }

    interface OtherServiceConfigItem extends ServiceConfigItem {
      key: OtherBaseURLKey;
    }

    /** The backend service config */
    interface ServiceConfig extends ServiceConfigItem {
      /** Other backend service config */
      other: OtherServiceConfigItem[];
    }

    interface SimpleServiceConfig extends Pick<ServiceConfigItem, 'baseURL'> {
      other: Record<OtherBaseURLKey, string>;
    }

    /** The backend service response data */
    type Response<T = unknown> = {
      /** The backend service response code */
      code: string;
      /** The backend service response message */
      msg: string;
      /** The backend service response data */
      data: T;
    };

    /** The demo backend service response data */
    type DemoResponse<T = unknown> = {
      /** The backend service response code */
      status: string;
      /** The backend service response message */
      message: string;
      /** The backend service response data */
      result: T;
    };
  }
}
