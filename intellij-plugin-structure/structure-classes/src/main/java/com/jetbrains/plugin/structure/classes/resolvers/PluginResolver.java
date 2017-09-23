package com.jetbrains.plugin.structure.classes.resolvers;

import com.google.common.base.Throwables;
import com.jetbrains.plugin.structure.base.utils.FileUtil;
import com.jetbrains.plugin.structure.classes.locator.ClassesDirectoryLocator;
import com.jetbrains.plugin.structure.classes.locator.CompileServerExtensionLocator;
import com.jetbrains.plugin.structure.classes.locator.IdePluginClassesLocator;
import com.jetbrains.plugin.structure.classes.locator.LibDirectoryLocator;
import com.jetbrains.plugin.structure.intellij.extractor.*;
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import com.jetbrains.plugin.structure.intellij.utils.StringUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class PluginResolver extends Resolver {

  private static final Logger LOG = LoggerFactory.getLogger(PluginResolver.class);
  private static final List<IdePluginClassesLocator> CLASSES_LOCATORS = Arrays.asList(new ClassesDirectoryLocator(), new LibDirectoryLocator(), new CompileServerExtensionLocator());
  private final IdePlugin myPlugin;
  private final ExtractedPluginFile myExtractedPluginFile;
  private final Resolver myResolver;
  private boolean isClosed;

  private PluginResolver(IdePlugin plugin, @NotNull ExtractedPluginFile extractedPluginFile) {
    myPlugin = plugin;
    myExtractedPluginFile = extractedPluginFile;
    try {
      myResolver = loadClasses(myExtractedPluginFile.getActualPluginFile());
    } catch (Throwable e) {
      IOUtils.closeQuietly(extractedPluginFile);
      throw Throwables.propagate(e);
    }
  }

  @NotNull
  public static Resolver createPluginResolver(@NotNull IdePlugin plugin, @NotNull File extractDirectory) throws IOException {
    File pluginFile = plugin.getOriginalFile();
    if (pluginFile == null) {
      return Resolver.getEmptyResolver();
    } else if (!pluginFile.exists()) {
      throw new IllegalArgumentException("Plugin file doesn't exist " + pluginFile);
    }
    if (pluginFile.isDirectory() || FileUtil.INSTANCE.isJarOrZip(pluginFile)) {
      if (FileUtil.INSTANCE.isZip(pluginFile)) {
        ExtractorResult extractorResult = PluginExtractor.INSTANCE.extractPlugin(pluginFile, extractDirectory);
        if (extractorResult instanceof ExtractorSuccess) {
          ExtractedPluginFile extractedPluginFile = ((ExtractorSuccess) extractorResult).getExtractedPlugin();
          return new PluginResolver(plugin, extractedPluginFile);
        } else {
          throw new IOException(((ExtractorFail) extractorResult).getPluginProblem().getMessage());
        }
      }
      return new PluginResolver(plugin, new ExtractedPluginFile(pluginFile, null));
    }
    throw new IllegalArgumentException("Incorrect plugin file type " + pluginFile + ": expected a directory, a .zip or a .jar archive");
  }

  @Override
  public synchronized void close() throws IOException {
    if (isClosed) {
      return;
    }
    isClosed = true;

    try {
      myResolver.close();
    } finally {
      myExtractedPluginFile.close();
    }
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    return myResolver.findClass(className);
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myResolver.getClassLocation(className);
  }

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    return myResolver.getAllClasses();
  }

  @Override
  public boolean isEmpty() {
    return myResolver.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myResolver.containsClass(className);
  }

  @NotNull
  @Override
  public List<File> getClassPath() {
    return myResolver.getClassPath();
  }

  @NotNull
  private Resolver loadClasses(@NotNull File file) throws IOException {
    if (file.isDirectory()) {
      return loadClassesFromDir(file);
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.getName(), ".jar")) {
      return createJarResolver(file);
    }
    throw new IllegalArgumentException("Invalid plugin file extension: " + file + ". It must be a directory or a jar file");
  }

  private Resolver loadClassesFromDir(@NotNull File pluginDirectory) {
    List<Resolver> resolvers = new ArrayList<Resolver>();
    try {
      for (IdePluginClassesLocator classesLocator : CLASSES_LOCATORS) {
        Resolver classes = classesLocator.findClasses(myPlugin, pluginDirectory);
        resolvers.add(classes);
      }
    } catch (Throwable e) {
      closeResolvers(resolvers);
      Throwables.propagate(e);
    }
    return createUnionResolver("Plugin resolver", resolvers);
  }

  private void closeResolvers(List<Resolver> resolvers) {
    for (Resolver resolver : resolvers) {
      try {
        resolver.close();
      } catch (Exception ce) {
        LOG.error("Unable to close resolver " + resolver, ce);
      }
    }
  }
}
