# 环境管理平台（EnvOps/CMDB Lite）完整方案设计文档

## 文档说明

本文档面向“环境管理平台 / 轻量级 CMDB + 批量安装发布平台”的一期建设，范围覆盖项目完整目录结构、后端初始化代码骨架、数据库表设计 SQL、接口文档、前端菜单与路由设计，以及从资深架构与资深研发专家视角给出的最终落地建议。方案基于 Spring Boot、Maven、MyBatis 与 Soybean Admin，采用模块化单体架构，优先满足快速交付、长期演进与团队可维护性。[cite:1][cite:3][cite:14]

在技术选型层面，Soybean Admin 是一个面向中后台场景的开源管理端模板，具备较成熟的后台布局、权限路由与前端工程能力，适合作为本项目管理控制台的前端基座。[cite:14] 对于后端架构，分层设计与面向领域的模块划分比一开始拆成微服务更适合 0 到 1 阶段的平台型系统，可以降低复杂度并保留未来拆分空间。[cite:1][cite:3][cite:5]

## 建设目标

一期目标是构建一个可落地、可上线试运行的环境管理平台，聚焦两条主线：第一条是“机器资源管理与检测”，第二条是“应用批量安装与任务执行”。这类平台本质上已经同时包含了轻量 CMDB、作业执行平台与初级发布平台的核心能力，因此设计重点不应放在“字段堆砌”，而应聚焦“资产—检测—应用—安装任务—结果反馈”闭环。[cite:5][cite:15]

平台建设建议采用“先主机纳管，再检测采集，再模板化安装”的路线。以 SSH Agentless 为一期执行模型可以降低落地门槛；后续如果要增强持续上报、实时日志回传与跨系统检测，再增加 Agent 模式即可。[cite:7][cite:15]

## 总体架构

### 架构原则

- 一期采用模块化单体架构，不直接拆微服务。
- 以业务域进行模块拆分，而不是传统的全局 controller/service/mapper 平铺。
- 平台执行能力与业务能力分离，便于后续接入 Agent、Ansible、Salt 或自研执行器。
- 所有长耗时操作统一走异步任务模型，不在 HTTP 请求链路中执行完整安装过程。
- 所有敏感操作都要有审计日志、任务状态与失败补偿机制。

### 分层模型

建议采用“经典四层增强版”实现：

- `interfaces`：控制器层，处理 API 输入输出。
- `application`：应用服务层，编排业务流程与事务。
- `domain`：领域层，定义实体、聚合、仓储接口、核心规则。
- `infrastructure`：基础设施层，包含 MyBatis Mapper、远程执行器、缓存、消息、文件存储等实现。[cite:3][cite:5]

如果团队更偏传统 Java 企业开发风格，也可以在代码组织中体现为 `controller / service / domain / mapper`，但建议在服务层之上保留应用服务概念，避免将复杂业务规则全部写死在 serviceImpl 中。[cite:1][cite:5]

### 核心业务链路

1. 主机录入或导入。
2. 凭据绑定、分组、标签归类。
3. 发起主机探测，回填硬件与软件事实数据。
4. 建立巡检任务，形成指标快照。
5. 创建应用定义、版本与模板。
6. 选择主机，生成安装任务。
7. 分发文件、执行脚本、健康检查、记录日志。
8. 输出安装结果，支持失败重试与回滚。

## 技术选型

| 维度 | 选型 | 说明 |
|---|---|---|
| 前端 | Soybean Admin + Vue 3 + TypeScript + Pinia + Vue Router + Naive UI | 适合中后台控制台，具备成熟权限路由与布局基础。[cite:14] |
| 后端 | Spring Boot 3.x | 适合企业级平台系统快速搭建与演进。[cite:1][cite:5] |
| 构建 | Maven 多模块 | 便于依赖统一、模块拆分与企业规范治理。 |
| ORM | MyBatis | 保留 SQL 可控性，适合 CMDB/任务类复杂查询。 |
| 数据库 | MySQL 8.x | 一期优先成熟稳定。 |
| 缓存 | Redis | 用于会话、验证码、任务进度缓存、状态快照。 |
| 认证鉴权 | Spring Security + JWT | 前后端分离常见方案。 |
| 任务调度 | Quartz / XXL-JOB | 满足巡检与安装编排；复杂 DAG 场景再考虑工作流平台。[cite:7][cite:15] |
| 远程执行 | SSH Agentless（一期） | 低成本快速落地。 |
| 文件存储 | MinIO / 本地制品目录 | 用于安装包、脚本与模板存储。 |
| 文档 | OpenAPI/Swagger | 用于接口联调与文档生成。 |

## 项目目录结构

### Maven 多模块结构

```text
envops-parent
├── pom.xml
├── README.md
├── sql
│   └── envops-init.sql
├── docs
│   └── architecture.md
├── envops-common
│   ├── pom.xml
│   └── src/main/java/com/company/envops/common
│       ├── constants
│       ├── enums
│       ├── exception
│       ├── model
│       ├── response
│       ├── util
│       └── validation
├── envops-framework
│   ├── pom.xml
│   └── src/main/java/com/company/envops/framework
│       ├── config
│       ├── security
│       │   ├── filter
│       │   ├── handler
│       │   ├── model
│       │   └── service
│       ├── mybatis
│       ├── redis
│       ├── web
│       ├── logging
│       └── aspect
├── envops-system
│   ├── pom.xml
│   └── src/main/java/com/company/envops/modules/system
│       ├── controller
│       ├── application
│       ├── service
│       ├── domain
│       │   ├── entity
│       │   ├── dto
│       │   ├── vo
│       │   └── repository
│       ├── infrastructure
│       │   ├── mapper
│       │   └── repository
│       └── convert
├── envops-asset
│   ├── pom.xml
│   └── src/main/java/com/company/envops/modules/asset
│       ├── controller
│       ├── application
│       ├── service
│       ├── domain
│       ├── infrastructure
│       └── convert
├── envops-monitor
│   ├── pom.xml
│   └── src/main/java/com/company/envops/modules/monitor
│       ├── controller
│       ├── application
│       ├── service
│       ├── domain
│       ├── infrastructure
│       └── detector
├── envops-app
│   ├── pom.xml
│   └── src/main/java/com/company/envops/modules/app
│       ├── controller
│       ├── application
│       ├── service
│       ├── domain
│       ├── infrastructure
│       └── installer
├── envops-deploy
│   ├── pom.xml
│   └── src/main/java/com/company/envops/modules/deploy
│       ├── controller
│       ├── application
│       ├── service
│       ├── domain
│       ├── infrastructure
│       ├── executor
│       └── scheduler
└── envops-admin
    ├── pom.xml
    └── src/main
        ├── java/com/company/envops/EnvOpsApplication.java
        └── resources
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            ├── mapper
            └── logback-spring.xml
```

### 模块职责划分

| 模块 | 职责 |
|---|---|
| `envops-common` | 公共响应、异常、枚举、工具类、基础模型。 |
| `envops-framework` | Spring Security、JWT、MyBatis 配置、Web 全局配置、日志与切面。 |
| `envops-system` | 用户、角色、菜单、权限、审计日志、字典等系统基础能力。 |
| `envops-asset` | 主机、主机组、标签、凭据、静态资产事实。 |
| `envops-monitor` | 主机探测、巡检、指标采集、异常检测。 |
| `envops-app` | 应用定义、版本、安装包、配置模板、脚本模板。 |
| `envops-deploy` | 安装任务、步骤执行、日志、回滚、批量分发。 |
| `envops-admin` | 启动模块与统一资源装配。 |

## Maven 初始化代码骨架

### 根工程 `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>envops-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>envops-common</module>
        <module>envops-framework</module>
        <module>envops-system</module>
        <module>envops-asset</module>
        <module>envops-monitor</module>
        <module>envops-app</module>
        <module>envops-deploy</module>
        <module>envops-admin</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring.boot.version>3.3.6</spring.boot.version>
        <mybatis.spring.boot.version>3.0.3</mybatis.spring.boot.version>
        <mysql.version>8.4.0</mysql.version>
        <hutool.version>5.8.34</hutool.version>
        <jjwt.version>0.12.6</jjwt.version>
        <lombok.version>1.18.34</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <encoding>UTF-8</encoding>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 启动模块 `envops-admin/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.company</groupId>
        <artifactId>envops-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>envops-admin</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-framework</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-system</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-asset</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-monitor</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-app</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>envops-deploy</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.spring.boot.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 启动类

```java
package com.company.envops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.company.envops.**.infrastructure.mapper")
public class EnvOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnvOpsApplication.class, args);
    }
}
```

## 后端基础代码骨架

### 统一返回结构

```java
package com.company.envops.common.response;

import lombok.Data;

@Data
public class R<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }
}
```

### 分页结果

```java
package com.company.envops.common.model;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;
}
```

### 业务异常

```java
package com.company.envops.common.exception;

public class BizException extends RuntimeException {
    private final Integer code;

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
```

### 全局异常处理

```java
package com.company.envops.framework.web;

import com.company.envops.common.exception.BizException;
import com.company.envops.common.response.R;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() == null
                ? "参数校验失败"
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleEx(Exception e) {
        return R.fail(500, e.getMessage());
    }
}
```

### 基础实体类

```java
package com.company.envops.common.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BaseEntity {
    private Long id;
    private Integer deleted;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### JWT 工具类

```java
package com.company.envops.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenUtil {
    private static final String SECRET = "EnvOpsJwtSecretKeyEnvOpsJwtSecretKey123456";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String createToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 86400000))
                .signWith(KEY)
                .compact();
    }

    public static String getUsername(String token) {
        Claims claims = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }
}
```

### 安全配置

```java
package com.company.envops.framework.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/doc.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

### 登录接口骨架

```java
package com.company.envops.modules.system.controller;

import com.company.envops.common.response.R;
import com.company.envops.framework.security.JwtTokenUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginCmd cmd) {
        String token = JwtTokenUtil.createToken(cmd.getUsername());
        return R.ok(Map.of("token", token));
    }

    @Data
    public static class LoginCmd {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
```

### 主机实体骨架

```java
package com.company.envops.modules.asset.domain.entity;

import com.company.envops.common.model.BaseEntity;
import lombok.Data;

@Data
public class Host extends BaseEntity {
    private String hostName;
    private String ip;
    private String osType;
    private Integer sshPort;
    private String sshUser;
    private String env;
    private String regionCode;
    private String status;
    private Integer cpuCores;
    private Long memoryMb;
    private Long diskGb;
}
```

### 主机 Mapper 骨架

```java
package com.company.envops.modules.asset.infrastructure.mapper;

import com.company.envops.modules.asset.domain.entity.Host;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HostMapper {
    int insert(Host host);
    int updateById(Host host);
    Host selectById(@Param("id") Long id);
    List<Host> selectPage(@Param("hostName") String hostName,
                          @Param("ip") String ip,
                          @Param("status") String status);
}
```

### 主机应用服务骨架

```java
package com.company.envops.modules.asset.application;

import com.company.envops.modules.asset.domain.entity.Host;
import com.company.envops.modules.asset.infrastructure.mapper.HostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HostApplicationService {

    private final HostMapper hostMapper;

    public Long createHost(Host host) {
        hostMapper.insert(host);
        return host.getId();
    }

    public List<Host> listHosts(String hostName, String ip, String status) {
        return hostMapper.selectPage(hostName, ip, status);
    }
}
```

### 主机控制器骨架

```java
package com.company.envops.modules.asset.controller;

import com.company.envops.common.response.R;
import com.company.envops.modules.asset.application.HostApplicationService;
import com.company.envops.modules.asset.domain.entity.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hosts")
@RequiredArgsConstructor
public class HostController {

    private final HostApplicationService hostApplicationService;

    @PostMapping
    public R<Long> create(@RequestBody Host host) {
        return R.ok(hostApplicationService.createHost(host));
    }

    @GetMapping
    public R<List<Host>> list(@RequestParam(required = false) String hostName,
                              @RequestParam(required = false) String ip,
                              @RequestParam(required = false) String status) {
        return R.ok(hostApplicationService.listHosts(hostName, ip, status));
    }
}
```

### 远程执行器抽象

```java
package com.company.envops.modules.deploy.executor;

public interface RemoteExecutor {
    ExecResult exec(ExecRequest request);
    UploadResult upload(FileUploadRequest request);
    DetectResult detect(DetectRequest request);
}
```

### SSH 执行器实现骨架

```java
package com.company.envops.modules.deploy.executor;

import org.springframework.stereotype.Component;

@Component
public class SshRemoteExecutor implements RemoteExecutor {
    @Override
    public ExecResult exec(ExecRequest request) {
        return new ExecResult();
    }

    @Override
    public UploadResult upload(FileUploadRequest request) {
        return new UploadResult();
    }

    @Override
    public DetectResult detect(DetectRequest request) {
        return new DetectResult();
    }
}
```

### 检测器抽象

```java
package com.company.envops.modules.monitor.detector;

import com.company.envops.modules.asset.domain.entity.Host;
import java.util.Map;

public interface Detector {
    String type();
    DetectOutput detect(Host host, Map<String, Object> params);
}
```

### 安装器抽象

```java
package com.company.envops.modules.app.installer;

import com.company.envops.modules.asset.domain.entity.Host;
import java.util.Map;

public interface Installer {
    String appType();
    InstallPlan buildPlan(Long appId, Long versionId, Host host, Map<String, Object> params);
}
```

## 配置文件建议

### `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: envops-admin
  profiles:
    active: dev
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/envops?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  data:
    redis:
      host: 127.0.0.1
      port: 6379

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.company.envops
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.company.envops: info
```

## 数据库表设计 SQL

以下 SQL 为一期推荐基线，包含系统、资产、检测、应用与部署五大域。对于任务平台，状态字段、审计字段与逻辑删除字段必须统一，这样才能支持后续查询治理、审计追溯与软删除恢复。[cite:5][cite:15]

```sql
CREATE DATABASE IF NOT EXISTS envops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE envops;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(64),
  mobile VARCHAR(32),
  email VARCHAR(128),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id)
);

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  menu_name VARCHAR(64) NOT NULL,
  menu_type VARCHAR(16) NOT NULL COMMENT 'DIR/MENU/BUTTON',
  route_name VARCHAR(64),
  route_path VARCHAR(128),
  component VARCHAR(255),
  perm_code VARCHAR(128),
  icon VARCHAR(64),
  sort_no INT DEFAULT 0,
  visible TINYINT DEFAULT 1,
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_menu (role_id, menu_id)
);

CREATE TABLE sys_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64),
  module_code VARCHAR(64),
  biz_type VARCHAR(64),
  biz_id VARCHAR(64),
  operator_id BIGINT,
  operator_name VARCHAR(64),
  request_uri VARCHAR(255),
  request_method VARCHAR(16),
  request_body TEXT,
  response_body TEXT,
  op_result VARCHAR(16),
  error_msg TEXT,
  op_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE asset_host (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_name VARCHAR(128) NOT NULL,
  ip VARCHAR(64) NOT NULL,
  os_type VARCHAR(32),
  os_version VARCHAR(64),
  arch VARCHAR(32),
  ssh_port INT DEFAULT 22,
  ssh_user VARCHAR(64),
  env_code VARCHAR(32),
  region_code VARCHAR(64),
  host_status VARCHAR(32) DEFAULT 'UNKNOWN',
  online_status VARCHAR(32) DEFAULT 'UNKNOWN',
  cpu_cores INT DEFAULT 0,
  memory_mb BIGINT DEFAULT 0,
  disk_gb BIGINT DEFAULT 0,
  last_detect_time DATETIME,
  remark VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_host_ip (ip)
);

CREATE TABLE asset_host_group (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_code VARCHAR(64) NOT NULL UNIQUE,
  group_name VARCHAR(128) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  remark VARCHAR(500),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE asset_host_group_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_host_group (host_id, group_id)
);

CREATE TABLE asset_host_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_code VARCHAR(64) NOT NULL UNIQUE,
  tag_name VARCHAR(64) NOT NULL,
  tag_color VARCHAR(32),
  remark VARCHAR(255),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE asset_host_tag_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_host_tag (host_id, tag_id)
);

CREATE TABLE asset_host_credential (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  credential_name VARCHAR(128) NOT NULL,
  auth_type VARCHAR(32) NOT NULL COMMENT 'PASSWORD/KEY',
  username VARCHAR(64) NOT NULL,
  password_cipher VARCHAR(1024),
  private_key_cipher TEXT,
  passphrase_cipher VARCHAR(1024),
  port INT DEFAULT 22,
  remark VARCHAR(255),
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE asset_host_fact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_id BIGINT NOT NULL,
  hostname_fact VARCHAR(128),
  kernel_version VARCHAR(128),
  cpu_model VARCHAR(255),
  cpu_cores INT,
  memory_mb BIGINT,
  disk_gb BIGINT,
  jdk_version VARCHAR(64),
  docker_version VARCHAR(64),
  nginx_version VARCHAR(64),
  redis_version VARCHAR(64),
  mysql_version VARCHAR(64),
  collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_host_fact (host_id)
);

CREATE TABLE monitor_metric_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  host_id BIGINT NOT NULL,
  cpu_usage DECIMAL(10,2),
  memory_usage DECIMAL(10,2),
  disk_usage DECIMAL(10,2),
  load_avg VARCHAR(64),
  process_count INT,
  collect_status VARCHAR(32) DEFAULT 'SUCCESS',
  error_msg VARCHAR(500),
  collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_host_collect_time (host_id, collected_at)
);

CREATE TABLE monitor_detect_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL UNIQUE,
  task_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(32) NOT NULL COMMENT 'MANUAL/SCHEDULED',
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  target_count INT DEFAULT 0,
  success_count INT DEFAULT 0,
  fail_count INT DEFAULT 0,
  started_at DATETIME,
  finished_at DATETIME,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE monitor_detect_task_host (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  host_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  detect_output TEXT,
  error_msg TEXT,
  started_at DATETIME,
  finished_at DATETIME,
  UNIQUE KEY uk_detect_host (task_id, host_id)
);

CREATE TABLE app_definition (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  app_code VARCHAR(64) NOT NULL UNIQUE,
  app_name VARCHAR(128) NOT NULL,
  app_type VARCHAR(32) NOT NULL COMMENT 'JAVA/NGINX/SCRIPT/DOCKER',
  runtime_type VARCHAR(32),
  deploy_mode VARCHAR(32) COMMENT 'SYSTEMD/PROCESS/DOCKER',
  default_port INT,
  health_check_path VARCHAR(255),
  description VARCHAR(500),
  status TINYINT DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE app_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  app_id BIGINT NOT NULL,
  version_no VARCHAR(64) NOT NULL,
  package_id BIGINT,
  config_template_id BIGINT,
  script_template_id BIGINT,
  changelog VARCHAR(1000),
  status TINYINT DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_app_version (app_id, version_no)
);

CREATE TABLE app_package (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_name VARCHAR(255) NOT NULL,
  package_type VARCHAR(32) NOT NULL COMMENT 'JAR/TAR/RPM/SH',
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT,
  file_hash VARCHAR(128),
  storage_type VARCHAR(32) DEFAULT 'LOCAL',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE app_config_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL UNIQUE,
  template_name VARCHAR(128) NOT NULL,
  template_content TEXT NOT NULL,
  render_engine VARCHAR(32) DEFAULT 'PLAINTEXT',
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE app_script_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL UNIQUE,
  template_name VARCHAR(128) NOT NULL,
  script_type VARCHAR(32) NOT NULL COMMENT 'BASH/PYTHON',
  script_content MEDIUMTEXT NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE deploy_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL UNIQUE,
  task_name VARCHAR(128) NOT NULL,
  task_type VARCHAR(32) NOT NULL COMMENT 'INSTALL/UPGRADE/ROLLBACK',
  app_id BIGINT NOT NULL,
  version_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  batch_strategy VARCHAR(32) DEFAULT 'ALL',
  batch_size INT DEFAULT 0,
  target_count INT DEFAULT 0,
  success_count INT DEFAULT 0,
  fail_count INT DEFAULT 0,
  operator_id BIGINT,
  operator_name VARCHAR(64),
  started_at DATETIME,
  finished_at DATETIME,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  updated_by VARCHAR(64),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE deploy_task_host (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  host_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  current_step VARCHAR(64),
  started_at DATETIME,
  finished_at DATETIME,
  error_msg TEXT,
  UNIQUE KEY uk_task_host (task_id, host_id)
);

CREATE TABLE deploy_task_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_host_id BIGINT NOT NULL,
  step_no INT NOT NULL,
  step_code VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  step_status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  command_text TEXT,
  started_at DATETIME,
  finished_at DATETIME,
  error_msg TEXT,
  UNIQUE KEY uk_task_host_step (task_host_id, step_no)
);

CREATE TABLE deploy_task_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  task_host_id BIGINT,
  step_id BIGINT,
  log_level VARCHAR(16) DEFAULT 'INFO',
  log_content MEDIUMTEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_task_time (task_id, created_at)
);

CREATE TABLE deploy_task_param (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  param_key VARCHAR(128) NOT NULL,
  param_value TEXT,
  secret_flag TINYINT DEFAULT 0,
  UNIQUE KEY uk_task_param (task_id, param_key)
);
```

## API 接口文档

接口风格建议统一为 RESTful + JSON，响应结构统一为 `R<T>`。针对任务类接口，创建动作与执行动作建议分离，即“先创建任务，再执行任务”，这样更有利于审批、预检和重试。[cite:5][cite:15]

### 认证接口

#### 1. 登录

- `POST /api/auth/login`

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "jwt-token"
  },
  "timestamp": 1710000000000
}
```

#### 2. 获取当前用户信息

- `GET /api/auth/me`

返回用户基本信息、角色与前端菜单权限。

### 系统管理接口

#### 用户管理

- `GET /api/system/users`
- `POST /api/system/users`
- `PUT /api/system/users/{id}`
- `DELETE /api/system/users/{id}`
- `POST /api/system/users/{id}/reset-password`

#### 角色管理

- `GET /api/system/roles`
- `POST /api/system/roles`
- `PUT /api/system/roles/{id}`
- `DELETE /api/system/roles/{id}`
- `POST /api/system/roles/{id}/menus`

#### 菜单管理

- `GET /api/system/menus/tree`
- `POST /api/system/menus`
- `PUT /api/system/menus/{id}`
- `DELETE /api/system/menus/{id}`

#### 审计日志

- `GET /api/system/audit-logs`

### 资产中心接口

#### 主机管理

- `GET /api/hosts`
- `GET /api/hosts/{id}`
- `POST /api/hosts`
- `PUT /api/hosts/{id}`
- `DELETE /api/hosts/{id}`
- `POST /api/hosts/import`
- `POST /api/hosts/export`

查询参数示例：

- `hostName`
- `ip`
- `envCode`
- `regionCode`
- `onlineStatus`
- `pageNum`
- `pageSize`

#### 主机分组

- `GET /api/host-groups/tree`
- `POST /api/host-groups`
- `PUT /api/host-groups/{id}`
- `DELETE /api/host-groups/{id}`
- `POST /api/host-groups/{id}/hosts`

#### 主机标签

- `GET /api/host-tags`
- `POST /api/host-tags`
- `PUT /api/host-tags/{id}`
- `DELETE /api/host-tags/{id}`
- `POST /api/hosts/{id}/tags`

#### 凭据管理

- `GET /api/credentials`
- `POST /api/credentials`
- `PUT /api/credentials/{id}`
- `DELETE /api/credentials/{id}`
- `POST /api/hosts/{id}/credential/bind`

### 检测中心接口

#### 即时检测

- `POST /api/detect/tasks`
- `GET /api/detect/tasks`
- `GET /api/detect/tasks/{id}`
- `POST /api/detect/tasks/{id}/execute`
- `GET /api/detect/tasks/{id}/hosts`

#### 主机指标

- `GET /api/hosts/{id}/metrics/latest`
- `GET /api/hosts/{id}/metrics/history`
- `GET /api/hosts/{id}/facts`

#### 巡检计划

- `GET /api/inspect/plans`
- `POST /api/inspect/plans`
- `PUT /api/inspect/plans/{id}`
- `DELETE /api/inspect/plans/{id}`

### 应用中心接口

#### 应用定义

- `GET /api/apps`
- `GET /api/apps/{id}`
- `POST /api/apps`
- `PUT /api/apps/{id}`
- `DELETE /api/apps/{id}`

#### 应用版本

- `GET /api/apps/{id}/versions`
- `POST /api/apps/{id}/versions`
- `PUT /api/app-versions/{id}`
- `DELETE /api/app-versions/{id}`

#### 安装包管理

- `GET /api/packages`
- `POST /api/packages/upload`
- `DELETE /api/packages/{id}`

#### 模板管理

- `GET /api/config-templates`
- `POST /api/config-templates`
- `PUT /api/config-templates/{id}`
- `DELETE /api/config-templates/{id}`
- `GET /api/script-templates`
- `POST /api/script-templates`
- `PUT /api/script-templates/{id}`
- `DELETE /api/script-templates/{id}`

### 安装发布接口

#### 安装任务

- `GET /api/deploy/tasks`
- `GET /api/deploy/tasks/{id}`
- `POST /api/deploy/tasks`
- `POST /api/deploy/tasks/{id}/execute`
- `POST /api/deploy/tasks/{id}/retry`
- `POST /api/deploy/tasks/{id}/rollback`
- `POST /api/deploy/tasks/{id}/cancel`

创建任务请求示例：

```json
{
  "taskName": "install-order-service-prod",
  "taskType": "INSTALL",
  "appId": 1001,
  "versionId": 2002,
  "hostIds": [1, 2, 3],
  "batchStrategy": "ROLLING",
  "batchSize": 1,
  "params": {
    "deployDir": "/data/apps/order-service",
    "port": 8080,
    "profile": "prod"
  }
}
```

#### 任务执行详情

- `GET /api/deploy/tasks/{id}/hosts`
- `GET /api/deploy/task-hosts/{id}/steps`
- `GET /api/deploy/tasks/{id}/logs`

### 响应码建议

| 响应码 | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 409 | 状态冲突，例如任务已执行 |
| 500 | 服务异常 |

## 权限模型设计

权限建议采用“用户—角色—菜单/按钮权限”三层模型，并把资源访问与动作权限分开。Soybean Admin 适合做动态菜单与按钮级权限控制，因此后端需要给前端返回菜单树、路由标识和按钮权限码。[cite:14]

### 权限码规范

建议命名方式：`模块:资源:动作`

示例：

- `asset:host:view`
- `asset:host:create`
- `asset:host:update`
- `asset:host:delete`
- `monitor:detect:execute`
- `app:definition:create`
- `deploy:task:execute`
- `deploy:task:rollback`
- `system:user:reset-password`

### 角色建议

- 超级管理员：全量权限。
- 运维管理员：资产、检测、应用、安装发布全权限。
- 发布工程师：应用中心与安装发布权限。
- 只读审计员：只读查看资产、任务与日志。
- 开发负责人：只读应用与指定环境发布权限。

## 前端菜单与路由设计

Soybean Admin 的优势在于后台系统结构化路由能力较强，因此建议平台前端采用“一级模块 + 二级功能页”的典型中后台信息架构。动态路由与权限菜单结合，可以让不同角色看到不同能力入口。[cite:14]

### 菜单结构

```text
工作台
资产中心
├── 主机管理
├── 主机分组
├── 主机标签
└── 凭据管理
检测中心
├── 即时检测
├── 巡检计划
├── 检测历史
└── 指标快照
应用中心
├── 应用定义
├── 版本管理
├── 安装包管理
├── 配置模板
└── 脚本模板
安装发布
├── 发布任务
├── 创建任务
├── 执行日志
└── 回滚记录
任务中心
├── 全部任务
├── 运行中任务
├── 失败任务
└── 任务详情
系统管理
├── 用户管理
├── 角色管理
├── 菜单管理
└── 审计日志
```

### 前端路由建议

```ts
export const routes = [
  {
    name: 'dashboard',
    path: '/dashboard',
    component: 'layout.base',
    meta: { title: '工作台', icon: 'mdi:monitor-dashboard' },
    children: [
      {
        name: 'dashboard_home',
        path: '/dashboard/home',
        component: '/dashboard/home/index',
        meta: { title: '首页', requiresAuth: true }
      }
    ]
  },
  {
    name: 'asset',
    path: '/asset',
    component: 'layout.base',
    meta: { title: '资产中心', icon: 'mdi:server-network' },
    children: [
      {
        name: 'asset_host',
        path: '/asset/host',
        component: '/asset/host/index',
        meta: { title: '主机管理', requiresAuth: true, permissions: ['asset:host:view'] }
      },
      {
        name: 'asset_group',
        path: '/asset/group',
        component: '/asset/group/index',
        meta: { title: '主机分组', requiresAuth: true, permissions: ['asset:group:view'] }
      },
      {
        name: 'asset_tag',
        path: '/asset/tag',
        component: '/asset/tag/index',
        meta: { title: '主机标签', requiresAuth: true, permissions: ['asset:tag:view'] }
      },
      {
        name: 'asset_credential',
        path: '/asset/credential',
        component: '/asset/credential/index',
        meta: { title: '凭据管理', requiresAuth: true, permissions: ['asset:credential:view'] }
      }
    ]
  },
  {
    name: 'monitor',
    path: '/monitor',
    component: 'layout.base',
    meta: { title: '检测中心', icon: 'mdi:chart-line' },
    children: [
      {
        name: 'monitor_detect_task',
        path: '/monitor/detect-task',
        component: '/monitor/detect-task/index',
        meta: { title: '即时检测', requiresAuth: true, permissions: ['monitor:detect:view'] }
      },
      {
        name: 'monitor_inspect_plan',
        path: '/monitor/inspect-plan',
        component: '/monitor/inspect-plan/index',
        meta: { title: '巡检计划', requiresAuth: true, permissions: ['monitor:inspect:view'] }
      },
      {
        name: 'monitor_history',
        path: '/monitor/history',
        component: '/monitor/history/index',
        meta: { title: '检测历史', requiresAuth: true, permissions: ['monitor:history:view'] }
      },
      {
        name: 'monitor_metric',
        path: '/monitor/metric',
        component: '/monitor/metric/index',
        meta: { title: '指标快照', requiresAuth: true, permissions: ['monitor:metric:view'] }
      }
    ]
  },
  {
    name: 'app',
    path: '/app',
    component: 'layout.base',
    meta: { title: '应用中心', icon: 'mdi:application-cog' },
    children: [
      {
        name: 'app_definition',
        path: '/app/definition',
        component: '/app/definition/index',
        meta: { title: '应用定义', requiresAuth: true, permissions: ['app:definition:view'] }
      },
      {
        name: 'app_version',
        path: '/app/version',
        component: '/app/version/index',
        meta: { title: '版本管理', requiresAuth: true, permissions: ['app:version:view'] }
      },
      {
        name: 'app_package',
        path: '/app/package',
        component: '/app/package/index',
        meta: { title: '安装包管理', requiresAuth: true, permissions: ['app:package:view'] }
      },
      {
        name: 'app_config_template',
        path: '/app/config-template',
        component: '/app/config-template/index',
        meta: { title: '配置模板', requiresAuth: true, permissions: ['app:config-template:view'] }
      },
      {
        name: 'app_script_template',
        path: '/app/script-template',
        component: '/app/script-template/index',
        meta: { title: '脚本模板', requiresAuth: true, permissions: ['app:script-template:view'] }
      }
    ]
  },
  {
    name: 'deploy',
    path: '/deploy',
    component: 'layout.base',
    meta: { title: '安装发布', icon: 'mdi:rocket-launch' },
    children: [
      {
        name: 'deploy_task',
        path: '/deploy/task',
        component: '/deploy/task/index',
        meta: { title: '发布任务', requiresAuth: true, permissions: ['deploy:task:view'] }
      },
      {
        name: 'deploy_task_create',
        path: '/deploy/task/create',
        component: '/deploy/task/create/index',
        meta: { title: '创建任务', requiresAuth: true, permissions: ['deploy:task:create'] }
      },
      {
        name: 'deploy_log',
        path: '/deploy/log',
        component: '/deploy/log/index',
        meta: { title: '执行日志', requiresAuth: true, permissions: ['deploy:log:view'] }
      },
      {
        name: 'deploy_rollback',
        path: '/deploy/rollback',
        component: '/deploy/rollback/index',
        meta: { title: '回滚记录', requiresAuth: true, permissions: ['deploy:rollback:view'] }
      }
    ]
  },
  {
    name: 'system',
    path: '/system',
    component: 'layout.base',
    meta: { title: '系统管理', icon: 'mdi:cog' },
    children: [
      {
        name: 'system_user',
        path: '/system/user',
        component: '/system/user/index',
        meta: { title: '用户管理', requiresAuth: true, permissions: ['system:user:view'] }
      },
      {
        name: 'system_role',
        path: '/system/role',
        component: '/system/role/index',
        meta: { title: '角色管理', requiresAuth: true, permissions: ['system:role:view'] }
      },
      {
        name: 'system_menu',
        path: '/system/menu',
        component: '/system/menu/index',
        meta: { title: '菜单管理', requiresAuth: true, permissions: ['system:menu:view'] }
      },
      {
        name: 'system_audit_log',
        path: '/system/audit-log',
        component: '/system/audit-log/index',
        meta: { title: '审计日志', requiresAuth: true, permissions: ['system:audit-log:view'] }
      }
    ]
  }
];
```

## 资深架构视角分析

### 为什么不建议一开始上微服务

对于一期仅聚焦“主机纳管、检测、批量安装”的系统，业务复杂度还没有高到必须拆分成注册中心、配置中心、网关、认证中心、任务中心、资产中心等多个服务。过早微服务化会显著增加部署复杂度、调试成本、链路治理成本和研发门槛，而业务收益并不明显。[cite:1][cite:5]

模块化单体的最大优势是边界清晰、运行形态简单、事务处理直接、研发协作成本低，同时仍可通过清晰的模块边界为未来演进预留空间。等到后续 Agent 上线、任务执行规模增大、实时采集链路增强之后，再逐步把执行中心、采集中心、认证中心拆分出去，路径会更稳。[cite:3][cite:5]

### 为什么远程执行要先抽象再落地

批量安装本质上依赖执行器，如果一开始把 SSH、上传、日志、检测直接写死在 service 层，后续要支持 Agent、Kubernetes、容器执行器时将产生大量重构成本。将执行模型抽象为 `RemoteExecutor`，同时将检测器与安装器设计为接口，有利于隔离协议细节与业务编排逻辑。[cite:5][cite:15]

换句话说，平台真正的“核心资产”不是一堆控制器和表，而是“执行编排能力”。因此架构上必须将任务定义、执行编排、执行器实现、日志回传分层清楚，这样系统才能在第二阶段自然演进成更成熟的运维平台。

### 为什么任务模型必须状态机化

无论是检测任务还是安装任务，都不能只保留一个简单的成功失败字段。平台必须跟踪 `INIT / PENDING / RUNNING / SUCCESS / FAILED / CANCELLED` 等状态，才能支持幂等校验、重复点击保护、失败重试、审计追踪和 UI 实时展示。[cite:15]

如果后续引入审批、灰度、回滚和分批策略，状态机会进一步扩展，因此建议从第一版开始就把状态字段、开始结束时间、成功失败数量、当前执行步骤等信息纳入主表设计，而不是事后补字段。

## 资深研发专家视角分析

### 一期最容易踩的坑

- 把“CMDB”理解成只有资产录入，忽略与安装任务、检测结果的联动。
- 控制器直接写业务，ServiceImpl 逐渐变成“上帝类”。
- 所有 SQL 堆到单个 mapper 中，后期难以维护。
- 任务执行直接同步阻塞接口，导致请求超时与前端体验极差。
- SSH 密码明文存储，存在严重安全风险。
- 日志只打印本地文件，没有写入任务维度日志表，无法定位批量执行问题。

### 一期应该优先做对的事情

- 先做通用任务框架，再做具体安装模板。
- 先做最常见的 Linux + Java 应用安装模板，再逐步扩展 Nginx、脚本类、Docker 类安装器。
- 主机详情页必须能看到“基础信息 + 最近检测 + 最近任务 + 最近异常”，这样资产才能真正服务安装流程。
- 前端任务详情页必须支持按主机维度查看步骤日志，否则批量执行不可运营。
- 数据库表统一审计字段与逻辑删除字段，方便长期治理。

### 开发节奏建议

第一周完成项目骨架、权限、统一返回、主机 CRUD、菜单与登录。第二周完成主机分组、标签、凭据与主机探测。第三周完成应用定义、版本、安装包、模板管理。第四周完成发布任务、执行日志与任务详情。这个节奏比同时平推所有功能更现实，也更适合团队分工。

## 最终完整版建议

### 最终推荐架构结论

从综合建设成本、可维护性、交付效率与未来演进空间来看，当前最优方案是：**Spring Boot 模块化单体 + Maven 多模块 + MyBatis + Spring Security + JWT + Redis + MySQL + Soybean Admin 前端控制台 + SSH Agentless 执行器**。[cite:1][cite:5][cite:14]

这个方案最符合一期目标，因为它能在不引入过多分布式治理复杂度的前提下，完成主机纳管、检测与应用批量安装闭环。与此同时，采用领域化模块边界、执行器抽象与任务状态机，也为后续拆出 Agent、执行中心、调度中心与更复杂发布能力保留了清晰演进路线。[cite:3][cite:15]

### 最终落地建议

- 一期严格控制范围，只做“主机、检测、应用、安装任务、日志、权限”。
- 所有安装逻辑必须模板化，不允许把具体应用脚本散落在代码中。
- 所有远程执行必须可追踪、可审计、可取消、可失败重试。
- 所有前端页面优先围绕“列表 + 详情 + 执行动作 + 日志回看”四种核心交互设计。
- 从第一版开始为 Agent 化与灰度发布预留扩展接口，但不提前建设重型平台能力。

### 二期演进方向

- 引入 Agent 心跳与指标上报。
- 增加应用升级、配置分发、启停控制。
- 引入审批流与生产环境发布门禁。
- 增加告警中心、通知集成与拓扑关系模型。
- 根据规模决定是否拆分执行中心与采集中心。

## 附录：首批建议实体清单

### 系统域

- `SysUser`
- `SysRole`
- `SysMenu`
- `SysAuditLog`

### 资产域

- `Host`
- `HostGroup`
- `HostTag`
- `HostCredential`
- `HostFact`

### 检测域

- `DetectTask`
- `DetectTaskHost`
- `MetricSnapshot`

### 应用域

- `AppDefinition`
- `AppVersion`
- `AppPackage`
- `ConfigTemplate`
- `ScriptTemplate`

### 部署域

- `DeployTask`
- `DeployTaskHost`
- `DeployTaskStep`
- `DeployTaskLog`
- `DeployTaskParam`

