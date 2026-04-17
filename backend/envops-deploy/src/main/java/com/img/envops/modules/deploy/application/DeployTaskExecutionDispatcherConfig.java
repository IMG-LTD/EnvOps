package com.img.envops.modules.deploy.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class DeployTaskExecutionDispatcherConfig {

  @Bean("deployTaskExecutor")
  public TaskExecutor deployTaskExecutor(@Value("${envops.deploy.async:true}") boolean async) {
    return async ? new SimpleAsyncTaskExecutor("deploy-task-") : new SyncTaskExecutor();
  }
}
