package com.jetbrains.pluginverifier.util;

/**
 * @author Sergey Evdokimov
 */
public class UpdateJson {

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

  public boolean equalsByIdOrVersion(UpdateJson another) {
    if (updateId != null && another.updateId != null) {
      return updateId.equals(another.updateId);
    }

    if (pluginId != null && another.pluginId != null) {
      if (pluginId.equals(another.pluginId) && version != null && version.equals(another.version)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String toString() {
    if (StringUtil.isNotEmpty(pluginId)) {
      return pluginId
             + (StringUtil.isEmpty(version) ? "" : ':' + version)
             + (updateId == null ? "" : " (#" + updateId + ')');
    }

    return "#" + updateId;
  }
}
