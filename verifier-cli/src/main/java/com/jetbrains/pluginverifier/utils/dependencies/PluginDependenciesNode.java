package com.jetbrains.pluginverifier.utils.dependencies;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Sergey Patrikeev
 */
public final class PluginDependenciesNode {
  @NotNull
  private final Plugin myPlugin;

  /**
   * All plugins reachable from myPlugin.
   */
  @NotNull
  private final Set<Plugin> myTransitiveDependencies;

  /**
   * Missing dependency -> reason why it is missing.
   */
  @NotNull
  private final Map<PluginDependency, MissingReason> myMissingDependencies;

  /**
   * The set of existing {@link PluginDependenciesNode}s reachable from {@code this} node.
   */
  @NotNull
  private final Set<PluginDependenciesNode> myEdges;

  PluginDependenciesNode(@NotNull Plugin plugin,
                         @NotNull Set<PluginDependenciesNode> edges,
                         @NotNull Set<Plugin> transitiveDependencies,
                         @NotNull Map<PluginDependency, MissingReason> missingDependencies) {
    myPlugin = plugin;
    myTransitiveDependencies = new HashSet<>(transitiveDependencies);
    myMissingDependencies = new HashMap<>(missingDependencies);
    myEdges = new HashSet<>(edges);
  }

  @NotNull
  public Set<PluginDependenciesNode> getEdges() {
    return Collections.unmodifiableSet(myEdges);
  }

  @NotNull
  public Set<Plugin> getTransitiveDependencies() {
    return Collections.unmodifiableSet(myTransitiveDependencies);
  }

  @NotNull
  public Map<PluginDependency, MissingReason> getMissingDependencies() {
    return Collections.unmodifiableMap(myMissingDependencies);
  }

  @NotNull
  public Plugin getPlugin() {
    return myPlugin;
  }

  @Override
  public String toString() {
    return myPlugin.getPluginId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PluginDependenciesNode that = (PluginDependenciesNode) o;

    return myPlugin.getPluginFile().equals(that.myPlugin.getPluginFile());
  }

  @Override
  public int hashCode() {
    return myPlugin.getPluginFile().hashCode();
  }
}
