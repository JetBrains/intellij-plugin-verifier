package com.jetbrains.pluginverifier.format;

import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.misc.VersionComparatorUtil;
import kotlin.jvm.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;

/**
 * The plugin descriptor.
 * <p>
 * It's used either to describe the plugin build in the Plugin repository or
 * the plugin located locally (but not necessarily published to the Repository).
 * In the former case the {@link #updateId} is specified (it's the unique id of the build in the database).
 * In the latter case {@link #pluginId}, {@link #pluginName} and {@link #version} are specified.
 *
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "update")
public final class UpdateInfo {

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

  @SerializedName("updateId") private Integer updateId;
  @SerializedName("pluginId") private String pluginId;
  @SerializedName("pluginName") private String pluginName;
  @SerializedName("version") private String version;
  @Transient private Long cdate;

  public UpdateInfo() {
  }

  public UpdateInfo(Integer updateId, String pluginId, String pluginName, String version) {
    this.updateId = updateId;
    this.pluginId = pluginId;
    this.pluginName = pluginName;
    this.version = version;
  }

  public UpdateInfo(int updateId) {
    this.updateId = updateId;
  }

  public UpdateInfo(@NotNull String pluginId, @NotNull String pluginName, @NotNull String version) {
    this.pluginId = pluginId;
    this.pluginName = pluginName;
    this.version = version;
  }

  @NotNull
  public static UpdateInfo copy(@NotNull UpdateInfo instance) {
    UpdateInfo info = new UpdateInfo();
    info.updateId = instance.updateId;
    info.pluginId = instance.pluginId;
    info.pluginName = instance.pluginName;
    info.version = instance.version;
    info.cdate = instance.cdate;
    return info;
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
