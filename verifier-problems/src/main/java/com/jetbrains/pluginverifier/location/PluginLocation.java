package com.jetbrains.pluginverifier.location;

import com.jetbrains.pluginverifier.utils.Pair;
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

  @Override
  public List<Pair<String, String>> serialize() {
    return Collections.singletonList(Pair.create("plugin", myPluginId));
  }

  @Override
  public ProblemLocation deserialize(String... params) {
    return new PluginLocation(params[0]);
  }
}
