package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * In case of "check-ide" command is used and there is a specified list of plugins
 * to be checked it indicates that for some plugin there are no updates for this IDE at all.
 *
 * @author Sergey Patrikeev
 */
@XmlRootElement
public class NoCompatibleUpdatesProblem extends Problem {

  @NotNull private String myPlugin;
  @NotNull private String myIdeVersion;

  public NoCompatibleUpdatesProblem(@NotNull String plugin, @NotNull String ideVersion) {
    myPlugin = plugin;
    myIdeVersion = ideVersion;
  }

  @NotNull
  public String getPlugin() {
    return myPlugin;
  }

  public void setPlugin(@NotNull String plugin) {
    myPlugin = plugin;
  }

  public void setIdeVersion(@NotNull String ideVersion) {
    myIdeVersion = ideVersion;
  }

  @NotNull
  public String getIdeVersion() {
    return myIdeVersion;
  }

  @Override
  public String getDescription() {
    return "For " + myPlugin + " there are no updates compatible with " + myIdeVersion + " in the Plugin Repository";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    NoCompatibleUpdatesProblem that = (NoCompatibleUpdatesProblem) o;

    return myPlugin.equals(that.myPlugin) && myIdeVersion.equals(that.myIdeVersion);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myPlugin.hashCode();
    result = 31 * result + myIdeVersion.hashCode();
    return result;
  }
}
