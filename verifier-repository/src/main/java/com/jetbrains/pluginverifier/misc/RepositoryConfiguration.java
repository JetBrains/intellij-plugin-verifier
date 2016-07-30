package com.jetbrains.pluginverifier.misc;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class RepositoryConfiguration {

  private static RepositoryConfiguration INSTANCE;

  private final Properties myDefaultProperties;

  private RepositoryConfiguration() {
    Properties defaultConfig = new Properties();
    try {
      defaultConfig.load(RepositoryConfiguration.class.getResourceAsStream("/defaultConfig.properties"));
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read defaultConfig.properties", e);
    }

    myDefaultProperties = new Properties(defaultConfig);
  }

  @NotNull
  public static RepositoryConfiguration getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RepositoryConfiguration();
    }

    return INSTANCE;
  }

  @NotNull
  private File getVerifierHomeDir() {
    String verifierHomeDir = getProperty("plugin.verifier.home.dir");
    if (verifierHomeDir != null) {
      return new File(verifierHomeDir);
    }
    String userHome = getProperty("user.home");
    if (userHome != null) {
      return new File(userHome, ".pluginVerifier");
    }
    return new File(FileUtils.getTempDirectory(), ".pluginVerifier");
  }

  @Nullable
  private String getProperty(String propertyName) {
    String systemProperty = System.getProperty(propertyName);
    if (systemProperty != null) return systemProperty;

    return myDefaultProperties.getProperty(propertyName);
  }

  @NotNull
  public String getPluginRepositoryUrl() {
    String res = getProperty("plugin.repository.url");
    if (res == null) {
      throw new RuntimeException("Plugin repository URL is not specified");
    }

    if (res.endsWith("/")) {
      res = res.substring(0, res.length() - 1);
    }
    return res;
  }

  @NotNull
  File getPluginCacheDir() {
    return new File(getVerifierHomeDir(), "cache");
  }

  @Nullable
  public String getCustomRepositories() {
    return getProperty("plugin.custom.repositories");
  }
}
