package com.img.envops.modules.task.infrastructure.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskCenterBackfillRunner implements ApplicationRunner {

  @Override
  public void run(ApplicationArguments args) {
    // Task 1 keeps backfill behavior as a skeleton only.
  }
}
