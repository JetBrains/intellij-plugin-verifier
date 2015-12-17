package com.jetbrains.pluginverifier.problems;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UnresolvedDependencyProblem extends Problem {

  private String myRequiredPlugin;
  private boolean myIsOptional;

  public UnresolvedDependencyProblem() {

  }

  public UnresolvedDependencyProblem(String requiredPlugin, boolean isOptional) {
    myRequiredPlugin = requiredPlugin;
    myIsOptional = isOptional;
  }

  public boolean isOptional() {
    return myIsOptional;
  }

  public void setOptional(boolean optional) {
    myIsOptional = optional;
  }

  public String getRequiredPlugin() {
    return myRequiredPlugin;
  }

  public void setRequiredPlugin(String requiredPlugin) {
    myRequiredPlugin = requiredPlugin;
  }

  @Override
  public String getDescriptionPrefix() {
    return "unresolved plugin dependency";
  }

  public String getDescription() {
    return getDescriptionPrefix() + " on " + myRequiredPlugin + " " + (myIsOptional ? "(optional)" : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UnresolvedDependencyProblem)) return false;

    UnresolvedDependencyProblem problem = (UnresolvedDependencyProblem) o;

    return !(myRequiredPlugin != null ? !myRequiredPlugin.equals(problem.myRequiredPlugin) : problem.myRequiredPlugin != null);
  }

  @Override
  public int hashCode() {
    return myRequiredPlugin != null ? myRequiredPlugin.hashCode() : 0;
  }
}
