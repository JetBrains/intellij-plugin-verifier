package com.jetbrains.pluginverifier.dependencies;

/**
 * @author Sergey Patrikeev
 */
public class MissingDependenciesError extends DependenciesError {

  private final String myPlugin;
  private final String myMissedPlugin;
  private final String myDescription;

  public MissingDependenciesError(String plugin, String missedPlugin, String description) {
    myPlugin = plugin;
    myMissedPlugin = missedPlugin;
    myDescription = description;
  }

  public MissingDependenciesError(String plugin, String missedPlugin, String description, Throwable cause) {
    super(cause);
    myPlugin = plugin;
    myMissedPlugin = missedPlugin;
    myDescription = description;
  }

  public String getPlugin() {
    return myPlugin;
  }

  public String getMissedPlugin() {
    return myMissedPlugin;
  }

  public String getDescription() {
    return myDescription;
  }
}
