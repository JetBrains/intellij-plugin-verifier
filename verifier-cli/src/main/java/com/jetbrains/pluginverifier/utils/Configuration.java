package com.jetbrains.pluginverifier.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Properties;

public class Configuration {

  private static Configuration INSTANCE;

  private final Properties myProperties;

  public Configuration() {
    File cfg = new File(Util.getValidatorHome(), "config.properties");

    Properties defaultConfig = new Properties();
    try {
      defaultConfig.load(Configuration.class.getResourceAsStream("/defaultConfig.properties"));
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read defaultConfig.properties", e);
    }

    myProperties = new Properties(defaultConfig);

    if (cfg.exists()) {
      try {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(cfg));

        try {
          myProperties.load(inputStream);
        }
        finally {
          inputStream.close();
        }
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to read config file: " + cfg, e);
      }
    }
  }

  public static Configuration getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Configuration();
    }

    return INSTANCE;
  }

  public String getProperty(String propertyName) {
    String systemProperty = System.getProperty(propertyName);
    if (systemProperty != null) return systemProperty;

    return myProperties.getProperty(propertyName);
  }

  @NotNull
  public String getPluginRepositoryUrl() {
    String res = getProperty("plugin.repository.url");
    if (res.endsWith("/")) {
      res = res.substring(0, res.length() - 1);
    }
    return res;
  }

  @Nullable
  public String getPluginCacheDir() {
    return getProperty("plugin.cache.dir");
  }

  @Nullable
  public String getCustomRepositories() {
    return getProperty("plugin.custom.repositories");
  }
}
