package com.img.envops.modules.asset.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.asset.application.AssetApplicationService;
import com.img.envops.modules.asset.application.DatabaseConnectivityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
  private final AssetApplicationService assetApplicationService;
  private final DatabaseConnectivityService databaseConnectivityService;

  public AssetController(AssetApplicationService assetApplicationService,
                         DatabaseConnectivityService databaseConnectivityService) {
    this.assetApplicationService = assetApplicationService;
    this.databaseConnectivityService = databaseConnectivityService;
  }

  @GetMapping("/hosts")
  public R<AssetApplicationService.HostPage> getHosts(@RequestParam(defaultValue = "1") Integer current,
                                                      @RequestParam(defaultValue = "10") Integer size) {
    return R.ok(assetApplicationService.getHosts(current, size));
  }

  @PostMapping("/hosts")
  public R<AssetApplicationService.HostRecord> createHost(@RequestBody CreateHostRequest request) {
    return R.ok(assetApplicationService.createHost(new AssetApplicationService.CreateHostCommand(
        request.hostName(),
        request.ipAddress(),
        request.environment(),
        request.clusterName(),
        request.ownerName(),
        request.status(),
        request.lastHeartbeat())));
  }

  @GetMapping("/credentials")
  public R<List<AssetApplicationService.CredentialRecord>> getCredentials() {
    return R.ok(assetApplicationService.getCredentials());
  }

  @GetMapping("/databases")
  public R<AssetApplicationService.DatabasePage> getDatabases(@RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) String environment,
                                                              @RequestParam(required = false) String databaseType,
                                                              @RequestParam(required = false) String lifecycleStatus,
                                                              @RequestParam(required = false) String connectivityStatus,
                                                              @RequestParam(defaultValue = "1") Integer current,
                                                              @RequestParam(defaultValue = "10") Integer size) {
    return R.ok(assetApplicationService.getDatabases(
        keyword,
        environment,
        databaseType,
        lifecycleStatus,
        connectivityStatus,
        current,
        size));
  }

  @PostMapping("/databases")
  public R<AssetApplicationService.DatabaseRecord> createDatabase(@RequestBody CreateDatabaseRequest request) {
    return R.ok(assetApplicationService.createDatabase(new AssetApplicationService.CreateDatabaseCommand(
        request.databaseName(),
        request.databaseType(),
        request.environment(),
        request.hostId(),
        request.port(),
        request.instanceName(),
        request.credentialId(),
        request.ownerName(),
        request.lifecycleStatus(),
        request.connectivityStatus(),
        request.connectionUsername(),
        request.connectionPassword(),
        request.description(),
        request.lastCheckedAt())));
  }

  @PutMapping("/databases/{id}")
  public R<AssetApplicationService.DatabaseRecord> updateDatabase(@PathVariable Long id,
                                                                  @RequestBody UpdateDatabaseRequest request) {
    return R.ok(assetApplicationService.updateDatabase(id, new AssetApplicationService.UpdateDatabaseCommand(
        request.databaseName(),
        request.databaseType(),
        request.environment(),
        request.hostId(),
        request.port(),
        request.instanceName(),
        request.credentialId(),
        request.ownerName(),
        request.lifecycleStatus(),
        request.connectivityStatus(),
        request.connectionUsername(),
        request.connectionPassword(),
        request.description(),
        request.lastCheckedAt())));
  }

  @DeleteMapping("/databases/{id}")
  public R<Boolean> deleteDatabase(@PathVariable Long id) {
    return R.ok(assetApplicationService.deleteDatabase(id));
  }

  @PostMapping("/databases/{id}/connectivity-check")
  public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkDatabaseConnectivity(@PathVariable Long id) {
    return R.ok(databaseConnectivityService.checkOneDatabase(id));
  }

  @PostMapping("/databases/connectivity-check:selected")
  public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkSelectedDatabases(@RequestBody BatchDatabaseConnectivityRequest request) {
    return R.ok(databaseConnectivityService.checkSelectedDatabases(request.ids()));
  }

  @PostMapping("/databases/connectivity-check:page")
  public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkCurrentPageDatabases(@RequestBody BatchDatabaseConnectivityRequest request) {
    return R.ok(databaseConnectivityService.checkSelectedDatabases(request.ids()));
  }

  @PostMapping("/databases/connectivity-check:query")
  public R<DatabaseConnectivityService.DatabaseConnectivityReport> checkQueryDatabases(@RequestBody QueryDatabaseConnectivityRequest request) {
    return R.ok(databaseConnectivityService.checkDatabasesByQuery(
        request.keyword(),
        request.environment(),
        request.databaseType(),
        request.lifecycleStatus(),
        request.connectivityStatus()));
  }

  @PostMapping("/credentials")
  public R<AssetApplicationService.CredentialRecord> createCredential(@RequestBody CreateCredentialRequest request) {
    return R.ok(assetApplicationService.createCredential(new AssetApplicationService.CreateCredentialCommand(
        request.name(),
        request.credentialType(),
        request.username(),
        request.secret(),
        request.description())));
  }

  @GetMapping("/groups")
  public R<List<AssetApplicationService.GroupRecord>> getGroups() {
    return R.ok(assetApplicationService.getGroups());
  }

  @GetMapping("/tags")
  public R<List<AssetApplicationService.TagRecord>> getTags() {
    return R.ok(assetApplicationService.getTags());
  }

  public record CreateHostRequest(String hostName,
                                  String ipAddress,
                                  String environment,
                                  String clusterName,
                                  String ownerName,
                                  String status,
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastHeartbeat) {
  }

  public record CreateCredentialRequest(String name,
                                        String credentialType,
                                        String username,
                                        String secret,
                                        String description) {
  }

  public record BatchDatabaseConnectivityRequest(List<Long> ids) {
  }

  public record QueryDatabaseConnectivityRequest(String keyword,
                                                 String environment,
                                                 String databaseType,
                                                 String lifecycleStatus,
                                                 String connectivityStatus) {
  }

  public record CreateDatabaseRequest(String databaseName,
                                      String databaseType,
                                      String environment,
                                      Long hostId,
                                      Integer port,
                                      String instanceName,
                                      Long credentialId,
                                      String ownerName,
                                      String lifecycleStatus,
                                      String connectivityStatus,
                                      String connectionUsername,
                                      String connectionPassword,
                                      String description,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCheckedAt) {
  }

  public record UpdateDatabaseRequest(String databaseName,
                                      String databaseType,
                                      String environment,
                                      Long hostId,
                                      Integer port,
                                      String instanceName,
                                      Long credentialId,
                                      String ownerName,
                                      String lifecycleStatus,
                                      String connectivityStatus,
                                      String connectionUsername,
                                      String connectionPassword,
                                      String description,
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCheckedAt) {
  }
}
