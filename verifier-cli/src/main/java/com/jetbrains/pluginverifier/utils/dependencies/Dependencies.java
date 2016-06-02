package com.jetbrains.pluginverifier.utils.dependencies;

import com.google.common.base.Preconditions;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.PluginCache;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class Dependencies {

  //TODO: add some caching

  private static final Dependencies INSTANCE = new Dependencies();

  private Dependencies() {

  }

  public static Dependencies getInstance() {
    return INSTANCE;
  }

  @NotNull
  public DependenciesResult calcDependencies(@NotNull Plugin plugin, @NotNull Ide ide) {
    Dfs dfs = new Dfs(ide);
    PluginDependenciesNode result = dfs.dfs(plugin);
    Preconditions.checkNotNull(result);
    return new DependenciesResult(result, dfs.cycle);
  }

  private static class Dfs {

    /**
     * Already calculated plugin nodes.
     */
    final Map<Plugin, PluginDependenciesNode> nodes = new HashMap<>();

    /**
     * Current DFS state.
     */
    final Map<Plugin, DfsState> state = new HashMap<>();

    /**
     * DFS path.
     */
    final List<Plugin> path = new ArrayList<>();

    /**
     * IDE against which to resolve dependencies.
     */
    final Ide ide;

    /**
     * This is some cycle in the dependencies graph, or null if no cycle
     */
    @Nullable
    List<Plugin> cycle;

    Dfs(@NotNull Ide ide) {
      this.ide = ide;
    }

    @NotNull
    PluginDependenciesNode dfs(@NotNull Plugin plugin) {
      if (nodes.containsKey(plugin)) {
        //already calculated.
        Preconditions.checkArgument(state.containsKey(plugin) && state.get(plugin) == DfsState.BLACK);
        return nodes.get(plugin);
      }

      //assure plugin is not in-progress.
      Preconditions.checkArgument(!state.containsKey(plugin));

      //mark as in-progress
      state.put(plugin, DfsState.GRAY);

      path.add(plugin);

      //current node results
      Set<Plugin> transitives = new HashSet<>();
      Map<PluginDependency, MissingReason> missing = new HashMap<>();
      Set<PluginDependenciesNode> edges = new HashSet<>();

      try {
        //process plugin dependencies.

        List<PluginDependency> union = new ArrayList<>(plugin.getModuleDependencies());
        union.addAll(plugin.getDependencies());

        for (PluginDependency pd : union) {
          boolean isModule = plugin.getModuleDependencies().indexOf(pd) != -1;
          String depId = pd.getId();
          Plugin dependency;
          if (isModule) {
            if (Util.isDefaultModule(depId)) {
              continue;
            }
            dependency = ide.getPluginByModule(depId);
            if (dependency == null) {
              String reason = String.format("Plugin %s depends on module %s which is not found in %s", plugin.getPluginId(), depId, ide.getVersion());
              missing.put(pd, new MissingReason(reason, null));
              continue;
            }

          } else {
            dependency = ide.getPluginById(depId);

            if (dependency == null) {
              //try to load plugin
              UpdateInfo updateInfo;
              try {
                updateInfo = RepositoryManager.getInstance().getLastCompatibleUpdateOfPlugin(ide.getVersion(), depId);
              } catch (Exception e) {
                String message = String.format("Couldn't get dependency plugin '%s' from the Plugin Repository for IDE %s", depId, ide.getVersion());
                System.err.println(message);
                e.printStackTrace();
                missing.put(pd, new MissingReason(message, e));
                continue;
              }

              if (updateInfo != null) {
                //update does really exist in the repo
                File pluginZip;
                try {
                  pluginZip = RepositoryManager.getInstance().getPluginFile(updateInfo);
                } catch (Exception e) {
                  String message = String.format("Couldn't download dependency plugin '%s' from the Plugin Repository for IDE %s", depId, ide.getVersion());
                  System.err.println(message);
                  e.printStackTrace();
                  missing.put(pd, new MissingReason(message, e));
                  continue;
                }

                try {
                  dependency = PluginCache.getInstance().createPlugin(pluginZip);
                } catch (Exception e) {
                  final String message = String.format("Plugin %s depends on the other plugin %s which has some problems%s", plugin, depId, e.getMessage() != null ? e.getMessage() : "");
                  System.err.println(message);
                  e.printStackTrace();
                  missing.put(pd, new MissingReason(message, e));
                  continue;
                }
              }
            }

            if (dependency == null) {
              final String message = String.format("Plugin %s depends on the other plugin %s which has not a compatible build with %s", plugin, depId, ide.getVersion());
              System.err.println(message);
              missing.put(pd, new MissingReason(message, null));
              continue;
            }
          }
          //the dependency is found and is OK.


          //check if cycle
          if (state.containsKey(dependency) && state.get(dependency) == DfsState.GRAY) {
            int idx = path.lastIndexOf(dependency);
            Preconditions.checkArgument(idx != -1);
            cycle = new ArrayList<>(path.subList(idx, path.size()));
            cycle.add(dependency); //first and last entries are the same (A -> B -> C -> A)

            //TODO: we can't append edges and transitives at this point, because the dependency is currently calculating.
            continue;
          }

          PluginDependenciesNode to = dfs(dependency);
          edges.add(to);
          transitives.add(to.getPlugin()); //the dependency itself
          transitives.addAll(to.getTransitiveDependencies()); //and all its transitive dependencies
        }

        PluginDependenciesNode result = new PluginDependenciesNode(plugin, edges, transitives, missing);

        //remember the result.
        nodes.put(plugin, result);

        return result;

      } finally {
        //plugin is visited
        state.put(plugin, DfsState.BLACK);

        int lastIdx = path.size() - 1;
        Preconditions.checkArgument(path.size() > 0 && path.get(lastIdx) == plugin);
        path.remove(lastIdx);
      }
    }

    private enum DfsState {
      GRAY, //in progress
      BLACK //already visited
    }

  }

  public static class DependenciesResult {
    @NotNull
    private final PluginDependenciesNode myDescriptor;
    /**
     * Not-null value represents some cycle in the dependencies graph.
     * It's for the caller consideration whether to throw an exception in a such case.
     */
    @Nullable
    private final List<Plugin> myCycle;

    public DependenciesResult(@NotNull PluginDependenciesNode descriptor, @Nullable List<Plugin> cycle) {
      myDescriptor = descriptor;
      myCycle = cycle;
    }

    @NotNull
    public PluginDependenciesNode getDescriptor() {
      return myDescriptor;
    }

    @Nullable
    public List<Plugin> getCycle() {
      return myCycle;
    }
  }


}
