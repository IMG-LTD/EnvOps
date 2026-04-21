package com.img.envops.modules.asset.application.connectivity;

public record DatabaseConnectivityTarget(Long databaseId,
                                         String databaseName,
                                         String databaseType,
                                         String environment,
                                         String hostIpAddress,
                                         Integer port,
                                         String instanceName,
                                         String connectionUsername,
                                         String connectionPassword) {
}
