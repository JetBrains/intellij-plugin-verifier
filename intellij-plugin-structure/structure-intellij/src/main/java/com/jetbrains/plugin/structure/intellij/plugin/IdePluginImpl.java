package com.jetbrains.plugin.structure.intellij.plugin;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.jetbrains.plugin.structure.base.plugin.PluginIcon;
import com.jetbrains.plugin.structure.intellij.beans.*;
import com.jetbrains.plugin.structure.intellij.utils.StringUtil;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

import static com.jetbrains.plugin.structure.intellij.utils.StringUtil.isEmpty;

public class IdePluginImpl implements IdePlugin {
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";

  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles = new HashMap<PluginDependency, String>();
  private final Map<String, IdePlugin> myOptionalDescriptors = new HashMap<String, IdePlugin>();
  private final List<PluginIcon> icons = new ArrayList<PluginIcon>();
  private Multimap<String, Element> myExtensions;
  private File myOriginalFile;
  private File myExtractDirectory;
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
  private ProductDescriptor myProductDescriptor;

  IdePluginImpl(@NotNull Document underlyingDocument, @NotNull PluginBean bean) {
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

    IdeaVersionBean ideaVersionBean = bean.ideaVersion;
    if (ideaVersionBean != null) {
      mySinceBuild = ideaVersionBean.sinceBuild != null ? IdeVersion.createIdeVersion(ideaVersionBean.sinceBuild) : null;
      String untilBuild = ideaVersionBean.untilBuild;
      if (!StringUtil.isEmpty(untilBuild)) {
        if (untilBuild.endsWith(".*")) {
          int idx = untilBuild.lastIndexOf('.');
          untilBuild = untilBuild.substring(0, idx + 1) + Integer.MAX_VALUE;
        }
        myUntilBuild = IdeVersion.createIdeVersion(untilBuild);
      }
    }

    if (bean.dependencies != null) {
      for (PluginDependencyBean dependencyBean : bean.dependencies) {
        if (dependencyBean.pluginId != null) {
          boolean isModule = dependencyBean.pluginId.startsWith(INTELLIJ_MODULES_PREFIX);
          PluginDependency dependency = new PluginDependencyImpl(dependencyBean.pluginId, dependencyBean.optional, isModule);
          myDependencies.add(dependency);

          if (dependency.isOptional() && dependencyBean.configFile != null) {
            myOptionalConfigFiles.put(dependency, dependencyBean.configFile);
          }
        }
      }
    }

    PluginVendorBean vendorBean = bean.vendor;
    if (vendorBean != null) {
      myPluginVendor = vendorBean.name != null ? vendorBean.name.trim() : null;
      myVendorUrl = vendorBean.url;
      myVendorEmail = vendorBean.email;
    }
    ProductDescriptorBean productDescriptorBean = bean.productDescriptor;
    if (productDescriptorBean != null) {

      myProductDescriptor = new ProductDescriptor(
          productDescriptorBean.code,
          LocalDate.parse(productDescriptorBean.releaseDate, PluginCreator.releaseDateFormatter),
          Integer.valueOf(productDescriptorBean.releaseVersion)
      );
    }
    myNotes = bean.changeNotes;
    myDescription = bean.description;
  }

  @Override
  @Nullable
  public String getChangeNotes() {
    return myNotes;
  }

  @NotNull
  @Override
  public Map<String, IdePlugin> getOptionalDescriptors() {
    return Collections.unmodifiableMap(myOptionalDescriptors);
  }

  @NotNull
  @Override
  public List<PluginIcon> getIcons() {
    return Collections.unmodifiableList(icons);
  }

  public void setIcons(List<PluginIcon> icons) {
    this.icons.clear();
    this.icons.addAll(icons);
  }

  void addOptionalDescriptor(@NotNull String configurationFile, @NotNull IdePlugin optionalPlugin) {
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

  @Nullable
  @Override
  public ProductDescriptor getProductDescriptor() {
    return myProductDescriptor;
  }

  void setOriginalPluginFile(@NotNull File originalFile) {
    myOriginalFile = originalFile;
  }

  public File getExtractDirectory() {
    return myExtractDirectory;
  }

  void setExtractDirectory(File extractDirectory) {
    this.myExtractDirectory = extractDirectory;
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
