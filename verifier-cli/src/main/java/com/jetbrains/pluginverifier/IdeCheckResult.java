package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement
public class IdeCheckResult {

  private String myIdeVersion;

  private List<PluginDescriptor> plugins;

  public String getIdeVersion() {
    return myIdeVersion;
  }

  public void setIdeVersion(String ideVersion) {
    myIdeVersion = ideVersion;
  }

  public List<PluginDescriptor> getPlugins() {
    return plugins;
  }

  public void setPlugins(List<PluginDescriptor> plugins) {
    this.plugins = plugins;
  }

  public static class PluginDescriptor {
    private String myPluginName;
    private String myBuildId;

    private List<Problem> myProblems = new ArrayList<Problem>();

    public String getPluginName() {
      return myPluginName;
    }

    public void setPluginName(String pluginName) {
      myPluginName = pluginName;
    }

    public String getBuildId() {
      return myBuildId;
    }

    public void setBuildId(String buildId) {
      myBuildId = buildId;
    }

    public List<Problem> getProblems() {
      return myProblems;
    }

    public void setProblems(List<Problem> problems) {
      myProblems = problems;
    }
  }
}
