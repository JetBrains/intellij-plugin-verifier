package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.ide.IdeVersion;
import com.intellij.structure.impl.beans.PluginBean;
import com.intellij.structure.impl.beans.PluginDependencyBean;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.plugin.Plugin;
import com.intellij.structure.plugin.PluginDependency;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.util.*;

import static com.intellij.structure.impl.utils.StringUtil.isEmpty;

public class PluginImpl implements Plugin {
  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";

  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles = new HashMap<PluginDependency, String>();
  private final Map<String, Plugin> myOptionalDescriptors = new HashMap<String, Plugin>();
  private final Set<String> myReferencedClasses = new HashSet<String>();
  private Multimap<String, Element> myExtensions;
  private File myOriginalFile;
  private Document myUnderlyingDocument;
  private String myPluginName;
  private String myPluginVersion;
  private String myPluginId;
  private String myPluginVendor;
  private String myVendorEmail;
  private String myVendorUrl;
  private String myDescription;
  private String myUrl;
  private String myNotes;
  private IdeVersion mySinceBuild;
  private IdeVersion myUntilBuild;

  PluginImpl(@NotNull Document underlyingDocument, @NotNull PluginBean bean) {
    myUnderlyingDocument = underlyingDocument;
    setInfoFromBean(bean);
  }

  @Override
  @NotNull
  public Multimap<String, Element> getExtensions() {
    return Multimaps.unmodifiableMultimap(myExtensions);
  }

  @Override
  @NotNull
  public List<PluginDependency> getDependencies() {
    return Collections.unmodifiableList(myDependencies);
  }

  @Override
  @NotNull
  public List<PluginDependency> getModuleDependencies() {
    return Collections.unmodifiableList(myModuleDependencies);
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

  @Override
  public boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion) {
    //noinspection SimplifiableIfStatement
    if (mySinceBuild == null) return true;

    return mySinceBuild.compareTo(ideVersion) <= 0 && (myUntilBuild == null || ideVersion.compareTo(myUntilBuild) <= 0);
  }

  @Nullable
  @Override
  public String getPluginName() {
    return myPluginName;
  }

  @Override
  @Nullable
  public String getPluginVersion() {
    return myPluginVersion;
  }

  @Nullable
  @Override
  public String getPluginId() {
    return myPluginId;
  }

  @Override
  public String getVendor() {
    return myPluginVendor;
  }

  @Override
  @NotNull
  public Set<String> getDefinedModules() {
    return Collections.unmodifiableSet(myDefinedModules);
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  @Nullable
  public String getVendorEmail() {
    return myVendorEmail;
  }

  @Override
  @Nullable
  public String getVendorUrl() {
    return myVendorUrl;
  }

  @Override
  @Nullable
  public String getUrl() {
    return myUrl;
  }

  private void setInfoFromBean(PluginBean bean) {
    myPluginName = bean.name;
    myPluginId = bean.id != null ? bean.id : bean.name;
    myUrl = bean.url;
    myPluginVersion = bean.pluginVersion != null ? bean.pluginVersion.trim() : null;
    myDefinedModules.addAll(bean.modules);
    myExtensions = bean.extensions;
    myReferencedClasses.addAll(bean.classes);

    mySinceBuild = bean.ideaVersion != null ? IdeVersion.createIdeVersion(bean.ideaVersion.sinceBuild) : null;
    String untilBuild = bean.ideaVersion != null ? bean.ideaVersion.untilBuild : null;
    if (!Strings.isNullOrEmpty(untilBuild)) {
      if (untilBuild.endsWith(".*")) {
        int idx = untilBuild.lastIndexOf('.');
        untilBuild = untilBuild.substring(0, idx + 1) + Integer.MAX_VALUE;
      }
      myUntilBuild = IdeVersion.createIdeVersion(untilBuild);
    }

    for (PluginDependencyBean dependencyBean : bean.dependencies) {
      PluginDependency dependency = new PluginDependencyImpl(dependencyBean);
      if (dependency.getId().startsWith(INTELLIJ_MODULES_PREFIX)) {
        myModuleDependencies.add(dependency);
      } else {
        myDependencies.add(dependency);
      }

      if (dependency.isOptional() && dependencyBean.configFile != null) {
        myOptionalConfigFiles.put(dependency, dependencyBean.configFile);
      }
    }
    if (bean.vendor != null) {
      myPluginVendor = bean.vendor.name.trim();
      myVendorUrl = bean.vendor.url;
      myVendorEmail = bean.vendor.email;
    }
    if (!StringUtil.isEmptyOrSpaces(bean.changeNotes)) {
      myNotes = Jsoup.clean(bean.changeNotes.trim(), WHITELIST);
    }
    if (!StringUtil.isEmptyOrSpaces(bean.description)) {
      myDescription = Jsoup.clean(bean.description.trim(), WHITELIST);
    }
  }

  @Override
  @Nullable
  public String getChangeNotes() {
    return myNotes;
  }

  @NotNull
  @Override
  public Set<String> getAllClassesReferencedFromXml() {
    return Collections.unmodifiableSet(myReferencedClasses);
  }

  @NotNull
  @Override
  public Map<String, Plugin> getOptionalDescriptors() {
    return Collections.unmodifiableMap(myOptionalDescriptors);
  }

  void addOptionalDescriptor(@NotNull String configurationFile, @NotNull Plugin optionalPlugin) {
    myOptionalDescriptors.put(configurationFile, optionalPlugin);
    myExtensions.putAll(optionalPlugin.getExtensions());
  }

  @NotNull
  @Override
  public Document getUnderlyingDocument() {
    return myUnderlyingDocument.clone();
  }

  @Nullable
  @Override
  public File getOriginalFile() {
    return myOriginalFile;
  }

  void setOriginalPluginFile(@NotNull File originalFile) {
    myOriginalFile = originalFile;
  }

  @NotNull
  Map<PluginDependency, String> getOptionalDependenciesConfigFiles() {
    return Collections.unmodifiableMap(myOptionalConfigFiles);
  }

  @Override
  public String toString() {
    String id = myPluginId;
    if (isEmpty(id)) {
      id = myPluginName;
    }
    if (isEmpty(id)) {
      id = myUrl;
    }
    return id + (getPluginVersion() != null ? ":" + getPluginVersion() : "");
  }

}
