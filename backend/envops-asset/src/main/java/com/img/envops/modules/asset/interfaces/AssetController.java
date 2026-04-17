package com.img.envops.modules.asset.interfaces;

import com.img.envops.common.response.R;
import com.img.envops.modules.asset.application.AssetApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

  public AssetController(AssetApplicationService assetApplicationService) {
    this.assetApplicationService = assetApplicationService;
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
}
