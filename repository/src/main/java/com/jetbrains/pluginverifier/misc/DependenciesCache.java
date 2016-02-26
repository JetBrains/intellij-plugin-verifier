package com.jetbrains.pluginverifier.misc;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.Jdk;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.FailUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private final WeakHashMap<Ide, WeakHashMap<Plugin, PluginDependenciesDescriptor>> map = new WeakHashMap<Ide, WeakHashMap<Plugin, PluginDependenciesDescriptor>>();

  private final WeakHashMap<Ide, Resolver> myIdeResolvers = new WeakHashMap<Ide, Resolver>();

  private DependenciesCache() {
  }

  @NotNull
  public static DependenciesCache getInstance() {
    return ourInstance;
  }

  @NotNull
  private PluginDependenciesDescriptor getPluginDependenciesDescriptor(@NotNull Ide ide, @NotNull Plugin plugin) {
    WeakHashMap<Plugin, PluginDependenciesDescriptor> m = map.get(ide);
    if (m == null) {
      m = new WeakHashMap<Plugin, PluginDependenciesDescriptor>();
      map.put(ide, m);
    }

    PluginDependenciesDescriptor descr = m.get(plugin);
    if (descr == null) {
      String pluginIdentifier = plugin.getPluginId().isEmpty() ? plugin.getPluginName() : plugin.getPluginId();
      descr = new PluginDependenciesDescriptor(pluginIdentifier, false);
      m.put(plugin, descr);
    }

    return descr;
  }

  @NotNull
  private Resolver getResolverForIde(@NotNull Ide ide, @NotNull Jdk jdk, @Nullable Resolver externalClassPath) {
    Resolver resolver = myIdeResolvers.get(ide);
    if (resolver == null) {
      List<Resolver> resolvers = new ArrayList<Resolver>();
      resolvers.add(ide.getResolver());
      resolvers.add(jdk.getResolver());
      if (externalClassPath != null) {
        resolvers.add(externalClassPath);
      }
      String moniker = String.format("Ide-%s+Jdk-%s-resolver", ide.getVersion(), jdk);
      resolver = Resolver.createCacheResolver(Resolver.getUnion(moniker, resolvers));
      myIdeResolvers.put(ide, resolver);
    }
    return resolver;
  }


  @NotNull
  public PluginDependenciesDescriptor getResolver(@NotNull Plugin plugin, @NotNull Ide ide, @NotNull Jdk jdk, @Nullable Resolver externalClassPath) throws VerificationError {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);
    if (descriptor.myResolver == null) {
      //not calculated yet

      final PluginDependenciesDescriptor all = getDependenciesWithTransitive(ide, plugin, new ArrayList<PluginDependenciesDescriptor>());

      List<Resolver> resolvers = new ArrayList<Resolver>();

      resolvers.add(Resolver.getUnion(plugin.getPluginId(), Arrays.asList(plugin.getPluginResolver(), plugin.getLibraryResolver())));

      resolvers.add(getResolverForIde(ide, jdk, externalClassPath));

      for (Plugin dep : all.getDependencies()) {
        Resolver pluginResolver = dep.getPluginResolver();
        if (!pluginResolver.isEmpty()) {
          resolvers.add(pluginResolver);
        }

        Resolver libraryResolver = dep.getLibraryResolver();
        if (!libraryResolver.isEmpty()) {
          resolvers.add(libraryResolver);
        }
      }

      String moniker = String.format("Plugin-%s+Ide-%s+Jdk-%s", plugin.getPluginId(), ide.getVersion(), jdk.toString());
      descriptor.myResolver = Resolver.getUnion(moniker, resolvers);
    }

    return descriptor;
  }


  @NotNull
  public PluginDependenciesDescriptor getDependenciesWithTransitive(@NotNull Ide ide,
                                                                    @NotNull Plugin plugin,
                                                                    @NotNull List<PluginDependenciesDescriptor> pluginStack) throws VerificationError {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);

    if (descriptor.myDependencies == DEP_CALC_MARKER) {
      if (failOnCyclicDependency()) {
        int idx = pluginStack.lastIndexOf(descriptor); //compare descriptors by their identity (default equals behavior)
        throw new VerificationError("Cyclic plugin dependencies: " + Joiner.on(" -> ").join(pluginStack.subList(idx, pluginStack.size())) + " -> " + plugin.getPluginId());
      }

      for (int i = pluginStack.size() - 1; i >= 0; i--) {
        pluginStack.get(i).myIsCyclic = true;
        if (pluginStack.get(i) == descriptor) break;
      }

      return new PluginDependenciesDescriptor(descriptor.getPluginName(), true);
    }

    if (descriptor.myDependencies == null) {
      //not calculated yet
      Set<Plugin> calc = new HashSet<Plugin>();

      descriptor.myDependencies = DEP_CALC_MARKER;
      pluginStack.add(descriptor);

      try {

        //iterate through IntelliJ module dependencies
        for (PluginDependency dependency : plugin.getModuleDependencies()) {

          final String moduleId = dependency.getId();
          if (IDEA_ULTIMATE_MODULES.contains(moduleId)) {
            //by default IDEA Ultimate contains this module
            continue;
          }

          Plugin depPlugin = ide.getPluginByModule(moduleId);

          if (depPlugin == null) {
            final String message = "Plugin " + plugin.getPluginId() + " depends on module " + moduleId + " which is not found in " + ide.getVersion();
            if (!dependency.isOptional()) {
              //this is required plugin
              throw new VerificationError(message);
            } else {
              descriptor.addMissingDependency(descriptor.getPluginName(), moduleId, message);
            }
          } else if (calc.add(depPlugin)) {
            PluginDependenciesDescriptor transitive = getDependenciesWithTransitive(ide, depPlugin, pluginStack);
            descriptor.combineMissing(transitive);
            if (transitive.getDependencies() != null) {
              calc.addAll(transitive.getDependencies());
            }
          }
        }

        //iterate through the other plugin dependencies
        for (PluginDependency pluginDependency : plugin.getDependencies()) {
          Plugin depPlugin = ide.getPluginById(pluginDependency.getId());

          Exception maybeException = null;
          if (depPlugin == null) {
            UpdateInfo updateInfo;
            try {
              updateInfo = RepositoryManager.getInstance().findPlugin(ide.getVersion().getFullPresentation(), pluginDependency.getId());
            } catch (IOException e) {
              throw FailUtil.fail("Couldn't get dependency update from the Repository (IDE = " + ide.getVersion() + " plugin = " + plugin.getPluginId() + ")", e);
            }

            if (updateInfo != null) {
              try {
                File pluginZip = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
                depPlugin = PluginCache.getInstance().getPlugin(pluginZip);
              } catch (IOException e) {
                maybeException = e;
                System.err.println("Couldn't load plugin dependency for " + plugin.getPluginId());
                e.printStackTrace();
              }
            }
          }

          if (depPlugin == null) {
            final String message = "Plugin " + plugin.getPluginId() + " depends on the other plugin " + pluginDependency.getId() + " which is not found for " + ide.getVersion();
            if (!pluginDependency.isOptional()) {
              //this is required plugin
              throw new VerificationError(message, maybeException);
            } else {
              descriptor.addMissingDependency(descriptor.getPluginName(), pluginDependency.getId(), message);
            }
          } else if (calc.add(depPlugin)) {
            PluginDependenciesDescriptor transitive = getDependenciesWithTransitive(ide, depPlugin, pluginStack);
            descriptor.combineMissing(transitive);
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

  private boolean failOnCyclicDependency() {
    return Boolean.parseBoolean(RepositoryConfiguration.getInstance().getProperty("fail.on.cyclic.dependencies"));
  }

  public static class PluginDependenciesDescriptor {
    final String myPluginName;

    Resolver myResolver;

    boolean myIsCyclic;

    Set<Plugin> myDependencies;

    /**
     * pluginId -> (missingPluginId -> description)
     */
    Map<String, Map<String, String>> myMissingDependencies = new HashMap<String, Map<String, String>>();

    PluginDependenciesDescriptor(@NotNull String pluginName, boolean isEmpty) {
      this.myPluginName = pluginName;
      if (isEmpty) {
        myResolver = Resolver.getEmptyResolver();
        myIsCyclic = true;
        myDependencies = Collections.emptySet();
      }
    }

    public Set<Plugin> getDependencies() {
      return myDependencies;
    }


    public Resolver getResolver() {
      return myResolver;
    }

    public Map<String, Map<String, String>> getMissingDependencies() {
      return myMissingDependencies;
    }

    public String getPluginName() {
      return myPluginName;
    }

    void addMissingDependency(@NotNull String pluginId, @NotNull String missingId, @NotNull String message) {
      Map<String, String> map = myMissingDependencies.get(pluginId);
      if (map == null) {
        map = new HashMap<String, String>();
        myMissingDependencies.put(pluginId, map);
      }
      map.put(missingId, message);
    }

    void combine(PluginDependenciesDescriptor other) {
      combineDependencies(other);
      combineMissing(other);
    }

    void combineDependencies(PluginDependenciesDescriptor other) {
      if (other.getDependencies() != null) {
        myDependencies.addAll(other.getDependencies());
      }
    }

    void combineMissing(PluginDependenciesDescriptor other) {
      Map<String, Map<String, String>> map = other.getMissingDependencies();
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
  }
}
