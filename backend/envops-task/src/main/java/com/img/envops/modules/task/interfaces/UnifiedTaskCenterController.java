package com.img.envops.modules.task.interfaces;

import com.img.envops.common.response.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/task-center/tasks")
public class UnifiedTaskCenterController {

  @GetMapping
  public R<Object> getTasks() {
    return R.ok(null);
  }

  @GetMapping("/{id}")
  public R<Object> getTaskDetail(@PathVariable Long id) {
    return R.ok(null);
  }
}
