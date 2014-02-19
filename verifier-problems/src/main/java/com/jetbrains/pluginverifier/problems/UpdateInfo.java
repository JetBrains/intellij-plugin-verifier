package com.jetbrains.pluginverifier.problems;

/**
 * @author Sergey Evdokimov
 */
public class UpdateInfo {

  private Integer updateId;

  private String pluginId;

  private String pluginName;

  private String version;

  private Long cdate;

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

  public String getPluginName() {
    return pluginName;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  public Long getCdate() {
    return cdate;
  }

  public void setCdate(Long cdate) {
    this.cdate = cdate;
  }

  public String getDisplayName() {
    return pluginName == null || pluginName.isEmpty() ? pluginId : pluginName;
  }

  @Override
  public String toString() {
    if (pluginId != null && !pluginId.isEmpty()) {
      if (version == null || version.isEmpty()) {
        return pluginId + (updateId == null ? "" : "#" + updateId);
      }
      else {
        return pluginId + ':' + version;
      }
    }

    return "#" + updateId;
  }
}
