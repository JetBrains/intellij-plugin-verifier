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

  public PluginLocation(@NotNull String pluginId) {
    myPluginId = pluginId;
  }

  public String getPluginId() {
    return myPluginId;
  }

  public void setPluginId(String pluginId) {
    myPluginId = pluginId;
  }

  @Override
  public String asString() {
    return myPluginId;
  }

}
