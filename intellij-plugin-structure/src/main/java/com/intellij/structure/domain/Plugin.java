package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Patrikeev
 */
public interface Plugin {
  @NotNull
  Document getPluginXml();

  /**
   * Returns all the .xml-files under the <i>META-INF/</i> subdirectory.<p> <i>META-INF/</i> is a directory containing
   * the mandatory <i>META-INF/plugin.xml</i> entry with plugin info.<p>
   *
   * @return all the .xml files under <i>META-INF</i> in form: (relative path to the xml-entry) <b>-></b>
   * (<i>content</i>)
   */
  @NotNull
  Map<String, Document> getAllXmlInRoot();

  @Nullable
  IdeVersion getSinceBuild();

  @Nullable
  IdeVersion getUntilBuild();

  boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion);

  @NotNull
  List<PluginDependency> getDependencies();

  @NotNull
  List<PluginDependency> getModuleDependencies();

  @NotNull
  String getPluginName();

  @NotNull
  String getPluginVersion();

  @NotNull
  String getPluginId();

  @NotNull
  String getPluginVendor();

  @NotNull
  Set<String> getDefinedModules();

  @NotNull
  Resolver getPluginClassPool();

  @NotNull
  Resolver getLibraryClassPool();

  /**
   * Returns a {@code stream} representing a file with the following absolute path: given a {@code relativePath} the
   * absolute path is constructed as concatenation of the main-<i>.jar</i> path inside a plugin (that is a .jar
   * containing a META-INF/plugin.xml entry) and a given relative path.
   * <p>
   * For example, given the following plugin structure
   * <pre>
   * .IntelliJIDEAx0/
   *    plugins/
   *        sample.jar/
   *            com/foo/...
   *            bar/images/logo.png
   *            ...
   *            ...
   *            META-INF/
   *              plugin.xml
   * </pre>
   * invocation of {@code Plugin.getResourceFile("bar/images/logo.png")} returns an input stream of <b>logo.png</b>
   *
   * @param relativePath a path relative to the path of the main .jar file
   * @return stream for a file or {@code null} if file is not found
   */
  @Nullable
  InputStream getResourceFile(@NotNull String relativePath);

}
