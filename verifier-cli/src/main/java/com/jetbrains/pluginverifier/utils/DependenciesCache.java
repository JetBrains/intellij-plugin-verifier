package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.PluginCache;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.FatalError;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.resolvers.CombiningResolver;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DependenciesCache {
  private static final Set<IdeaPlugin> DEP_CALC_MARKER = new HashSet<IdeaPlugin>();
  private static DependenciesCache ourInstance = new DependenciesCache();
  private final WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>> map = new WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>>();

  private DependenciesCache() {
  }

  public static DependenciesCache getInstance() {
    return ourInstance;
  }

  private PluginDependenciesDescriptor getPluginDependenciesDescriptor(Idea ide, IdeaPlugin plugin) {
    WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor> m = map.get(ide);
    if (m == null) {
      m = new WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>();
      map.put(ide, m);
    }

    PluginDependenciesDescriptor descr = m.get(plugin);
    if (descr == null) {
      descr = new PluginDependenciesDescriptor(plugin.toString());
      m.put(plugin, descr);
    }

    return descr;
  }

  public Resolver getResolver(Idea ide, IdeaPlugin plugin) {
    PluginDependenciesDescriptor descr = getPluginDependenciesDescriptor(ide, plugin);
    if (descr.myResolver == null) {
      List<Resolver> resolvers = new ArrayList<Resolver>();

      resolvers.add(plugin.getCommonClassPool());
      resolvers.add(ide.getResolver());

      for (IdeaPlugin dep : getDependenciesWithTransitive(ide, plugin, new ArrayList<PluginDependenciesDescriptor>())) {
        ClassPool pluginClassPool = dep.getPluginClassPool();
        if (!pluginClassPool.isEmpty()) {
          resolvers.add(pluginClassPool);
        }

        ClassPool libraryClassPool = dep.getLibraryClassPool();
        if (!libraryClassPool.isEmpty()) {
          resolvers.add(libraryClassPool);
        }
      }

      descr.myResolver = CombiningResolver.union(resolvers);
    }

    return descr.myResolver;
  }

  public Set<IdeaPlugin> getDependenciesWithTransitive(Idea ide, IdeaPlugin plugin, List<PluginDependenciesDescriptor> pluginStack) {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);

    Set<IdeaPlugin> res = descriptor.dependenciesWithTransitive;
    if (res == DEP_CALC_MARKER) {
      if (Boolean.parseBoolean(Configuration.getInstance().getProperty("fail.on.cyclic.dependencies"))) {
        int idx = pluginStack.lastIndexOf(descriptor);
        throw new FatalError("Cyclic plugin dependencies: " + Joiner.on(" -> ").join(pluginStack.subList(idx, pluginStack.size())) + " -> " + plugin);
      }

      for (int i = pluginStack.size() - 1; i >= 0; i--) {
        pluginStack.get(i).isCyclic = true;
        if (pluginStack.get(i) == descriptor) break;
      }

      return Collections.emptySet();
    }

    if (descriptor.dependenciesWithTransitive == null) {
      descriptor.dependenciesWithTransitive = DEP_CALC_MARKER;
      pluginStack.add(descriptor);

      try {
        res = new HashSet<IdeaPlugin>();

        for (PluginDependency dependency : plugin.getModuleDependencies()) {
          IdeaPlugin depPlugin = ide.getPluginByModule(dependency.getId());
          if (depPlugin != null && res.add(depPlugin)) {
            res.addAll(getDependenciesWithTransitive(ide, depPlugin, pluginStack));
          }
        }
        for (PluginDependency pluginDependency : plugin.getDependencies()) {
          IdeaPlugin depPlugin = ide.getPlugin(pluginDependency.getId());
          if (depPlugin == null) {
            try {
              UpdateInfo updateInfo = RepositoryManager.getInstance().findPlugin(ide.getVersion(), pluginDependency.getId());
              if (updateInfo != null) {
                File pluginZip = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
                depPlugin = PluginCache.getInstance().getPlugin(pluginZip);
              }
            }
            catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (depPlugin != null && res.add(depPlugin)) {
            res.addAll(getDependenciesWithTransitive(ide, depPlugin, pluginStack));
          }
        }

        if (descriptor.isCyclic) {
          descriptor.dependenciesWithTransitive = null;
        }
        else {
          descriptor.dependenciesWithTransitive = res;
        }
      }
      finally {
        pluginStack.remove(pluginStack.size() - 1);

        if (descriptor.dependenciesWithTransitive == DEP_CALC_MARKER) {
          descriptor.dependenciesWithTransitive = null;
        }
      }
    }

    return res;
  }

  private static class PluginDependenciesDescriptor {
    public final String pluginName;

    public Resolver myResolver;

    public boolean isCyclic;

    public Set<IdeaPlugin> dependenciesWithTransitive;

    private PluginDependenciesDescriptor(String pluginName) {
      this.pluginName = pluginName;
    }

    @Override
    public String toString() {
      return pluginName;
    }
  }
}
