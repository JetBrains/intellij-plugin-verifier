package com.intellij.structure.impl.resolvers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.impl.domain.IdeManagerImpl;
import com.intellij.structure.impl.utils.JarsUtils;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.structure.resolvers.Resolver.createUnionResolver;

/**
 * @author Sergey Patrikeev
 */
public class IdeResolverCreator {

  private static final String[] HARD_CODED_LIB_FOLDERS = new String[] {
      "community/android/android/lib",
      "community/plugins/gradle/lib"
  };

  private static final Logger LOG = LoggerFactory.getLogger(IdeResolverCreator.class);

  @NotNull
  public static Resolver createIdeResolver(@NotNull Ide ide) throws IOException {
    File idePath = ide.getIdePath();
    if (IdeManagerImpl.isSourceDir(idePath)) {
      return IdeResolverCreator.getIdeaResolverFromSources(idePath);
    } else {
      return IdeResolverCreator.getIdeResolverFromLibraries(idePath);
    }
  }

  @NotNull
  private static Resolver getIdeResolverFromLibraries(File ideDir) throws IOException {
    final File lib = new File(ideDir, "lib");
    if (!lib.isDirectory()) {
      throw new IOException("Directory \"lib\" is not found (should be found at " + lib + ")");
    }

    final Collection<File> jars = JarsUtils.collectJars(lib, new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return !file.getName().endsWith("javac2.jar");
      }
    }, false);

    return JarsUtils.makeResolver("Idea `lib` dir: " + lib.getCanonicalPath(), jars);
  }

  @NotNull
  private static Resolver getIdeaResolverFromSources(@NotNull File ideaDir) throws IOException {
    List<Resolver> pools = new ArrayList<Resolver>();

    pools.add(getIdeResolverFromLibraries(ideaDir));

    if (IdeManagerImpl.isUltimate(ideaDir)) {
      pools.add(new CompileOutputResolver(IdeManagerImpl.getUltimateClassesRoot(ideaDir)));
      pools.add(getIdeResolverFromLibraries(new File(ideaDir, "community")));
      pools.add(hardCodedUltimateLibraries(ideaDir));
    } else {
      pools.add(new CompileOutputResolver(IdeManagerImpl.getCommunityClassesRoot(ideaDir)));
    }

    return createUnionResolver("Idea dir: " + ideaDir.getCanonicalPath(), pools);
  }

  @NotNull
  private static Resolver hardCodedUltimateLibraries(File ideaDir) {
    for (String libFolder : HARD_CODED_LIB_FOLDERS) {
      File libDir = new File(ideaDir, libFolder);
      if (libDir.isDirectory()) {
        try {
          return JarsUtils.makeResolver(libDir.getName() + " `lib` dir", JarsUtils.collectJars(libDir, Predicates.<File>alwaysTrue(), false));
        } catch (IOException e) {
          LOG.warn("Unable to read libraries from " + libDir, e);
        }
      }
    }
    return Resolver.getEmptyResolver();
  }

}
