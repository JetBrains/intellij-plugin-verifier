package com.jetbrains.pluginverifier.utils;

/**
 * @author Sergey Evdokimov
 */
public class Update {

  private Integer updateId;

  private String pluginId;

  private String version;

  public Integer getUpdateId() {
    return updateId;
  }

  public void setUpdateId(Integer updateId) {
    this.updateId = updateId;
  }

  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    if (StringUtil.isNotEmpty(pluginId)) {
      if (StringUtil.isEmpty(version)) {
        return pluginId + (updateId == null ? "" : "#" + updateId);
      }
      else {
        return pluginId + ':' + version;
      }
    }

    return "#" + updateId;
  }
}
