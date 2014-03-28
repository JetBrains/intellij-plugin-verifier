package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "update")
public class UpdateInfo {

  private Integer updateId;

  private String pluginId;

  private String pluginName;

  private String version;

  private Long cdate;

  @XmlAttribute
  public Integer getUpdateId() {
    return updateId;
  }

  public void setUpdateId(Integer updateId) {
    this.updateId = updateId;
  }

  @XmlAttribute
  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  @XmlAttribute
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @XmlAttribute
  public String getPluginName() {
    return pluginName;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  @XmlAttribute
  public Long getCdate() {
    return cdate;
  }

  public void setCdate(Long cdate) {
    this.cdate = cdate;
  }

  public String getDisplayName() {
    return pluginName == null || pluginName.isEmpty() ? pluginId : pluginName;
  }

  public boolean validate() {
    return updateId != null || (pluginId != null && !pluginId.isEmpty() && version != null && !version.isEmpty());
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
