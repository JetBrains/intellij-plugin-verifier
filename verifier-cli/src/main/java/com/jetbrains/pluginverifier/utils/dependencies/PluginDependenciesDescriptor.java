package com.jetbrains.pluginverifier.utils.dependencies;

import com.intellij.structure.domain.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public class PluginDependenciesDescriptor {
  private final String myPluginName;

  boolean myIsCyclic;

  Set<Plugin> myDependencies;

  boolean myInitialized;

  //TODO: add mandatory missing plugins as map (not exception)
  /**
   * pluginId -> (missingPluginId -> description)
   */
  private Map<String, Map<String, String>> myMissingOptionalDependencies = new HashMap<String, Map<String, String>>();

  PluginDependenciesDescriptor(@NotNull String pluginName, boolean isCyclic) {
    this.myPluginName = pluginName;
    if (isCyclic) {
      myIsCyclic = true;
      myDependencies = Collections.emptySet();
    }
  }

  public Set<Plugin> getDependencies() {
    return myDependencies;
  }


  public Map<String, Map<String, String>> getMissingOptionalDependencies() {
    return myMissingOptionalDependencies;
  }

  public String getPluginName() {
    return myPluginName;
  }

  void addMissingDependency(@NotNull String pluginId, @NotNull String missingId, @NotNull String message) {
    Map<String, String> map = myMissingOptionalDependencies.get(pluginId);
    if (map == null) {
      map = new HashMap<String, String>();
      myMissingOptionalDependencies.put(pluginId, map);
    }
    map.put(missingId, message);
  }

  void combineOptionalMissing(PluginDependenciesDescriptor other) {
    Map<String, Map<String, String>> map = other.getMissingOptionalDependencies();
    if (map != null) {
      for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
        for (Map.Entry<String, String> missEntry : entry.getValue().entrySet()) {
          addMissingDependency(entry.getKey(), missEntry.getKey(), missEntry.getValue());
        }
      }
    }
  }

  @Override
  public String toString() {
    return myPluginName;
  }

  @Override
  final public int hashCode() {
    return super.hashCode();
  }

  @Override
  final public boolean equals(Object obj) {
    //we want to check for equality with ==
    return super.equals(obj);
  }
}
