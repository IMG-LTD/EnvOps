package com.img.envops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.img.envops")
@MapperScan("com.img.envops.modules.*.infrastructure.mapper")
public class EnvOpsApplication {
  public static void main(String[] args) {
    SpringApplication.run(EnvOpsApplication.class, args);
  }
}
