package com.jetbrains.pluginverifier.misc;

import com.jetbrains.pluginverifier.utils.FailUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Properties;

/**
 * This class contains convenient methods for
 */
public class RepositoryConfiguration {

  private static RepositoryConfiguration INSTANCE;

  private final Properties myProperties;

  private RepositoryConfiguration() {
    Properties defaultConfig = new Properties();
    try {
      defaultConfig.load(RepositoryConfiguration.class.getResourceAsStream("/defaultConfig.properties"));
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to read defaultConfig.properties", e);
    }

    myProperties = new Properties(defaultConfig);

    File cfg = new File(getValidatorHome(), "config.properties");
    if (cfg.exists()) {
      try {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(cfg));

        try {
          myProperties.load(inputStream);
        }
        finally {
          IOUtils.closeQuietly(inputStream);
        }
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to read config file: " + cfg, e);
      }
    }
  }

  @NotNull
  public static RepositoryConfiguration getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RepositoryConfiguration();
    }

    return INSTANCE;
  }

  @NotNull
  private File getValidatorHome() {
    String homeDir = getProperty("home.directory.name");
    if (homeDir == null) {
      throw FailUtil.fail("Repository home directory is not specified");
    }
    return new File(getProperty("user.home"), homeDir);
  }

  @Nullable
  public String getProperty(String propertyName) {
    String systemProperty = System.getProperty(propertyName);
    if (systemProperty != null) return systemProperty;

    return myProperties.getProperty(propertyName);
  }

  @NotNull
  public String getPluginRepositoryUrl() {
    String res = getProperty("plugin.repository.url");
    if (res == null) {
      throw FailUtil.fail("Plugin repository URL is not specified");
    }

    if (res.endsWith("/")) {
      res = res.substring(0, res.length() - 1);
    }
    return res;
  }

  @NotNull
  public File getPluginCacheDir() {
    String pluginCacheDir = getProperty("plugin.cache.dir");
    if (pluginCacheDir != null) {
      return new File(pluginCacheDir);
    }

    return new File(getValidatorHome(), "cache");
  }

  @Nullable
  public String getCustomRepositories() {
    return getProperty("plugin.custom.repositories");
  }
}
