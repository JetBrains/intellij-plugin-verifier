package com.jetbrains.pluginverifier.misc;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.PluginCache;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.pool.ClassPool;
import com.intellij.structure.resolvers.CombiningResolver;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DependenciesCache {
  private static final Set<IdeaPlugin> DEP_CALC_MARKER = new HashSet<IdeaPlugin>();
  private static final ImmutableList<String> IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate"
  );
  private static DependenciesCache ourInstance = new DependenciesCache();
  private final WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>> map = new WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>>();

  private DependenciesCache() {
  }

  @NotNull
  public static DependenciesCache getInstance() {
    return ourInstance;
  }

  @NotNull
  private PluginDependenciesDescriptor getPluginDependenciesDescriptor(@NotNull Idea ide, @NotNull IdeaPlugin plugin) {
    WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor> m = map.get(ide);
    if (m == null) {
      m = new WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>();
      map.put(ide, m);
    }

    PluginDependenciesDescriptor descr = m.get(plugin);
    if (descr == null) {
      String pluginIdentifier = plugin.getPluginId().isEmpty() ? plugin.getPluginName() : plugin.getPluginId();
      descr = new PluginDependenciesDescriptor(pluginIdentifier);
      m.put(plugin, descr);
    }

    return descr;
  }

  @NotNull
  public Resolver getResolver(@NotNull Idea ide, @NotNull IdeaPlugin plugin) throws VerificationError {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);
    if (descriptor.myResolver == null) {
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

      descriptor.myResolver = CombiningResolver.union(resolvers);
    }

    return descriptor.myResolver;
  }


  @NotNull
  private Set<IdeaPlugin> getDependenciesWithTransitive(@NotNull Idea ide,
                                                        @NotNull IdeaPlugin plugin,
                                                        @NotNull List<PluginDependenciesDescriptor> pluginStack) throws VerificationError {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);

    Set<IdeaPlugin> result = descriptor.dependenciesWithTransitive;
    if (result == DEP_CALC_MARKER) {
      if (Boolean.parseBoolean(RepositoryConfiguration.getInstance().getProperty("fail.on.cyclic.dependencies"))) {
        int idx = pluginStack.lastIndexOf(descriptor); //compare descriptors by their identity (default equals behavior)
        throw new VerificationError("Cyclic plugin dependencies: " + Joiner.on(" -> ").join(pluginStack.subList(idx, pluginStack.size())) + " -> " + plugin.getPluginId());
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
        result = new HashSet<IdeaPlugin>(); //compare IdeaPlugins by their identity (default equals and hashCode behavior)

        //iterate through IntelliJ module dependencies
        for (PluginDependency dependency : plugin.getModuleDependencies()) {

          final String moduleId = dependency.getId();
          if (IDEA_ULTIMATE_MODULES.contains(moduleId)) {
            continue;
          }

          IdeaPlugin depPlugin = ide.getPluginByModule(moduleId);

          //noinspection Duplicates
          if (depPlugin == null) {
            final String message = "Plugin " + plugin.getPluginId() + " depends on module " + moduleId + " which is not found in " + ide.getVersion();
            if (!dependency.isOptional()) {
              //this is required plugin
              throw new VerificationError(message);
            } else {
              System.err.println("(optional dependency)" + message);
            }
          } else if (result.add(depPlugin)) {
            result.addAll(getDependenciesWithTransitive(ide, depPlugin, pluginStack));
          }
        }

        //iterate through the other plugin dependencies
        for (PluginDependency pluginDependency : plugin.getDependencies()) {
          IdeaPlugin depPlugin = ide.getPlugin(pluginDependency.getId());

          Exception maybeException = null;
          if (depPlugin == null) {
            try {
              UpdateInfo updateInfo = RepositoryManager.getInstance().findPlugin(ide.getVersion(), pluginDependency.getId());
              if (updateInfo != null) {
                File pluginZip = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
                depPlugin = PluginCache.getInstance().getPlugin(pluginZip);
              }
            }
            catch (IOException e) {
              maybeException = e;
              e.printStackTrace();
              System.err.println("Couldn't load plugin dependency for " + plugin.getPluginId());
            }
          }

          //noinspection Duplicates
          if (depPlugin == null) {
            final String message = "Plugin " + plugin.getPluginId() + " depends on the other plugin " + pluginDependency.getId() + " which is not found for " + ide.getVersion();
            if (!pluginDependency.isOptional()) {
              //this is required plugin
              throw new VerificationError(message, maybeException);
            } else {
              System.err.println("(optional dependency)" + message);
              if (maybeException != null) {
                maybeException.printStackTrace();
              }
            }
          } else if (result.add(depPlugin)) {
            result.addAll(getDependenciesWithTransitive(ide, depPlugin, pluginStack));
          }
        }

        if (descriptor.isCyclic) {
          descriptor.dependenciesWithTransitive = null;
        }
        else {
          descriptor.dependenciesWithTransitive = result;
        }
      }
      finally {
        pluginStack.remove(pluginStack.size() - 1);

        if (descriptor.dependenciesWithTransitive == DEP_CALC_MARKER) {
          descriptor.dependenciesWithTransitive = null;
        }
      }
    }

    return result;
  }

  private static class PluginDependenciesDescriptor {
    public final String pluginName;

    public Resolver myResolver;

    public boolean isCyclic;

    public Set<IdeaPlugin> dependenciesWithTransitive;

    private PluginDependenciesDescriptor(@NotNull String pluginName) {
      this.pluginName = pluginName;
    }

    @Override
    public String toString() {
      return pluginName;
    }
  }
}
