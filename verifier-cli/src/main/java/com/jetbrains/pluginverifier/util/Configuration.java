package com.jetbrains.pluginverifier.util;

import org.jetbrains.annotations.NotNull;

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
        throw new RuntimeException("Failet to read config file: " + cfg, e);
      }
    }
  }

  public static Configuration getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new Configuration();
    }

    return INSTANCE;
  }

  @NotNull
  public String getPluginRepositoryUrl() {
    return myProperties.getProperty("plugin.repository.url");
  }

}
