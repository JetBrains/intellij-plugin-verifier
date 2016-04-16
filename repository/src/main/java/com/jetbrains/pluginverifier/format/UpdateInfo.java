package com.jetbrains.pluginverifier.format;

import com.jetbrains.pluginverifier.utils.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "update")
public class UpdateInfo {

  public static final Comparator<UpdateInfo> UPDATE_NUMBER_COMPARATOR = new Comparator<UpdateInfo>() {
    @Override
    public int compare(@NotNull UpdateInfo o1, @NotNull UpdateInfo o2) {
      Integer u1 = o1.getUpdateId();
      Integer u2 = o2.getUpdateId();
      if (u1 != null && u2 != null) {
        return u1 - u2;
      }
      if (u1 != null) {
        return -1;
      }
      if (u2 != null) {
        return 1;
      }
      return VersionComparatorUtil.compare(o1.getVersion(), o2.getVersion());
    }
  };

  private Integer updateId;
  private String pluginId;
  private String pluginName;
  private String version;
  private Long cdate;

  public UpdateInfo() {
  }

  public UpdateInfo(int updateId) {
    this.updateId = updateId;
  }

  public UpdateInfo(@NotNull String pluginId, @NotNull String pluginName, @NotNull String version) {
    this.pluginId = pluginId;
    this.pluginName = pluginName;
    this.version = version;
  }

  @XmlAttribute
  @Nullable
  public Integer getUpdateId() {
    return updateId;
  }

  public void setUpdateId(Integer updateId) {
    this.updateId = updateId;
  }

  @XmlAttribute
  @Nullable
  public String getPluginId() {
    return pluginId;
  }

  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  @XmlAttribute
  @Nullable
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @XmlAttribute
  @Nullable
  public String getPluginName() {
    return pluginName;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  @XmlAttribute
  @Nullable
  public Long getCdate() {
    return cdate;
  }

  public void setCdate(Long cdate) {
    this.cdate = cdate;
  }

  @Override
  public String toString() {
    if (pluginId != null && !pluginId.isEmpty()) {
      if (version == null || version.isEmpty()) {
        return pluginId + (updateId == null ? "" : "#" + updateId);
      } else {
        return pluginId + ':' + version;
      }
    }
    if (updateId == null) {
      return null;
    }
    return "#" + updateId;
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UpdateInfo that = (UpdateInfo) o;

    if (updateId != null ? !updateId.equals(that.updateId) : that.updateId != null) return false;
    if (pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null) return false;
    if (pluginName != null ? !pluginName.equals(that.pluginName) : that.pluginName != null) return false;
    if (version != null ? !version.equals(that.version) : that.version != null) return false;
    return !(cdate != null ? !cdate.equals(that.cdate) : that.cdate != null);

  }

  @Override
  public int hashCode() {
    int result = updateId != null ? updateId.hashCode() : 0;
    result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
    result = 31 * result + (pluginName != null ? pluginName.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    result = 31 * result + (cdate != null ? cdate.hashCode() : 0);
    return result;
  }
}
