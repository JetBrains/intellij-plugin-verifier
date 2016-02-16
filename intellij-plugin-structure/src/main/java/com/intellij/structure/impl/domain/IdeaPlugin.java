package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.errors.IncorrectCompatibleBuildsException;
import com.intellij.structure.impl.errors.MissingPluginIdException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.resolvers.Resolver;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

class IdeaPlugin implements Plugin {

  private final Resolver myPluginResolver;
  private final Resolver myLibraryResolver;

  private final String myPluginName;
  private final String myPluginVersion;
  private final String myPluginId;
  private final String myPluginVendor;

  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Set<String> myDefinedModules;
  private final URL myMainJarUrl;
  private final Document myPluginXml;
  private final Map<String, Document> myXmlDocumentsInRoot;
  private IdeVersion mySinceBuild;
  private IdeVersion myUntilBuild;

  IdeaPlugin(@NotNull URL mainJarUrl,
             @NotNull String pluginMoniker,
             @NotNull Resolver pluginResolver,
             @NotNull Resolver libraryResolver,
             @NotNull Document pluginXml,
             @NotNull Map<String, Document> xmlDocumentsInRoot) throws IncorrectPluginException {
    myMainJarUrl = mainJarUrl;
    myPluginXml = pluginXml;
    myXmlDocumentsInRoot = xmlDocumentsInRoot;
    myPluginResolver = pluginResolver;
    myLibraryResolver = libraryResolver;

    myPluginId = getPluginId(pluginXml);
    if (myPluginId == null) {
      throw new MissingPluginIdException("No id or name in META-INF/plugin.xml for plugin " + pluginMoniker);
    }

    String name = pluginXml.getRootElement().getChildTextTrim("name");
    if (Strings.isNullOrEmpty(name)) {
      name = myPluginId;
    }
    myPluginName = name;
    myPluginVersion = StringUtil.notNullize(pluginXml.getRootElement().getChildTextTrim("version"));
    myPluginVendor = StringUtil.notNullize(pluginXml.getRootElement().getChildTextTrim("vendor"));

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
    //to pretend modification
    Map<String, Document> copy = new HashMap<String, Document>();
    for (Map.Entry<String, Document> entry : myXmlDocumentsInRoot.entrySet()) {
      copy.put(entry.getKey(), (Document) entry.getValue().clone());
    }
    return copy;
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
    return (Document) myPluginXml.clone();
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
  public boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion) {
    //noinspection SimplifiableIfStatement
    if (mySinceBuild == null) return true;

    return IdeVersion.VERSION_COMPARATOR.compare(mySinceBuild, ideVersion) <= 0
        && (myUntilBuild == null || IdeVersion.VERSION_COMPARATOR.compare(ideVersion, myUntilBuild) <= 0);
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
      String sb = ideaVersion.getAttributeValue("since-build");
      try {
        mySinceBuild = IdeVersion.createIdeVersion(sb);
      } catch (IllegalArgumentException e) {
        throw new IncorrectCompatibleBuildsException("<idea version since-build = /> attribute has incorrect value: " + sb);
      }

      String ub = ideaVersion.getAttributeValue("until-build");
      if (!Strings.isNullOrEmpty(ub)) {
        if (ub.endsWith(".*") || ub.endsWith(".999") || ub.endsWith(".9999") || ub.endsWith(".99999")) {
          int idx = ub.lastIndexOf('.');
          ub = ub.substring(0, idx + 1) + Integer.MAX_VALUE;
        }

        try {
          myUntilBuild = IdeVersion.createIdeVersion(ub);
        } catch (IllegalArgumentException e) {
          throw new IncorrectCompatibleBuildsException("<idea-version until-build= /> attribute has incorrect value: " + ub);
        }
      }
    }
  }

  @Override
  @NotNull
  public Resolver getPluginClassPool() {
    return myPluginResolver;
  }

  @Override
  @NotNull
  public Resolver getLibraryClassPool() {
    return myLibraryResolver;
  }

  @Nullable
  @Override
  public InputStream getResourceFile(@NotNull String relativePath) {
    relativePath = StringUtil.trimStart(relativePath, "/");
    URL url;
    try {
      url = new URL(myMainJarUrl.toExternalForm() + relativePath);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("File path is invalid " + relativePath);
    }
    try {
      return URLUtil.openStream(url);
    } catch (IOException e) {
      //consider it doesn't exist
      return null;
    }
  }

}
