# EnvOps v0.0.7 Task Center Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the v0.0.7 unified Task Center tracking entry, keeping the v0.0.6 lightweight drawer while adding a full tracking page backed by `GET /api/task-center/tasks/{id}/tracking`.

**Architecture:** Keep list and drawer contracts unchanged, add a read-only tracking query layer in `envops-task`, and assemble a shared tracking view with task-type-specific assemblers. The full tracking page is a hidden authenticated child route under Task Center, uses the new tracking endpoint, and only renders basic info, timeline, log summary, and source module entries.

**Tech Stack:** Spring Boot 3.3, Java records, MyBatis, H2 test data, JUnit 5, MockMvc, Vue 3, TypeScript, Naive UI, elegant-router, Vitest.

---

## Scope Guardrails

This plan implements only the approved v0.0.7 tracking scope:

- Keep `GET /api/task-center/tasks` as the unified list endpoint.
- Keep `GET /api/task-center/tasks/{id}` as the lightweight drawer endpoint.
- Add `GET /api/task-center/tasks/{id}/tracking` for the full tracking page.
- Keep the drawer-first interaction and add a `查看完整追踪` entry inside the drawer.
- Add one full tracking page with four fixed sections: basic info, status timeline, log summary, source module log/detail entries.
- Let Deploy historical unified tasks enter tracking with degraded data.
- Only guarantee full tracking data for database connectivity and Traffic tasks created after v0.0.7.

This plan must not add:

- Unified task retry.
- Unified task cancel.
- Multi-task orchestration.
- A unified full log page or log platform.
- Database or Traffic full historical backfill.
- A new executor or execution-engine abstraction.
- Domain details beyond what the source modules already expose.

## File Structure

### Backend application and tracking assembly

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationService.java`
  - Keep current list/detail methods and DTOs.
  - Add `getTaskTracking(Long id)`.
  - Add tracking response records: `UnifiedTaskTrackingDetail`, `UnifiedTaskTrackingBasicInfo`, `UnifiedTaskTimelineItem`, `UnifiedTaskSourceLink`.
  - Delegate task-type-specific fields to tracking assemblers.

- Create `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/UnifiedTaskTrackingViewAssembler.java`
  - Defines the task-type assembler interface used by the application service.

- Create `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/TrackingViewSupport.java`
  - Shared utilities for parsing `detailPreview`, formatting base timeline nodes, and building fallback source links.

- Create `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/DeployTrackingViewAssembler.java`
  - Builds Deploy timeline/log summary/source links from unified row data plus existing Deploy task/log access where available.
  - Supports degraded historical display when Deploy source detail/log data is unavailable.

- Create `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/DatabaseConnectivityTrackingViewAssembler.java`
  - Builds database connectivity tracking from unified row data and v0.0.7 tracking snapshot fields.
  - Falls back to degraded summary when old database tasks do not have tracking snapshots.

- Create `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/TrafficActionTrackingViewAssembler.java`
  - Builds Traffic action tracking from unified row data and v0.0.7 tracking snapshot fields.
  - Falls back to degraded summary when old Traffic tasks do not have tracking snapshots.

### Backend persistence

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterEntity.java`
  - Add optional `trackingTimeline`, `trackingLogSummary`, and `logRoute` fields.

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
  - Select the new optional tracking columns.
  - Insert the new optional tracking columns.
  - Add `updateTrackingSnapshot(Long id, String trackingTimeline, String trackingLogSummary, String logRoute)`.

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterRow.java`
  - Add optional `trackingTimeline`, `trackingLogSummary`, and `logRoute` fields for read-side assembly.

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
  - Keep existing `CreateCommand`, `UpdateCommand`, `UpsertBySourceCommand`, and `UpdateBySourceCommand` compatible with current callers.
  - Add a narrow `updateTrackingSnapshot` method for post-run tracking snapshots.
  - Do not add a new executor abstraction.

- Modify `backend/envops-boot/src/main/resources/schema.sql`
  - Add nullable columns to `unified_task_center`: `tracking_timeline`, `tracking_log_summary`, `log_route`.
  - Add route metadata columns to `sys_menu_route`: `hide_in_menu`, `active_menu`.

- Modify `backend/envops-boot/src/test/resources/schema.sql`
  - Mirror the production schema changes.

### Backend source modules

- Modify `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
  - After v0.0.7 database checks complete, persist the tracking snapshot on the unified task row.
  - Snapshot only the unified tracking view: timeline nodes and log summary.
  - Do not create a database connectivity history table.

- Modify `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
  - After v0.0.7 Traffic actions complete or fail, persist the tracking snapshot on the unified task row.
  - Snapshot only action type, strategy, plugin, rollback-token availability, status, and error summary.
  - Do not create a Traffic action history table.

- Modify `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskApplicationService.java`
  - Ensure Deploy source route and log route are exposed as `/deploy/task?taskId={id}` and `/deploy/task?taskId={id}&detailTab=logs`.
  - Do not alter Deploy execution controls.

- Modify `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskExecutionApplicationService.java`
  - When current Deploy tasks update the unified projection, also update tracking snapshot when source data is available.
  - Historical Deploy tasks without richer tracking data must still work via degraded assembly.

### Backend route metadata

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RouteMenuMapper.java`
  - Select `hide_in_menu AS hideInMenu` and `active_menu AS activeMenu`.
  - Add fields to `RouteRow`.

- Modify `backend/envops-system/src/main/java/com/img/envops/modules/system/application/route/RouteApplicationService.java`
  - Extend `Meta` to `Meta(String title, String i18nKey, String icon, Integer order, Boolean hideInMenu, String activeMenu)`.
  - Populate `hideInMenu` and `activeMenu` from `RouteRow`.

- Modify `backend/envops-boot/src/main/resources/data.sql`
  - Add hidden user route `task_tracking` under `task`: `/task/tracking/:id`, `view.task_tracking_[id]`, title `任务追踪`, `hide_in_menu = TRUE`, `active_menu = 'task_center'`.

- Modify `backend/envops-boot/src/test/resources/data.sql`
  - Mirror the route seed.

### Backend controller

- Modify `backend/envops-task/src/main/java/com/img/envops/modules/task/interfaces/UnifiedTaskCenterController.java`
  - Add `GET /api/task-center/tasks/{id}/tracking`.
  - Return `R<UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail>`.
  - Do not add mutation endpoints.

### Frontend API and types

- Modify `frontend/src/service/api/task.ts`
  - Add `fetchGetTaskCenterTaskTracking(id: number)` calling `/api/task-center/tasks/${id}/tracking`.

- Modify `frontend/src/typings/api/task.d.ts`
  - Add tracking DTO types without changing `TaskCenterDetail`.

- Modify `frontend/src/typings/api/route.d.ts`
  - Ensure route metadata accepts `hideInMenu?: boolean | null` and `activeMenu?: App.Global.RouteKey | null` through existing `ElegantConstRoute` typing.

### Frontend routing and tracking page

- Create `frontend/src/views/task/tracking/[id].vue`
  - Full tracking page.
  - Fetches tracking detail by `route.params.id`.
  - Renders exactly four sections: basic info, status timeline, log summary, source module entries.
  - Uses `logRoute`, `sourceRoute`, and optional source links for navigation.
  - Does not render retry, cancel, orchestration, or full-log controls.

- Modify generated route files by running `pnpm --dir frontend gen-route` after creating the page:
  - `frontend/src/router/elegant/routes.ts`
  - `frontend/src/router/elegant/imports.ts`
  - `frontend/src/router/elegant/transform.ts`
  - `frontend/src/typings/elegant-router.d.ts`

- Modify `frontend/src/views/task/center/index.vue`
  - Keep current drawer behavior.
  - Add `查看完整追踪` button inside the drawer.
  - Navigate to route key `task_tracking_[id]` with route param `{ id: String(activeTaskDetail.id) }`.

- Modify locale files that contain `page.envops.taskCenter` and route labels.
  - Add action text `查看完整追踪`.
  - Add tracking page labels for the four sections.
  - Add `route.task_tracking_[id]` label.

### Frontend tests

- Modify `frontend/src/views/task/task-contract.spec.ts`
  - Assert the drawer still uses `fetchGetTaskCenterTaskDetail`.
  - Assert the drawer adds a full tracking entry.
  - Assert the tracking page uses `fetchGetTaskCenterTaskTracking`.
  - Assert tracking page renders the four fixed sections and does not contain retry/cancel/full-log platform actions.

- Modify `frontend/src/store/modules/__tests__/route-envops.spec.ts`
  - Assert backend-provided hidden route metadata is respected in menu generation.
  - Assert `task_tracking_[id]` does not appear as a menu item.
  - Assert breadcrumb/active menu points back to `task_center`.

### Backend tests

- Modify `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`
  - Add unit tests for tracking assemblers and degraded fallback behavior.

- Modify `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`
  - Add integration tests for `/api/task-center/tasks/{id}/tracking` with Deploy historical degraded data and current Deploy tracking data.

- Modify `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
  - Add integration tests proving new database connectivity tasks write tracking snapshots and can be queried through the tracking endpoint.

- Modify `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`
  - Add integration tests proving new Traffic actions write tracking snapshots and can be queried through the tracking endpoint.

- Modify `backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java`
  - Assert `task_tracking_[id]` is returned as a hidden child route with `activeMenu = task_center`.

### Documentation and release notes

- Modify `README.md`
  - Update Task Center description from unified list/drawer to unified list/drawer plus full tracking page.

- Modify `docs/envops-项目详细说明.md`
  - Add v0.0.7 unified tracking page behavior and scope boundary.

- Modify `docs/envops-开发技术说明.md`
  - Document tracking endpoint, assembler structure, and route metadata fields.

- Modify `docs/envops-用户操作手册.md`
  - Add user steps for opening the drawer and then full tracking page.

- Create `release/0.0.7-release-notes.md`
  - Summarize v0.0.7 tracking scope, exclusions, and validation commands.

---

## Route Strategy Decision

Use a hidden authenticated dynamic route under the existing `task` route:

- Route key: `task_tracking_[id]`
- Route path: `/task/tracking/:id`
- Component: `view.task_tracking_[id]`
- Parent: `task`
- `meta.hideInMenu = true`
- `meta.activeMenu = 'task_center'`

Why this route strategy is required:

- The tracking page must be authenticated, so it must not be a `constant` route. `frontend/src/router/guard/route.ts` treats `meta.constant` routes as not requiring login.
- The app uses dynamic auth routes from `GET /api/routes/getUserRoutes`, so the hidden page must be present in backend route seed data.
- The frontend menu already honors `meta.hideInMenu`, and breadcrumbs already honor `meta.activeMenu`, but the backend route payload currently does not expose these fields. v0.0.7 adds only these route metadata fields, not a new route system.
- The tracking page should be reachable from the Task Center drawer, but should not become a permanent left-menu item.

---

## Task 1: Add backend tracking schema and route metadata schema

**Files:**
- Modify: `backend/envops-boot/src/main/resources/schema.sql`
- Modify: `backend/envops-boot/src/test/resources/schema.sql`

- [ ] **Step 1: Add failing schema assertions in boot tests**

Add expectations later in `AuthRouteControllerTest` and tracking endpoint tests that require these columns:

```java
.andExpect(jsonPath("$.data.routes[5].children[1].meta.hideInMenu").value(true))
.andExpect(jsonPath("$.data.routes[5].children[1].meta.activeMenu").value("task_center"));
```

Tracking endpoint tests will insert rows with:

```sql
tracking_timeline
tracking_log_summary
log_route
```

- [ ] **Step 2: Run route test and verify it fails before schema support**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AuthRouteControllerTest test
```

Expected: FAIL because hidden route metadata is not present yet.

- [ ] **Step 3: Update production schema**

In `backend/envops-boot/src/main/resources/schema.sql`, extend `unified_task_center`:

```sql
tracking_timeline TEXT,
tracking_log_summary TEXT,
log_route VARCHAR(512),
```

Add the fields near the existing `detail_preview`, `source_route`, and `error_summary` columns so the projection data stays grouped.

In the `sys_menu_route` table definition, add:

```sql
hide_in_menu BOOLEAN DEFAULT FALSE,
active_menu VARCHAR(128),
```

- [ ] **Step 4: Update test schema**

Apply the same schema changes to `backend/envops-boot/src/test/resources/schema.sql`:

```sql
tracking_timeline TEXT,
tracking_log_summary TEXT,
log_route VARCHAR(512),
```

and:

```sql
hide_in_menu BOOLEAN DEFAULT FALSE,
active_menu VARCHAR(128),
```

- [ ] **Step 5: Run schema-dependent boot route test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AuthRouteControllerTest test
```

Expected: still FAIL until route metadata mapper and seed data are implemented.

---

## Task 2: Add hidden tracking route seed and backend route metadata

**Files:**
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/infrastructure/mapper/RouteMenuMapper.java`
- Modify: `backend/envops-system/src/main/java/com/img/envops/modules/system/application/route/RouteApplicationService.java`
- Modify: `backend/envops-boot/src/main/resources/data.sql`
- Modify: `backend/envops-boot/src/test/resources/data.sql`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AuthRouteControllerTest.java`

- [ ] **Step 1: Write failing route metadata test**

In `AuthRouteControllerTest.getUserRoutesReturnsEnvOpsShellRoutesWithLocalizedMeta`, update the Task route assertions from one child to two children:

```java
.andExpect(jsonPath("$.data.routes[5].name").value("task"))
.andExpect(jsonPath("$.data.routes[5].children.length()").value(2))
.andExpect(jsonPath("$.data.routes[5].children[0].name").value("task_center"))
.andExpect(jsonPath("$.data.routes[5].children[0].component").value("view.task_center"))
.andExpect(jsonPath("$.data.routes[5].children[0].meta.hideInMenu").value(false))
.andExpect(jsonPath("$.data.routes[5].children[1].name").value("task_tracking_[id]"))
.andExpect(jsonPath("$.data.routes[5].children[1].path").value("/task/tracking/:id"))
.andExpect(jsonPath("$.data.routes[5].children[1].component").value("view.task_tracking_[id]"))
.andExpect(jsonPath("$.data.routes[5].children[1].meta.hideInMenu").value(true))
.andExpect(jsonPath("$.data.routes[5].children[1].meta.activeMenu").value("task_center"));
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AuthRouteControllerTest test
```

Expected: FAIL because the route metadata and hidden route are not implemented.

- [ ] **Step 3: Extend route mapper row**

In `RouteMenuMapper`, add `hide_in_menu` and `active_menu` to both SELECT statements:

```java
hide_in_menu AS hideInMenu,
active_menu AS activeMenu,
```

Add fields and accessors to `RouteRow`:

```java
private Boolean hideInMenu;
private String activeMenu;

public Boolean getHideInMenu() {
  return hideInMenu;
}

public void setHideInMenu(Boolean hideInMenu) {
  this.hideInMenu = hideInMenu;
}

public String getActiveMenu() {
  return activeMenu;
}

public void setActiveMenu(String activeMenu) {
  this.activeMenu = activeMenu;
}
```

- [ ] **Step 4: Extend route application metadata**

In `RouteApplicationService.toBuilder`, construct meta with the two new fields:

```java
new Meta(
    row.getTitle(),
    buildI18nKey(row.getRouteName()),
    row.getIcon(),
    row.getRouteOrder() == null ? 0 : row.getRouteOrder(),
    Boolean.TRUE.equals(row.getHideInMenu()),
    row.getActiveMenu())
```

Change the record to:

```java
public record Meta(String title,
                   String i18nKey,
                   String icon,
                   Integer order,
                   Boolean hideInMenu,
                   String activeMenu) {
}
```

- [ ] **Step 5: Add hidden route seed**

In both `backend/envops-boot/src/main/resources/data.sql` and `backend/envops-boot/src/test/resources/data.sql`, add a Task child row after `task_center`:

```sql
(252, 250, 'task_tracking_[id]', '/task/tracking/:id', 'view.task_tracking_[id]', '任务追踪', NULL, 2, 'USER', 'SUPER_ADMIN', FALSE, TRUE, 'task_center'),
```

If the existing `sys_menu_route` insert column list does not include the new fields, extend it to include:

```sql
hide_in_menu, active_menu
```

For existing route rows, use:

```sql
FALSE, NULL
```

- [ ] **Step 6: Run route test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AuthRouteControllerTest test
```

Expected: PASS.

---

## Task 3: Add backend tracking projection fields and recorder support

**Files:**
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterEntity.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/entity/UnifiedTaskCenterRow.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/infrastructure/mapper/UnifiedTaskCenterMapper.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskRecorder.java`
- Test: `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`

- [ ] **Step 1: Write failing recorder test**

Add this test to `UnifiedTaskCenterApplicationServiceTest`:

```java
@Test
void recorderUpdatesTrackingSnapshotWithoutChangingTaskStatus() {
  UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
  UnifiedTaskRecorder recorder = new UnifiedTaskRecorder(mapper);

  recorder.updateTrackingSnapshot(new UnifiedTaskRecorder.TrackingSnapshotCommand(
      9001L,
      "[{\"label\":\"任务开始\",\"status\":\"success\"}]",
      "Deploy 日志摘要",
      "/deploy/task?taskId=2001&detailTab=logs"));

  verify(mapper).updateTrackingSnapshot(
      eq(9001L),
      eq("[{\"label\":\"任务开始\",\"status\":\"success\"}]"),
      eq("Deploy 日志摘要"),
      eq("/deploy/task?taskId=2001&detailTab=logs"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: FAIL because `TrackingSnapshotCommand` and `updateTrackingSnapshot` do not exist.

- [ ] **Step 3: Add entity and row fields**

In `UnifiedTaskCenterEntity`, add:

```java
private String trackingTimeline;
private String trackingLogSummary;
private String logRoute;

public String getTrackingTimeline() {
  return trackingTimeline;
}

public void setTrackingTimeline(String trackingTimeline) {
  this.trackingTimeline = trackingTimeline;
}

public String getTrackingLogSummary() {
  return trackingLogSummary;
}

public void setTrackingLogSummary(String trackingLogSummary) {
  this.trackingLogSummary = trackingLogSummary;
}

public String getLogRoute() {
  return logRoute;
}

public void setLogRoute(String logRoute) {
  this.logRoute = logRoute;
}
```

Apply the same fields/accessors to `UnifiedTaskCenterRow`.

- [ ] **Step 4: Update mapper selects, insert, and update**

In `UnifiedTaskCenterMapper`, include the columns in every row select:

```sql
tracking_timeline AS trackingTimeline,
tracking_log_summary AS trackingLogSummary,
log_route AS logRoute,
```

Add the columns to insert statements:

```sql
tracking_timeline,
tracking_log_summary,
log_route,
```

and values:

```sql
#{trackingTimeline},
#{trackingLogSummary},
#{logRoute},
```

Add mapper method:

```java
@Update("""
    UPDATE unified_task_center
    SET tracking_timeline = #{trackingTimeline},
        tracking_log_summary = #{trackingLogSummary},
        log_route = #{logRoute},
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
    """)
void updateTrackingSnapshot(@Param("id") Long id,
                            @Param("trackingTimeline") String trackingTimeline,
                            @Param("trackingLogSummary") String trackingLogSummary,
                            @Param("logRoute") String logRoute);
```

- [ ] **Step 5: Add recorder command**

In `UnifiedTaskRecorder`, add:

```java
public record TrackingSnapshotCommand(
    Long id,
    String trackingTimeline,
    String trackingLogSummary,
    String logRoute) {}

public void updateTrackingSnapshot(TrackingSnapshotCommand command) {
  unifiedTaskCenterMapper.updateTrackingSnapshot(
      command.id(),
      command.trackingTimeline(),
      command.trackingLogSummary(),
      command.logRoute());
}
```

- [ ] **Step 6: Run task service tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: PASS.

---

## Task 4: Add tracking DTOs, endpoint, and read-side assemblers

**Files:**
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationService.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/UnifiedTaskTrackingViewAssembler.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/TrackingViewSupport.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/DeployTrackingViewAssembler.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/DatabaseConnectivityTrackingViewAssembler.java`
- Create: `backend/envops-task/src/main/java/com/img/envops/modules/task/application/tracking/TrafficActionTrackingViewAssembler.java`
- Modify: `backend/envops-task/src/main/java/com/img/envops/modules/task/interfaces/UnifiedTaskCenterController.java`
- Test: `backend/envops-task/src/test/java/com/img/envops/modules/task/application/UnifiedTaskCenterApplicationServiceTest.java`

- [ ] **Step 1: Write failing application service tracking tests**

Add tests for three cases:

```java
@Test
void trackingDetailIncludesBaseInfoTimelineSummaryAndSourceLinks() {
  UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
  UnifiedTaskCenterRow row = new UnifiedTaskCenterRow();
  row.setId(9001L);
  row.setTaskType("deploy");
  row.setTaskName("Deploy order-service");
  row.setStatus("success");
  row.setTriggeredBy("release-admin");
  row.setStartedAt(LocalDateTime.of(2026, 4, 23, 10, 0));
  row.setFinishedAt(LocalDateTime.of(2026, 4, 23, 10, 5));
  row.setSummary("Deploy completed");
  row.setDetailPreview("{\"app\":\"order-service\"}");
  row.setSourceId(2001L);
  row.setSourceRoute("/deploy/task?taskId=2001");
  row.setTrackingTimeline("[{\"label\":\"执行完成\",\"status\":\"success\",\"occurredAt\":\"2026-04-23T10:05:00\"}]");
  row.setTrackingLogSummary("Deploy 日志摘要");
  row.setLogRoute("/deploy/task?taskId=2001&detailTab=logs");

  when(mapper.findById(9001L)).thenReturn(row);

  UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
      mapper,
      List.of(
          new DeployTrackingViewAssembler(new TrackingViewSupport()),
          new DatabaseConnectivityTrackingViewAssembler(new TrackingViewSupport()),
          new TrafficActionTrackingViewAssembler(new TrackingViewSupport())));

  UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9001L);

  assertThat(detail.basicInfo().taskType()).isEqualTo("deploy");
  assertThat(detail.timeline()).extracting("label").contains("执行完成");
  assertThat(detail.logSummary()).isEqualTo("Deploy 日志摘要");
  assertThat(detail.logRoute()).isEqualTo("/deploy/task?taskId=2001&detailTab=logs");
  assertThat(detail.sourceLinks()).extracting("route").contains("/deploy/task?taskId=2001");
}
```

Add a degraded Deploy history test:

```java
@Test
void deployHistoryTrackingFallsBackWhenSnapshotIsMissing() {
  UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
  UnifiedTaskCenterRow row = new UnifiedTaskCenterRow();
  row.setId(9002L);
  row.setTaskType("deploy");
  row.setTaskName("Historical Deploy");
  row.setStatus("failed");
  row.setStartedAt(LocalDateTime.of(2026, 4, 20, 9, 0));
  row.setSummary("Historical deploy failed");
  row.setSourceRoute("/deploy/task?taskId=1999");
  row.setErrorSummary("ssh timeout");

  when(mapper.findById(9002L)).thenReturn(row);

  UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
      mapper,
      List.of(new DeployTrackingViewAssembler(new TrackingViewSupport())));

  UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9002L);

  assertThat(detail.degraded()).isTrue();
  assertThat(detail.timeline()).extracting("label").contains("任务开始", "任务失败");
  assertThat(detail.logSummary()).contains("Historical deploy failed");
  assertThat(detail.sourceLinks()).extracting("route").contains("/deploy/task?taskId=1999");
}
```

Add a database/traffic old-task fallback test:

```java
@Test
void nonDeployOldTrackingFallsBackWithoutPromisingFullHistory() {
  UnifiedTaskCenterMapper mapper = mock(UnifiedTaskCenterMapper.class);
  UnifiedTaskCenterRow row = new UnifiedTaskCenterRow();
  row.setId(9003L);
  row.setTaskType("traffic_action");
  row.setTaskName("Traffic Apply");
  row.setStatus("success");
  row.setStartedAt(LocalDateTime.of(2026, 4, 22, 8, 30));
  row.setFinishedAt(LocalDateTime.of(2026, 4, 22, 8, 31));
  row.setSummary("apply success");
  row.setSourceRoute("/traffic/controller");

  when(mapper.findById(9003L)).thenReturn(row);

  UnifiedTaskCenterApplicationService service = new UnifiedTaskCenterApplicationService(
      mapper,
      List.of(new TrafficActionTrackingViewAssembler(new TrackingViewSupport())));

  UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail detail = service.getTaskTracking(9003L);

  assertThat(detail.degraded()).isTrue();
  assertThat(detail.logSummary()).contains("apply success");
  assertThat(detail.sourceLinks()).extracting("route").contains("/traffic/controller");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: FAIL because tracking DTOs and assemblers do not exist.

- [ ] **Step 3: Create assembler interface**

Create `UnifiedTaskTrackingViewAssembler.java`:

```java
package com.img.envops.modules.task.application.tracking;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.TrackingViewParts;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;

public interface UnifiedTaskTrackingViewAssembler {
  boolean supports(String taskType);

  TrackingViewParts assemble(UnifiedTaskCenterRow row);
}
```

- [ ] **Step 4: Create shared support**

Create `TrackingViewSupport.java` with methods:

```java
package com.img.envops.modules.task.application.tracking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public class TrackingViewSupport {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  public Map<String, Object> parseDetailPreview(UnifiedTaskCenterRow row) {
    if (!StringUtils.hasText(row.getDetailPreview())) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(row.getDetailPreview(), new TypeReference<LinkedHashMap<String, Object>>() {});
    } catch (Exception exception) {
      return Map.of();
    }
  }

  public List<UnifiedTaskTimelineItem> parseTimelineSnapshot(UnifiedTaskCenterRow row) {
    if (!StringUtils.hasText(row.getTrackingTimeline())) {
      return List.of();
    }
    try {
      return objectMapper.readValue(row.getTrackingTimeline(), new TypeReference<List<UnifiedTaskTimelineItem>>() {});
    } catch (Exception exception) {
      return List.of();
    }
  }

  public List<UnifiedTaskTimelineItem> fallbackTimeline(UnifiedTaskCenterRow row) {
    List<UnifiedTaskTimelineItem> timeline = new ArrayList<>();
    timeline.add(new UnifiedTaskTimelineItem("任务开始", "success", format(row.getStartedAt()), row.getSummary()));
    if (row.getFinishedAt() != null) {
      timeline.add(new UnifiedTaskTimelineItem(resolveFinishedLabel(row.getStatus()), row.getStatus(), format(row.getFinishedAt()), row.getErrorSummary()));
    }
    return timeline;
  }

  public List<UnifiedTaskSourceLink> sourceLinks(UnifiedTaskCenterRow row) {
    List<UnifiedTaskSourceLink> links = new ArrayList<>();
    if (StringUtils.hasText(row.getSourceRoute())) {
      links.add(new UnifiedTaskSourceLink("detail", "查看原模块详情", row.getSourceRoute()));
    }
    if (StringUtils.hasText(row.getLogRoute())) {
      links.add(new UnifiedTaskSourceLink("log", "查看原模块日志", row.getLogRoute()));
    }
    return links;
  }

  public String fallbackLogSummary(UnifiedTaskCenterRow row) {
    if (StringUtils.hasText(row.getTrackingLogSummary())) {
      return row.getTrackingLogSummary();
    }
    if (StringUtils.hasText(row.getErrorSummary())) {
      return row.getSummary() + "；失败原因：" + row.getErrorSummary();
    }
    return StringUtils.hasText(row.getSummary()) ? row.getSummary() : "暂无日志摘要";
  }

  private String resolveFinishedLabel(String status) {
    return "failed".equals(status) ? "任务失败" : "任务完成";
  }

  private String format(java.time.LocalDateTime value) {
    return value == null ? null : formatter.format(value);
  }
}
```

- [ ] **Step 5: Add tracking records and method to application service**

In `UnifiedTaskCenterApplicationService`, inject assemblers:

```java
private final List<UnifiedTaskTrackingViewAssembler> trackingAssemblers;

public UnifiedTaskCenterApplicationService(UnifiedTaskCenterMapper unifiedTaskCenterMapper,
                                           List<UnifiedTaskTrackingViewAssembler> trackingAssemblers) {
  this.unifiedTaskCenterMapper = unifiedTaskCenterMapper;
  this.trackingAssemblers = trackingAssemblers;
}
```

Add method:

```java
public UnifiedTaskTrackingDetail getTaskTracking(Long id) {
  UnifiedTaskCenterRow row = unifiedTaskCenterMapper.findById(id);
  if (row == null) {
    throw new NotFoundException("unified task not found: " + id);
  }

  TrackingViewParts parts = trackingAssemblers.stream()
      .filter(assembler -> assembler.supports(row.getTaskType()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("unsupported unified task type: " + row.getTaskType()))
      .assemble(row);

  return new UnifiedTaskTrackingDetail(
      row.getId(),
      new UnifiedTaskTrackingBasicInfo(
          row.getTaskType(),
          row.getTaskName(),
          row.getStatus(),
          row.getTriggeredBy(),
          formatDateTime(row.getStartedAt()),
          formatDateTime(row.getFinishedAt())),
      parts.timeline(),
      parts.logSummary(),
      parts.logRoute(),
      parseDetailPreview(row),
      parts.sourceLinks(),
      parts.degraded());
}
```

Add records:

```java
public record UnifiedTaskTrackingDetail(
    Long id,
    UnifiedTaskTrackingBasicInfo basicInfo,
    List<UnifiedTaskTimelineItem> timeline,
    String logSummary,
    String logRoute,
    Map<String, Object> detailPreview,
    List<UnifiedTaskSourceLink> sourceLinks,
    boolean degraded) {}

public record UnifiedTaskTrackingBasicInfo(
    String taskType,
    String taskName,
    String status,
    String triggeredBy,
    String startedAt,
    String finishedAt) {}

public record UnifiedTaskTimelineItem(String label, String status, String occurredAt, String description) {}

public record UnifiedTaskSourceLink(String type, String label, String route) {}

public record TrackingViewParts(
    List<UnifiedTaskTimelineItem> timeline,
    String logSummary,
    String logRoute,
    List<UnifiedTaskSourceLink> sourceLinks,
    boolean degraded) {}
```

- [ ] **Step 6: Create task-type assemblers**

`DeployTrackingViewAssembler`:

```java
package com.img.envops.modules.task.application.tracking;

import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.TrackingViewParts;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskSourceLink;
import com.img.envops.modules.task.application.UnifiedTaskCenterApplicationService.UnifiedTaskTimelineItem;
import com.img.envops.modules.task.infrastructure.entity.UnifiedTaskCenterRow;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeployTrackingViewAssembler implements UnifiedTaskTrackingViewAssembler {
  private final TrackingViewSupport support;

  public DeployTrackingViewAssembler(TrackingViewSupport support) {
    this.support = support;
  }

  @Override
  public boolean supports(String taskType) {
    return "deploy".equals(taskType);
  }

  @Override
  public TrackingViewParts assemble(UnifiedTaskCenterRow row) {
    List<UnifiedTaskTimelineItem> timeline = support.parseTimelineSnapshot(row);
    boolean degraded = timeline.isEmpty() || !StringUtils.hasText(row.getTrackingLogSummary());
    if (timeline.isEmpty()) {
      timeline = support.fallbackTimeline(row);
    }
    List<UnifiedTaskSourceLink> links = new ArrayList<>(support.sourceLinks(row));
    String logRoute = StringUtils.hasText(row.getLogRoute()) ? row.getLogRoute() : buildDeployLogRoute(row);
    if (StringUtils.hasText(logRoute) && links.stream().noneMatch(link -> "log".equals(link.type()))) {
      links.add(new UnifiedTaskSourceLink("log", "查看原模块日志", logRoute));
    }
    return new TrackingViewParts(timeline, support.fallbackLogSummary(row), logRoute, links, degraded);
  }

  private String buildDeployLogRoute(UnifiedTaskCenterRow row) {
    if (row.getSourceId() != null) {
      return "/deploy/task?taskId=" + row.getSourceId() + "&detailTab=logs";
    }
    return null;
  }
}
```

`DatabaseConnectivityTrackingViewAssembler` and `TrafficActionTrackingViewAssembler` should use the same pattern without Deploy log-route derivation:

```java
@Override
public TrackingViewParts assemble(UnifiedTaskCenterRow row) {
  List<UnifiedTaskTimelineItem> timeline = support.parseTimelineSnapshot(row);
  boolean degraded = timeline.isEmpty() || !StringUtils.hasText(row.getTrackingLogSummary());
  if (timeline.isEmpty()) {
    timeline = support.fallbackTimeline(row);
  }
  return new TrackingViewParts(
      timeline,
      support.fallbackLogSummary(row),
      row.getLogRoute(),
      support.sourceLinks(row),
      degraded);
}
```

Use `supports("database_connectivity")` and `supports("traffic_action")` respectively.

- [ ] **Step 7: Register TrackingViewSupport bean**

Add `@Component` to `TrackingViewSupport` or instantiate it through a small `@Bean` configuration. Prefer `@Component`:

```java
@Component
public class TrackingViewSupport {
```

- [ ] **Step 8: Add controller endpoint**

In `UnifiedTaskCenterController`, add:

```java
@GetMapping("/api/task-center/tasks/{id}/tracking")
public R<UnifiedTaskCenterApplicationService.UnifiedTaskTrackingDetail> getTaskTracking(@PathVariable Long id) {
  return R.ok(unifiedTaskCenterApplicationService.getTaskTracking(id));
}
```

- [ ] **Step 9: Run task service tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: PASS.

---

## Task 5: Add tracking snapshots for database connectivity tasks

**Files:**
- Modify: `backend/envops-asset/src/main/java/com/img/envops/modules/asset/application/DatabaseConnectivityService.java`
- Test: `backend/envops-asset/src/test/java/com/img/envops/modules/asset/application/DatabaseConnectivityServiceTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`

- [ ] **Step 1: Write failing service test for database tracking snapshot**

In `DatabaseConnectivityServiceTest`, verify that a completed check updates a tracking snapshot:

```java
verify(unifiedTaskRecorder).updateTrackingSnapshot(argThat(command ->
    command.id().equals(startedTaskId)
        && command.trackingTimeline().contains("检测开始")
        && command.trackingTimeline().contains("检测完成")
        && command.trackingLogSummary().contains("成功")
        && command.logRoute().equals("/asset/database")));
```

- [ ] **Step 2: Run asset service test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test
```

Expected: FAIL because database tracking snapshot is not written yet.

- [ ] **Step 3: Build database tracking snapshot after completion**

In `DatabaseConnectivityService.updateConnectivityTask`, after `unifiedTaskRecorder.update(...)`, add:

```java
unifiedTaskRecorder.updateTrackingSnapshot(new UnifiedTaskRecorder.TrackingSnapshotCommand(
    unifiedTaskId,
    buildConnectivityTimeline(status, report),
    buildConnectivityLogSummary(report, errorSummary),
    SOURCE_ROUTE));
```

Add this helper method to `UnifiedTaskDetailPreviewFactory`:

```java
public String toJsonArray(List<Map<String, Object>> items) {
  try {
    return objectMapper.writeValueAsString(items);
  } catch (JsonProcessingException exception) {
    throw new IllegalArgumentException("failed to serialize tracking timeline", exception);
  }
}
```

Add helper methods in `DatabaseConnectivityService` using the raw timeline-array shape expected by `TrackingViewSupport.parseTimelineSnapshot`:

```java
private String buildConnectivityTimeline(String status, DatabaseConnectivityReport report) {
  return unifiedTaskDetailPreviewFactory.toJsonArray(List.of(
      Map.of("label", "检测开始", "status", "success", "description", "数据库连通性检测已开始"),
      Map.of("label", "检测完成", "status", status, "description", buildConnectivitySummary(report))));
}

private String buildConnectivityLogSummary(DatabaseConnectivityReport report, String errorSummary) {
  String summary = buildConnectivitySummary(report);
  return errorSummary == null || errorSummary.isBlank() ? summary : summary + "；失败摘要：" + errorSummary;
}
```

Do not add any new database connectivity history table.

- [ ] **Step 4: Run asset service test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Add boot integration test for database tracking endpoint**

In `AssetControllerTest`, after a successful `/api/assets/databases/{id}/connectivity-check`, find the latest `database_connectivity` unified task and call:

```java
mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", unifiedTaskId)
    .header("Authorization", accessToken))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.basicInfo.taskType").value("database_connectivity"))
  .andExpect(jsonPath("$.data.timeline.length()").value(2))
  .andExpect(jsonPath("$.data.logSummary").value(org.hamcrest.Matchers.containsString("成功")))
  .andExpect(jsonPath("$.data.sourceLinks[0].route").value("/asset/database"))
  .andExpect(jsonPath("$.data.degraded").value(false));
```

- [ ] **Step 6: Run boot asset test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=AssetControllerTest test
```

Expected: PASS.

---

## Task 6: Add tracking snapshots for Traffic actions

**Files:**
- Modify: `backend/envops-traffic/src/main/java/com/img/envops/modules/traffic/application/TrafficApplicationService.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`

- [ ] **Step 1: Write failing Traffic tracking endpoint test**

In `TrafficControllerTest`, after a successful preview/apply/rollback action, query the unified task tracking endpoint:

```java
JsonNode task = getLatestTrafficTask(accessToken, "Preview checkout-gateway", null);
JsonNode tracking = getTaskTracking(accessToken, task.path("id").asLong());

assertThat(tracking.path("basicInfo").path("taskType").asText()).isEqualTo("traffic_action");
assertThat(tracking.path("timeline")).hasSize(2);
assertThat(tracking.path("logSummary").asText()).contains("preview");
assertThat(tracking.path("sourceLinks").get(0).path("route").asText()).isEqualTo("/traffic/controller");
assertThat(tracking.path("degraded").asBoolean()).isFalse();
```

Add helper:

```java
private JsonNode getTaskTracking(String accessToken, long taskId) throws Exception {
  String body = mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", taskId)
      .header("Authorization", accessToken))
    .andExpect(status().isOk())
    .andReturn()
    .getResponse()
    .getContentAsString();
  return objectMapper.readTree(body).path("data");
}
```

- [ ] **Step 2: Run Traffic test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test
```

Expected: FAIL because Traffic snapshots are not written yet.

- [ ] **Step 3: Persist Traffic tracking snapshot on success and failure**

In `TrafficApplicationService.finishTrafficTask`, after `unifiedTaskRecorder.update(...)`, add:

```java
unifiedTaskRecorder.updateTrackingSnapshot(new UnifiedTaskRecorder.TrackingSnapshotCommand(
    unifiedTaskId,
    buildTrafficTimeline(action, status, errorSummary),
    buildTrafficLogSummary(action, policy, rollbackTokenAvailable, status, errorSummary),
    SOURCE_ROUTE));
```

Add helpers:

```java
private String buildTrafficTimeline(String action, String status, String errorSummary) {
  return unifiedTaskDetailPreviewFactory.toJsonArray(List.of(
      Map.of("label", "动作开始", "status", "success", "description", action + " 动作已提交"),
      Map.of("label", "动作完成", "status", status, "description", errorSummary == null ? action + " 动作完成" : errorSummary)));
}

private String buildTrafficLogSummary(String action,
                                      TrafficPolicyMapper.TrafficPolicyRow policy,
                                      boolean rollbackTokenAvailable,
                                      String status,
                                      String errorSummary) {
  String summary = action + " " + policy.getApp() + "，策略 " + policy.getStrategy()
      + "，插件 " + policy.getPluginType()
      + "，rollback token " + (rollbackTokenAvailable ? "可用" : "不可用")
      + "，状态 " + status;
  return errorSummary == null || errorSummary.isBlank() ? summary : summary + "；失败摘要：" + errorSummary;
}
```

Import `java.util.Map` if needed.

- [ ] **Step 4: Run Traffic integration test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=TrafficControllerTest test
```

Expected: PASS.

---

## Task 7: Add Deploy tracking endpoint behavior and degraded history assertions

**Files:**
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskApplicationService.java`
- Modify: `backend/envops-deploy/src/main/java/com/img/envops/modules/deploy/application/DeployTaskExecutionApplicationService.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`

- [ ] **Step 1: Write failing Deploy degraded tracking test**

In `DeployTaskControllerTest`, use an existing `unified_task_center` Deploy row that has no `tracking_timeline`:

```java
mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", 9001L)
    .header("Authorization", accessToken))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.basicInfo.taskType").value("deploy"))
  .andExpect(jsonPath("$.data.timeline.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
  .andExpect(jsonPath("$.data.logSummary").value(org.hamcrest.Matchers.containsString("Deploy")))
  .andExpect(jsonPath("$.data.sourceLinks[0].route").value(org.hamcrest.Matchers.startsWith("/deploy/task")))
  .andExpect(jsonPath("$.data.degraded").value(true));
```

- [ ] **Step 2: Write current Deploy tracking test**

For a newly created/executed Deploy task, assert a log route:

```java
mockMvc.perform(get("/api/task-center/tasks/{id}/tracking", unifiedTaskId)
    .header("Authorization", accessToken))
  .andExpect(status().isOk())
  .andExpect(jsonPath("$.data.logRoute").value("/deploy/task?taskId=" + deployTaskId + "&detailTab=logs"))
  .andExpect(jsonPath("$.data.sourceLinks[?(@.type == 'log')].route").value(org.hamcrest.Matchers.hasItem("/deploy/task?taskId=" + deployTaskId + "&detailTab=logs")));
```

- [ ] **Step 3: Run Deploy test to verify it fails**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test
```

Expected: FAIL until tracking endpoint and Deploy snapshot/log route behavior are wired.

- [ ] **Step 4: Ensure Deploy log route is generated**

In Deploy projection sync/update paths, set the tracking snapshot after create/update when a unified task id is available. For by-source Deploy writes, either find the unified row by source after upsert or rely on `DeployTrackingViewAssembler` to derive log route from `sourceId`.

Keep this minimal:

```java
private String buildDeployLogRoute(Long taskId) {
  return "/deploy/task?taskId=" + taskId + "&detailTab=logs";
}
```

Do not change Deploy retry/cancel/rollback behavior.

- [ ] **Step 5: Run Deploy integration test**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest test
```

Expected: PASS.

---

## Task 8: Add frontend API types and tracking fetcher

**Files:**
- Modify: `frontend/src/typings/api/task.d.ts`
- Modify: `frontend/src/service/api/task.ts`
- Test: `frontend/src/views/task/task-contract.spec.ts`

- [ ] **Step 1: Write failing frontend contract assertions**

In `task-contract.spec.ts`, add assertions:

```ts
expect(taskApi).toContain('fetchGetTaskCenterTaskTracking');
expect(taskApi).toContain("url: `/api/task-center/tasks/${id}/tracking`");
expect(taskTypings).toContain('interface TaskCenterTrackingDetail');
expect(taskTypings).toContain('interface TaskCenterTrackingBasicInfo');
expect(taskTypings).toContain('interface TaskCenterTimelineItem');
expect(taskTypings).toContain('interface TaskCenterSourceLink');
```

- [ ] **Step 2: Run Vitest to verify it fails**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: FAIL because the API and types do not exist.

- [ ] **Step 3: Add task tracking types**

In `frontend/src/typings/api/task.d.ts`, add:

```ts
interface TaskCenterTrackingBasicInfo {
  taskType: TaskCenterTaskType;
  taskName: string;
  status: UnifiedTaskStatus;
  triggeredBy: string;
  startedAt: string;
  finishedAt?: string | null;
}

interface TaskCenterTimelineItem {
  label: string;
  status: string;
  occurredAt?: string | null;
  description?: string | null;
}

interface TaskCenterSourceLink {
  type: 'log' | 'detail' | 'module' | string;
  label: string;
  route: string;
}

interface TaskCenterTrackingDetail {
  id: number;
  basicInfo: TaskCenterTrackingBasicInfo;
  timeline: TaskCenterTimelineItem[];
  logSummary: string;
  logRoute?: string | null;
  detailPreview: Record<string, unknown>;
  sourceLinks: TaskCenterSourceLink[];
  degraded: boolean;
}
```

Do not change:

```ts
type TaskCenterDetail = TaskCenterTaskDetail;
```

- [ ] **Step 4: Add fetcher**

In `frontend/src/service/api/task.ts`, add:

```ts
export function fetchGetTaskCenterTaskTracking(id: number) {
  return request<Api.Task.TaskCenterTrackingDetail>({
    url: `/api/task-center/tasks/${id}/tracking`
  });
}
```

- [ ] **Step 5: Run frontend contract test**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: PASS for API/type assertions, or FAIL only on tracking page assertions that are implemented in later tasks.

---

## Task 9: Add frontend full tracking page and route generation

**Files:**
- Create: `frontend/src/views/task/tracking/[id].vue`
- Generated after command: `frontend/src/router/elegant/routes.ts`
- Generated after command: `frontend/src/router/elegant/imports.ts`
- Generated after command: `frontend/src/router/elegant/transform.ts`
- Generated after command: `frontend/src/typings/elegant-router.d.ts`
- Test: `frontend/src/views/task/task-contract.spec.ts`

- [ ] **Step 1: Write failing tracking page contract tests**

In `task-contract.spec.ts`, read `frontend/src/views/task/tracking/[id].vue` and assert:

```ts
expect(taskTrackingPage).toContain('fetchGetTaskCenterTaskTracking');
expect(taskTrackingPage).toContain("t('page.envops.taskCenter.tracking.basicInfo.title')");
expect(taskTrackingPage).toContain("t('page.envops.taskCenter.tracking.timeline.title')");
expect(taskTrackingPage).toContain("t('page.envops.taskCenter.tracking.logSummary.title')");
expect(taskTrackingPage).toContain("t('page.envops.taskCenter.tracking.sourceLinks.title')");
expect(taskTrackingPage).not.toContain('retry');
expect(taskTrackingPage).not.toContain('cancel');
expect(taskTrackingPage).not.toContain('fullLog');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: FAIL because the tracking page does not exist.

- [ ] **Step 3: Create tracking page**

Create `frontend/src/views/task/tracking/[id].vue`:

```vue
<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { fetchGetTaskCenterTaskTracking } from '@/service/api';

defineOptions({
  name: 'TaskTrackingPage'
});

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const loading = ref(false);
const trackingDetail = ref<Api.Task.TaskCenterTrackingDetail | null>(null);
const requestToken = ref(0);

const taskId = computed(() => {
  const id = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  const parsed = Number(id);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
});

watch(
  taskId,
  value => {
    if (value) {
      void loadTrackingDetail(value);
    } else {
      trackingDetail.value = null;
    }
  },
  { immediate: true }
);

async function loadTrackingDetail(id: number) {
  const token = ++requestToken.value;
  loading.value = true;

  try {
    const { data, error } = await fetchGetTaskCenterTaskTracking(id);
    if (token !== requestToken.value) {
      return;
    }
    if (!error) {
      trackingDetail.value = data ?? null;
    }
  } finally {
    if (token === requestToken.value) {
      loading.value = false;
    }
  }
}

async function openRoute(routePath: string) {
  await router.push(routePath);
}
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.taskCenter.tracking.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.taskCenter.tracking.hero.description') }}</p>
        </div>
      </div>
    </NCard>

    <NSpin :show="loading">
      <NSpace v-if="trackingDetail" vertical :size="16">
        <NCard :title="t('page.envops.taskCenter.tracking.basicInfo.title')" :bordered="false" class="card-wrapper">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskName')">
              {{ trackingDetail.basicInfo.taskName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskType')">
              {{ trackingDetail.basicInfo.taskType }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.triggeredBy')">
              {{ trackingDetail.basicInfo.triggeredBy || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.startedAt')">
              {{ trackingDetail.basicInfo.startedAt || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.finishedAt')">
              {{ trackingDetail.basicInfo.finishedAt || '-' }}
            </NDescriptionsItem>
          </NDescriptions>
          <NAlert v-if="trackingDetail.degraded" type="warning" class="mt-16px">
            {{ t('page.envops.taskCenter.tracking.degraded') }}
          </NAlert>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.timeline.title')" :bordered="false" class="card-wrapper">
          <NTimeline>
            <NTimelineItem
              v-for="item in trackingDetail.timeline"
              :key="`${item.label}-${item.occurredAt || item.description || ''}`"
              :title="item.label"
              :content="item.description || '-'"
              :time="item.occurredAt || undefined"
            />
          </NTimeline>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.logSummary.title')" :bordered="false" class="card-wrapper">
          <NText>{{ trackingDetail.logSummary || '-' }}</NText>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.sourceLinks.title')" :bordered="false" class="card-wrapper">
          <NSpace>
            <NButton
              v-for="link in trackingDetail.sourceLinks"
              :key="`${link.type}-${link.route}`"
              secondary
              type="primary"
              @click="openRoute(link.route)"
            >
              {{ link.label }}
            </NButton>
          </NSpace>
        </NCard>
      </NSpace>
      <NEmpty v-else class="py-24px" :description="t('common.noData')" />
    </NSpin>
  </NSpace>
</template>

<style scoped></style>
```

- [ ] **Step 4: Generate route files**

Run:

```bash
pnpm --dir frontend gen-route
```

Expected generated updates include:

```ts
"task_tracking_[id]": "/task/tracking/:id"
```

and:

```ts
"task_tracking_[id]"
```

in `LastLevelRouteKey`.

- [ ] **Step 5: Run tracking page contract test**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: PASS for tracking page contract assertions, or FAIL only on drawer entry assertions that are implemented in Task 10.

---

## Task 10: Add drawer full tracking entry while preserving lightweight detail

**Files:**
- Modify: `frontend/src/views/task/center/index.vue`
- Test: `frontend/src/views/task/task-contract.spec.ts`

- [ ] **Step 1: Write failing drawer tracking-entry assertions**

In `task-contract.spec.ts`, assert:

```ts
expect(taskCenterPage).toContain('fetchGetTaskCenterTaskDetail');
expect(taskCenterPage).toContain("t('page.envops.taskCenter.actions.openTaskTracking')");
expect(taskCenterPage).toContain("routerPushByKey('task_tracking_[id]'");
expect(taskCenterPage).toContain('params: { id: String(activeTaskDetail.id) }');
expect(taskCenterPage).toContain("t('page.envops.taskCenter.actions.openSourceDetail')");
```

Also keep the existing assertions:

```ts
expect(taskCenterPage).toContain('showTaskDetailDrawer.value = true');
expect(taskCenterPage).toContain('router.push(activeTaskDetail.value.sourceRoute)');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: FAIL because the drawer tracking entry does not exist.

- [ ] **Step 3: Add navigation handler**

In `frontend/src/views/task/center/index.vue`, add:

```ts
async function openTaskTracking() {
  if (!activeTaskDetail.value?.id) {
    return;
  }

  await routerPushByKey('task_tracking_[id]', {
    params: { id: String(activeTaskDetail.value.id) }
  });
}
```

- [ ] **Step 4: Add drawer button**

Replace the drawer action container with two buttons:

```vue
<div class="mt-16px flex justify-end gap-12px">
  <NButton :disabled="!activeTaskDetail?.id" @click="openTaskTracking">
    {{ t('page.envops.taskCenter.actions.openTaskTracking') }}
  </NButton>
  <NButton type="primary" :disabled="!activeTaskDetail?.sourceRoute" @click="openSourceDetail">
    {{ t('page.envops.taskCenter.actions.openSourceDetail') }}
  </NButton>
</div>
```

Do not change row-click behavior or `handleOpenTaskDetail`.

- [ ] **Step 5: Run drawer contract test**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: PASS.

---

## Task 11: Add route-store tests for hidden tracking route

**Files:**
- Modify: `frontend/src/store/modules/__tests__/route-envops.spec.ts`

- [ ] **Step 1: Write failing hidden-route test**

Add or extend a dynamic route test with a `task_tracking_[id]` child:

```ts
const taskRoute = {
  id: '250',
  name: 'task',
  path: '/task',
  component: 'layout.base',
  meta: { title: '任务中心', i18nKey: 'route.task', icon: 'mdi:clipboard-list', order: 6 },
  children: [
    {
      id: '251',
      name: 'task_center',
      path: '/task/center',
      component: 'view.task_center',
      meta: { title: '全部任务', i18nKey: 'route.task_center', order: 1, hideInMenu: false }
    },
    {
      id: '252',
      name: 'task_tracking_[id]',
      path: '/task/tracking/:id',
      component: 'view.task_tracking_[id]',
      meta: {
        title: '任务追踪',
        i18nKey: 'route.task_tracking_[id]',
        order: 2,
        hideInMenu: true,
        activeMenu: 'task_center'
      }
    }
  ]
};
```

Assert the menu excludes the tracking route:

```ts
expect(routeStore.menus.some(menu => menu.key === 'task_tracking_[id]')).toBe(false);
expect(routeStore.menus.find(menu => menu.key === 'task')?.children?.map(item => item.key)).toEqual(['task_center']);
```

Assert the route still exists:

```ts
expect(routeStore.routes.some(route => route.name === 'task')).toBe(true);
```

- [ ] **Step 2: Run route-store test to verify it fails if frontend route generation is missing**

Run:

```bash
pnpm --dir frontend exec vitest run src/store/modules/__tests__/route-envops.spec.ts
```

Expected: PASS after Task 9 route generation and backend metadata typing; FAIL means the hidden route is either missing from generated route map or still entering menu output.

- [ ] **Step 3: Fix only menu/route metadata handling if needed**

If test fails because `hideInMenu` is ignored, update `frontend/src/store/modules/route/shared.ts` only if current behavior no longer matches the inspected implementation:

```ts
if (!route.meta?.hideInMenu) {
```

This already exists today, so no code change should be needed unless generated route typing strips the field.

- [ ] **Step 4: Run route-store test**

Run:

```bash
pnpm --dir frontend exec vitest run src/store/modules/__tests__/route-envops.spec.ts
```

Expected: PASS.

---

## Task 12: Add frontend locale strings

**Files:**
- Modify locale files containing `page.envops.taskCenter`
- Modify locale files containing route labels for `route.task_center`
- Test: `frontend/src/views/task/task-contract.spec.ts`

- [ ] **Step 1: Find exact locale files**

Run:

```bash
grep -R "page.envops.taskCenter" -n frontend/src/locales frontend/src | head -20
```

Expected: one or more locale files with existing Task Center labels.

- [ ] **Step 2: Add contract assertions for locale keys**

In `task-contract.spec.ts`, add file-content assertions for:

```ts
'page.envops.taskCenter.actions.openTaskTracking'
'page.envops.taskCenter.tracking.hero.title'
'page.envops.taskCenter.tracking.basicInfo.title'
'page.envops.taskCenter.tracking.timeline.title'
'page.envops.taskCenter.tracking.logSummary.title'
'page.envops.taskCenter.tracking.sourceLinks.title'
'page.envops.taskCenter.tracking.degraded'
'route.task_tracking_[id]'
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: FAIL because locale keys are not added yet.

- [ ] **Step 4: Add Chinese labels**

Add these values to the Chinese locale source:

```ts
openTaskTracking: '查看完整追踪'
```

Add tracking labels:

```ts
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
}
```

Add route label:

```ts
'route.task_tracking_[id]': '任务追踪'
```

- [ ] **Step 5: Add English labels if the project has English locale files**

Add:

```ts
openTaskTracking: 'View full tracking'
```

and:

```ts
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
}
```

Add route label:

```ts
'route.task_tracking_[id]': 'Task Tracking'
```

- [ ] **Step 6: Run frontend contract test**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts
```

Expected: PASS.

---

## Task 13: Backend end-to-end tracking endpoint validation

**Files:**
- Test: `backend/envops-boot/src/test/java/com/img/envops/DeployTaskControllerTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/AssetControllerTest.java`
- Test: `backend/envops-boot/src/test/java/com/img/envops/TrafficControllerTest.java`

- [ ] **Step 1: Run focused backend boot tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest,AuthRouteControllerTest test
```

Expected: PASS.

- [ ] **Step 2: Check response shape manually in tests if a failure occurs**

The endpoint response must include exactly these top-level `data` fields:

```json
{
  "id": 9001,
  "basicInfo": {},
  "timeline": [],
  "logSummary": "...",
  "logRoute": "...",
  "detailPreview": {},
  "sourceLinks": [],
  "degraded": false
}
```

Do not add action fields such as:

```json
{
  "retryable": true,
  "cancelable": true,
  "children": []
}
```

- [ ] **Step 3: Run task module unit tests again**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: PASS.

---

## Task 14: Frontend tracking validation

**Files:**
- Test: `frontend/src/views/task/task-contract.spec.ts`
- Test: `frontend/src/views/task/shared/query.spec.ts`
- Test: `frontend/src/store/modules/__tests__/route-envops.spec.ts`

- [ ] **Step 1: Run focused frontend tests**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/views/task/shared/query.spec.ts src/store/modules/__tests__/route-envops.spec.ts
```

Expected: PASS.

- [ ] **Step 2: Run frontend typecheck**

Run:

```bash
pnpm --dir frontend typecheck
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```bash
pnpm --dir frontend build
```

Expected: PASS.

---

## Task 15: Manual UI smoke test

**Files:**
- No required file changes unless bugs are found.

- [ ] **Step 1: Start backend if it is not already running**

Run the project’s normal backend startup command. If no standard local command is documented, use the existing boot module from the IDE or Maven profile used in this repo.

Expected: backend serves `/api/task-center/tasks` and auth endpoints.

- [ ] **Step 2: Start frontend dev server**

Run:

```bash
pnpm --dir frontend dev
```

Expected: Vite dev server starts.

- [ ] **Step 3: Smoke test Task Center in browser**

Open the app, log in, and verify:

1. Task Center list still loads.
2. Clicking a row still opens the lightweight drawer.
3. Drawer still has `查看原始详情`.
4. Drawer now has `查看完整追踪`.
5. `查看完整追踪` opens `/task/tracking/{id}`.
6. Tracking page shows basic info, status timeline, log summary, and source module entries.
7. Tracking route is not visible in the left menu.
8. Source module detail/log buttons navigate to Deploy, database, or Traffic pages.
9. No retry/cancel/orchestration/full-log platform controls appear in Task Center tracking.

- [ ] **Step 4: Record manual UI result in final implementation notes**

If the browser smoke test cannot be performed in the environment, explicitly report:

```text
Manual UI smoke test not performed: <reason>.
```

Do not claim UI verification without performing it.

---

## Task 16: Documentation and release notes

**Files:**
- Modify: `README.md`
- Modify: `docs/envops-项目详细说明.md`
- Modify: `docs/envops-开发技术说明.md`
- Modify: `docs/envops-用户操作手册.md`
- Create: `release/0.0.7-release-notes.md`

- [ ] **Step 1: Update README**

Add a short v0.0.7 Task Center line:

```md
- Task Center 在统一列表和轻量详情抽屉之外，新增统一任务完整追踪页，提供基础信息、状态时间线、日志摘要和原模块入口。
```

Keep exclusions clear:

```md
Task Center 不在 v0.0.7 承担统一重试、取消、多任务编排或完整日志平台职责。
```

- [ ] **Step 2: Update project overview**

In `docs/envops-项目详细说明.md`, describe the two-level flow:

```md
用户先在统一任务列表中打开轻量详情抽屉快速判断任务，再按需进入统一任务完整追踪页查看状态时间线、日志摘要和原模块入口。
```

- [ ] **Step 3: Update developer guide**

In `docs/envops-开发技术说明.md`, document:

```md
GET /api/task-center/tasks/{id}/tracking
```

and assembler boundary:

```md
统一追踪由 envops-task 读侧 application service 组装，按任务类型拆分 Deploy、Database Connectivity、Traffic Action assembler。assembler 只组装追踪视图，不执行任务，不写业务状态。
```

- [ ] **Step 4: Update user manual**

In `docs/envops-用户操作手册.md`, add user steps:

```md
1. 进入 Task Center。
2. 点击任务打开轻量详情抽屉。
3. 点击“查看完整追踪”。
4. 在完整追踪页查看基础信息、状态时间线、日志摘要。
5. 需要更深领域内容时点击原模块日志或原模块详情入口。
```

- [ ] **Step 5: Create release notes**

Create `release/0.0.7-release-notes.md`:

```md
# EnvOps 0.0.7 Release Notes

## Summary

- Task Center 从统一看板推进为统一追踪入口。
- 保留 v0.0.6 统一列表和轻量详情抽屉。
- 新增统一任务完整追踪页和 `GET /api/task-center/tasks/{id}/tracking`。
- 完整追踪页提供基础信息、状态时间线、日志摘要、原模块日志/详情入口。

## Scope

### Included in 0.0.7

- 轻量抽屉中的“查看完整追踪”入口
- 统一任务完整追踪页
- `GET /api/task-center/tasks/{id}/tracking`
- Deploy 历史任务降级进入追踪页
- 数据库连通性检测和 Traffic 动作的新任务追踪快照

### Not included in 0.0.7

- 统一任务内重试
- 统一任务内取消
- 多任务编排
- 统一完整日志平台
- 数据库与 Traffic 全历史补录
- 新执行器或新执行引擎抽象

## Validation

- `mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test`
- `mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test`
- `mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest,AuthRouteControllerTest test`
- `pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/views/task/shared/query.spec.ts src/store/modules/__tests__/route-envops.spec.ts`
- `pnpm --dir frontend typecheck`
- `pnpm --dir frontend build`
```

- [ ] **Step 6: Verify docs mention exclusions**

Run:

```bash
grep -R "统一任务内重试\|统一任务内取消\|多任务编排\|统一完整日志" -n README.md docs release/0.0.7-release-notes.md
```

Expected: docs explicitly state these are not included.

---

## Task 17: Full validation before implementation completion

**Files:**
- No file changes expected.

- [ ] **Step 1: Run backend task unit tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-task -am -Dtest=UnifiedTaskCenterApplicationServiceTest test
```

Expected: PASS.

- [ ] **Step 2: Run backend asset unit tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-asset -am -Dtest=DatabaseConnectivityServiceTest test
```

Expected: PASS.

- [ ] **Step 3: Run backend boot integration tests**

Run:

```bash
mvn -f backend/pom.xml -pl envops-boot -am -Dtest=DeployTaskControllerTest,AssetControllerTest,TrafficControllerTest,AuthRouteControllerTest test
```

Expected: PASS.

- [ ] **Step 4: Run frontend focused tests**

Run:

```bash
pnpm --dir frontend exec vitest run src/views/task/task-contract.spec.ts src/views/task/shared/query.spec.ts src/store/modules/__tests__/route-envops.spec.ts
```

Expected: PASS.

- [ ] **Step 5: Run frontend typecheck**

Run:

```bash
pnpm --dir frontend typecheck
```

Expected: PASS.

- [ ] **Step 6: Run frontend build**

Run:

```bash
pnpm --dir frontend build
```

Expected: PASS.

- [ ] **Step 7: Check scope exclusions in changed code**

Run:

```bash
grep -R "task-center/tasks/.*/retry\|task-center/tasks/.*/cancel\|orchestration\|fullLog" -n backend frontend || true
```

Expected: no new unified Task Center retry/cancel/orchestration/full-log platform implementation.

- [ ] **Step 8: Review git diff**

Run:

```bash
git diff --stat && git diff -- docs/superpowers/specs/2026-04-23-envops-task-center-tracking-design.md docs/superpowers/plans/2026-04-23-envops-task-center-tracking.md
```

Expected: spec remains unchanged, implementation matches plan.

---

## Self-Review Checklist

- Spec coverage:
  - List and lightweight drawer preserved: Tasks 8, 10, 14, 15.
  - Full tracking endpoint added: Tasks 4, 13, 17.
  - Full tracking page added: Tasks 9, 10, 14, 15.
  - Basic info, timeline, log summary, source entries: Tasks 4, 9, 13, 15.
  - Deploy historical degraded display: Tasks 4, 7, 13.
  - Database and Traffic new-task tracking: Tasks 5, 6, 13.
  - No retry/cancel/orchestration/full-log platform/new executor: Scope guardrails and Task 17.

- Placeholder scan:
  - The plan uses concrete files, commands, DTO names, route keys, endpoint paths, and test assertions. It does not leave unspecified implementation slots.

- Type consistency:
  - Backend endpoint returns `UnifiedTaskTrackingDetail`.
  - Frontend endpoint consumes `Api.Task.TaskCenterTrackingDetail`.
  - Route key is consistently `task_tracking_[id]`.
  - Route path is consistently `/task/tracking/:id`.
  - Source route remains existing `sourceRoute`; log entry uses `logRoute`.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-23-envops-task-center-tracking.md`. Two execution options:

**1. Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** - execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
