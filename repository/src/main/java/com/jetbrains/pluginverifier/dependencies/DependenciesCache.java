package com.jetbrains.pluginverifier.dependencies;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.PluginCache;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.FailUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DependenciesCache {
  private static final Set<Plugin> DEP_CALC_MARKER = Collections.emptySet();

  private static final ImmutableList<String> IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate",
      "com.intellij.modules.all"
  );
  private static DependenciesCache ourInstance = new DependenciesCache();

  /**
   * Dependencies of a plugin with respect to IDE
   */
  private final WeakHashMap<Ide, WeakHashMap<Plugin, PluginDependenciesDescriptor>> map = new WeakHashMap<Ide, WeakHashMap<Plugin, PluginDependenciesDescriptor>>();

  private DependenciesCache() {
  }

  @NotNull
  public static DependenciesCache getInstance() {
    return ourInstance;
  }

  @NotNull
  private PluginDependenciesDescriptor createDependenciesDescriptor(@NotNull Ide ide, @NotNull Plugin plugin) {
    WeakHashMap<Plugin, PluginDependenciesDescriptor> m = map.get(ide);
    if (m == null) {
      m = new WeakHashMap<Plugin, PluginDependenciesDescriptor>();
      map.put(ide, m);
    }

    PluginDependenciesDescriptor descr = m.get(plugin);
    if (descr == null) {
      String id = plugin.getPluginId();
      if (StringUtil.isEmpty(id)) {
        id = plugin.getPluginName();
      }
      if (StringUtil.isEmpty(id)) {
        id = plugin.toString();
      }
      descr = new PluginDependenciesDescriptor(id, false);
      m.put(plugin, descr);
    }

    return descr;
  }

  @NotNull
  public PluginDependenciesDescriptor getDependenciesDescriptor(@NotNull Plugin plugin, @NotNull Ide ide, @NotNull Jdk jdk, @Nullable Resolver externalClassPath) throws DependenciesError {
    PluginDependenciesDescriptor descriptor = createDependenciesDescriptor(ide, plugin);
    if (!descriptor.myInitialized) {
      //not calculated yet

      final PluginDependenciesDescriptor all = calcDependenciesWithTransitive(ide, plugin, new ArrayList<PluginDependenciesDescriptor>());

      List<Resolver> resolvers = createPluginWithAllResolvers(plugin, ide, jdk, externalClassPath, all);

      String name = "Common resolver for plugin " + plugin.getPluginId() + " with transitive dependencies; ide " + ide.getVersion() + "; jdk " + jdk;
      descriptor.myResolver = Resolver.createUnionResolver(name, resolvers);
      descriptor.myInitialized = true;
    }

    return descriptor;
  }

  @NotNull
  private List<Resolver> createPluginWithAllResolvers(@NotNull Plugin plugin, @NotNull Ide ide, @NotNull Jdk jdk, @Nullable Resolver externalClassPath, PluginDependenciesDescriptor transitives) {
    List<Resolver> resolvers = new ArrayList<Resolver>();

    //TODO: check the class-path sequence

    //JDK classes go first in classpath
    resolvers.add(jdk.getResolver());

    //then do plugin classes
    resolvers.add(plugin.getPluginResolver());

    //then do IDE classes
    resolvers.add(ide.getResolver());

    //then do dependencies
    for (Plugin dep : transitives.getDependencies()) {
      Resolver pluginResolver = dep.getPluginResolver();
      if (!pluginResolver.isEmpty()) {
        resolvers.add(pluginResolver);
      }
    }

    if (externalClassPath != null) {
      resolvers.add(externalClassPath);
    }

    return resolvers;
  }


  @NotNull
  @TestOnly
  public PluginDependenciesDescriptor calcDependenciesWithTransitive(@NotNull Ide ide,
                                                                     @NotNull Plugin plugin,
                                                                     @NotNull List<PluginDependenciesDescriptor> pluginStack) throws DependenciesError {
    PluginDependenciesDescriptor descriptor = createDependenciesDescriptor(ide, plugin);

    String id = descriptor.getPluginName();

    if (descriptor.myDependencies == DEP_CALC_MARKER) {
      if (failOnCyclicDependency()) {
        int idx = pluginStack.lastIndexOf(descriptor); //compare descriptors by their identity
        String cycle = Joiner.on(" -> ").join(pluginStack.subList(idx, pluginStack.size())) + " -> " + id;
        throw new CyclicDependencyError(cycle);
      }

      for (int i = pluginStack.size() - 1; i >= 0; i--) {
        pluginStack.get(i).myIsCyclic = true;
        if (pluginStack.get(i) == descriptor) break;
      }

      return new PluginDependenciesDescriptor(descriptor.getPluginName(), true);
    }

    if (descriptor.myDependencies == null) {
      //not calculated yet
      descriptor.myDependencies = DEP_CALC_MARKER;

      Set<Plugin> calc = Sets.newIdentityHashSet();

      pluginStack.add(descriptor);

      try {

        //iterate through IntelliJ module dependencies
        for (PluginDependency dependency : plugin.getModuleDependencies()) {

          final String moduleId = dependency.getId();
          if (isDefaultModule(moduleId)) {
            continue;
          }

          Plugin depPlugin = ide.getPluginByModule(moduleId);

          if (depPlugin == null) {
            final String message = "Plugin " + id + " depends on module " + moduleId + " which is not found in " + ide.getVersion();
            if (!dependency.isOptional()) {
              //this is required plugin
              throw new MissingDependenciesError(descriptor.getPluginName(), moduleId, message);
            } else {
              descriptor.addMissingDependency(descriptor.getPluginName(), moduleId, message);
            }
          } else if (calc.add(depPlugin)) {
            PluginDependenciesDescriptor transitive;
            try {
              transitive = calcDependenciesWithTransitive(ide, depPlugin, pluginStack);
            } catch (DependenciesError dependenciesError) {
              if (dependenciesError instanceof MissingDependenciesError) {
                String missedPlugin = ((MissingDependenciesError) dependenciesError).getMissedPlugin();
                final String message = "Plugin " + id + " depends on module " + moduleId + " which has a missing transitive dependency on " + missedPlugin;
                throw new MissingDependenciesError(id, missedPlugin, message, dependenciesError);
              } else {
                throw dependenciesError;
              }
            }

            descriptor.combineOptionalMissing(transitive);
            if (transitive.getDependencies() != null) {
              calc.addAll(transitive.getDependencies());
            }
          }
        }

        //iterate through the other plugin dependencies
        for (PluginDependency pluginDependency : plugin.getDependencies()) {
          Plugin depPlugin = ide.getPluginById(pluginDependency.getId());

          if (depPlugin == null) {
            UpdateInfo updateInfo;
            try {
              updateInfo = RepositoryManager.getInstance().findPlugin(ide.getVersion(), pluginDependency.getId());
            } catch (IOException e) {
              //repository problem
              throw FailUtil.fail("Couldn't get dependency update from the Repository (IDE = " + ide.getVersion() + " plugin = " + id + ")", e);
            }

            if (updateInfo != null) {
              //update does really exist in the repo
              File pluginZip;
              try {
                pluginZip = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
              } catch (IOException e) {
                throw FailUtil.fail("Couldn't get dependency update from the Repository (IDE = " + ide.getVersion() + " plugin = " + id + ")", e);
              }

              try {
                depPlugin = PluginCache.getInstance().createPlugin(pluginZip, true);
              } catch (Exception e) {
                final String message = "Plugin " + id + " depends on the other plugin " + pluginDependency.getId() + " which is incorrect";
                if (!pluginDependency.isOptional()) {
                  //this is a required plugin
                  throw new MissingDependenciesError(id, pluginDependency.getId(), message, e);
                } else {
                  descriptor.addMissingDependency(id, pluginDependency.getId(), message + "(" + e.getLocalizedMessage() + ")");
                }
              }
            }
          }

          if (depPlugin == null) {
            final String message = "Plugin " + id + " depends on the other plugin " + pluginDependency.getId() + " which has not a compatible build with " + ide.getVersion();
            if (!pluginDependency.isOptional()) {
              //this is a required plugin
              throw new MissingDependenciesError(id, pluginDependency.getId(), message);
            } else {
              descriptor.addMissingDependency(descriptor.getPluginName(), pluginDependency.getId(), message);
            }
          } else if (calc.add(depPlugin)) {
            PluginDependenciesDescriptor transitive;
            try {
              transitive = calcDependenciesWithTransitive(ide, depPlugin, pluginStack);
            } catch (DependenciesError dependenciesError) {
              if (dependenciesError instanceof MissingDependenciesError) {
                String missedPlugin = ((MissingDependenciesError) dependenciesError).getMissedPlugin();
                String message = "Plugin " + id + " depends on the other plugin " + pluginDependency.getId() + " which has a missing transitive dependency on " + missedPlugin;
                throw new MissingDependenciesError(id, pluginDependency.getId(), message, dependenciesError);
              } else {
                throw dependenciesError;
              }
            }
            descriptor.combineOptionalMissing(transitive);
            calc.addAll(transitive.getDependencies());
          }
        }

        if (descriptor.myIsCyclic) {
          descriptor.myDependencies = null;
        } else {
          descriptor.myDependencies = calc;
        }
      }
      finally {
        pluginStack.remove(pluginStack.size() - 1);

        if (descriptor.myDependencies == DEP_CALC_MARKER) {
          descriptor.myDependencies = null;
        }
      }
    }

    return descriptor;
  }

  private boolean isDefaultModule(String moduleId) {
    return IDEA_ULTIMATE_MODULES.contains(moduleId);
  }

  private boolean failOnCyclicDependency() {
    //TODO: change this with a method parameter
    return Boolean.parseBoolean(RepositoryConfiguration.getInstance().getProperty("fail.on.cyclic.dependencies"));
  }


}
