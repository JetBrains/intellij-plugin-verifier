package com.jetbrains.pluginverifier.location;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class PluginLocation extends ProblemLocation {

  private String myPluginId;

  public PluginLocation() {
  }

  PluginLocation(@NotNull String pluginId) {
    myPluginId = pluginId;
  }

  public String getPluginId() {
    return myPluginId;
  }

  public void setPluginId(String pluginId) {
    myPluginId = pluginId;
  }

  @Override
  public String toString() {
    return myPluginId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PluginLocation that = (PluginLocation) o;

    return myPluginId != null ? myPluginId.equals(that.myPluginId) : that.myPluginId == null;

  }

  @Override
  public int hashCode() {
    int result = 100;
    result = 31 * result + (myPluginId != null ? myPluginId.hashCode() : 0);
    return result;
  }
}
