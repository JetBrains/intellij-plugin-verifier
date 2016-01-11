package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectCompatibleBuildsException;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.errors.MissingPluginIdException;
import com.intellij.structure.impl.pool.ContainerClassPool;
import com.intellij.structure.pool.ClassPool;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

class IdeaPlugin implements Plugin {

  private final ClassPool myPluginClassPool;
  private final ClassPool myLibraryClassPool;
  private final ClassPool myAllClassesClassPool;

  private final String myPluginName;
  private final String myPluginVersion;
  private final String myPluginId;
  private final String myPluginVendor;

  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Set<String> myDefinedModules;
  private final Document myPluginXml;
  private final Map<String, Document> myXmlDocumentsInRoot;
  private IdeVersion mySinceBuild;
  private IdeVersion myUntilBuild;

  IdeaPlugin(@NotNull File pluginFile,
             @NotNull ClassPool pluginClassPool,
             @NotNull ClassPool libraryClassPool,
             @NotNull Document pluginXml,
             @NotNull Map<String, Document> xmlDocumentsInRoot) throws IncorrectPluginException {
    myPluginXml = pluginXml;
    myXmlDocumentsInRoot = xmlDocumentsInRoot;
    myPluginClassPool = pluginClassPool;
    myLibraryClassPool = libraryClassPool;
    myAllClassesClassPool = ContainerClassPool.getUnion(pluginFile.getAbsolutePath(), Arrays.asList(pluginClassPool, libraryClassPool));

    myPluginId = getPluginId(pluginXml);
    if (myPluginId == null) {
      throw new MissingPluginIdException("No id or name in META-INF/plugin.xml for plugin " + pluginFile);
    }

    String name = pluginXml.getRootElement().getChildTextTrim("name");
    if (Strings.isNullOrEmpty(name)) {
      name = myPluginId;
    }
    myPluginName = name;
    myPluginVersion = pluginXml.getRootElement().getChildTextTrim("version");
    myPluginVendor = pluginXml.getRootElement().getChildTextTrim("vendor");

    loadPluginDependencies(pluginXml);
    myDefinedModules = loadModules(pluginXml);

    getIdeaVersion(pluginXml);
  }

  @Nullable
  private static String getPluginId(@NotNull Document pluginXml) {
    String id = pluginXml.getRootElement().getChildText("id");
    if (id == null || id.isEmpty()) {
      String name = pluginXml.getRootElement().getChildText("name");
      if (name == null || name.isEmpty()) {
        return null;
      }
      return name;
    }
    return id;
  }


  @Override
  @NotNull
  public Map<String, Document> getAllXmlInRoot() {
    return myXmlDocumentsInRoot;
  }

  @Override
  @NotNull
  public List<PluginDependency> getDependencies() {
    return myDependencies;
  }

  @Override
  @NotNull
  public List<PluginDependency> getModuleDependencies() {
    return myModuleDependencies;
  }

  @Override
  @NotNull
  public Document getPluginXml() {
    return myPluginXml;
  }

  @Override
  @Nullable
  public IdeVersion getSinceBuild() {
    return mySinceBuild;
  }

  @Override
  @Nullable
  public IdeVersion getUntilBuild() {
    return myUntilBuild;
  }

  private void loadPluginDependencies(@NotNull Document pluginXml) {
    final List dependsElements = pluginXml.getRootElement().getChildren("depends");

    for (Object dependsObj : dependsElements) {
      Element dependsElement = (Element) dependsObj;

      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      PluginDependency dependency = new PluginDependency(pluginId, optional);
      if (pluginId.startsWith("com.intellij.modules.")) {
        myModuleDependencies.add(dependency);
      } else {
        myDependencies.add(dependency);
      }
    }
  }

  @Override
  public boolean isCompatibleWithIde(@NotNull String ideVersion) {
    IdeVersion ide = IdeVersion.createIdeVersion(ideVersion);
    if (!ide.isOk()) {
      return false;
    }

    //noinspection SimplifiableIfStatement
    if (mySinceBuild == null) return true;

    return IdeVersion.VERSION_COMPARATOR.compare(mySinceBuild, ide) <= 0
        && (myUntilBuild == null || IdeVersion.VERSION_COMPARATOR.compare(ide, myUntilBuild) <= 0);
  }

  @NotNull
  private Set<String> loadModules(@NotNull Document pluginXml) {
    LinkedHashSet<String> modules = new LinkedHashSet<String>();
    @SuppressWarnings("unchecked")
    Iterable<? extends Element> children = (Iterable<? extends Element>) pluginXml.getRootElement().getChildren("module");
    for (Element module : children) {
      modules.add(module.getAttributeValue("value"));
    }
    return modules;
  }

  @Override
  @NotNull
  public String getPluginName() {
    return myPluginName;
  }

  @Override
  @NotNull
  public String getPluginVersion() {
    return myPluginVersion;
  }

  @Override
  @NotNull
  public String getPluginId() {
    return myPluginId;
  }

  @Override
  @NotNull
  public String getPluginVendor() {
    return myPluginVendor;
  }

  @Override
  @NotNull
  public Set<String> getDefinedModules() {
    return Collections.unmodifiableSet(myDefinedModules);
  }

  private void getIdeaVersion(@NotNull Document pluginXml) throws IncorrectCompatibleBuildsException {
    Element ideaVersion = pluginXml.getRootElement().getChild("idea-version");
    if (ideaVersion != null && ideaVersion.getAttributeValue("min") == null) { // min != null in legacy plugins.
      String sinceBuildString = ideaVersion.getAttributeValue("since-build");
      mySinceBuild = IdeVersion.createIdeVersion(sinceBuildString);
      if (!mySinceBuild.isOk()) {
        throw new IncorrectCompatibleBuildsException("<idea version since-build = /> attribute has incorrect value: " + sinceBuildString);
      }

      String untilBuildString = ideaVersion.getAttributeValue("until-build");
      if (!Strings.isNullOrEmpty(untilBuildString)) {
        if (untilBuildString.endsWith(".*") || untilBuildString.endsWith(".999") || untilBuildString.endsWith(".9999") || untilBuildString.endsWith(".99999")) {
          int idx = untilBuildString.lastIndexOf('.');
          untilBuildString = untilBuildString.substring(0, idx + 1) + Integer.MAX_VALUE;
        }

        myUntilBuild = IdeVersion.createIdeVersion(untilBuildString);
        if (!myUntilBuild.isOk()) {
          throw new IncorrectCompatibleBuildsException("<idea-version until-build= /> attribute has incorrect value: " + untilBuildString);
        }
      }
    }
  }

  @Override
  @NotNull
  public ClassPool getPluginClassPool() {
    return myPluginClassPool;
  }

  @Override
  @NotNull
  public ClassPool getLibraryClassPool() {
    return myLibraryClassPool;
  }

  @Override
  @NotNull
  public ClassPool getAllClassesPool() {
    return myAllClassesClassPool;
  }
}
