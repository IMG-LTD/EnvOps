package com.img.envops.modules.asset.application.connectivity;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class MongoDbConnectivityChecker implements DatabaseConnectivityChecker {
  @Override
  public String databaseType() {
    return "mongodb";
  }

  @Override
  public DatabaseConnectivityProbeResult check(DatabaseConnectivityTarget target) {
    String connectionString = "mongodb://%s:%s@%s:%d/%s?serverSelectionTimeoutMS=5000"
        .formatted(
            URLEncoder.encode(target.connectionUsername(), StandardCharsets.UTF_8),
            URLEncoder.encode(target.connectionPassword(), StandardCharsets.UTF_8),
            target.hostIpAddress(),
            target.port(),
            target.databaseName());

    MongoClientSettings settings = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(connectionString))
        .applyToSocketSettings(builder -> builder
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS))
        .build();

    try (MongoClient client = MongoClients.create(settings)) {
      client.getDatabase(target.databaseName()).runCommand(new Document("ping", 1));
      return DatabaseConnectivityProbeResult.success("connected");
    } catch (MongoException exception) {
      return DatabaseConnectivityProbeResult.failure("认证失败");
    }
  }
}
