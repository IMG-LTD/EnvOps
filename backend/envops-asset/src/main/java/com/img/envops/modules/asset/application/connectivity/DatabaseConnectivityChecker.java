package com.img.envops.modules.asset.application.connectivity;

public interface DatabaseConnectivityChecker {
  String databaseType();

  DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target);
}
