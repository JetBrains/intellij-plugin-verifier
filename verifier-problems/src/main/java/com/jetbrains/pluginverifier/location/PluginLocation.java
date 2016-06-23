package com.jetbrains.pluginverifier.location;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

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

  @NotNull
  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(new Pair<String, String>("plugin", myPluginId));
  }

  @NotNull
  @Override
  public ProblemLocation deserialize(@NotNull String... params) {
    return new PluginLocation(params[0]);
  }
}
