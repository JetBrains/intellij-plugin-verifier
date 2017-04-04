package com.intellij.structure.impl.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static com.intellij.structure.impl.utils.StringUtil.isEmpty;

public class PluginImpl implements Plugin {

  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles  = new HashMap<PluginDependency, String>();
  private final Map<String, Plugin> myOptionalDescriptors = new HashMap<String, Plugin>();
  private final Set<String> myReferencedClasses = new HashSet<String>();
  private final Multimap<String, Element> myExtensions = ArrayListMultimap.create();
  private final File myPluginFile;
  private Document myUnderlyingDocument;
  private String myFileName;
  private byte[] myLogoContent;
  private String myLogoUrl;
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

  PluginImpl(@NotNull File pluginFile) {
    myPluginFile = pluginFile;
  }

  void setFileName(@NotNull String myFileName) {
    this.myFileName = myFileName;
  }

  void setLogoContent(@Nullable byte[] myLogoContent) {
    this.myLogoContent = myLogoContent;
  }

  void setLogoUrl(@Nullable String myLogoUrl) {
    this.myLogoUrl = myLogoUrl;
  }

  void setPluginVendor(@Nullable String myPluginVendor) {
    this.myPluginVendor = myPluginVendor;
  }

  void setNotes(@Nullable String myNotes) {
    this.myNotes = myNotes;
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

  void setSinceBuild(@Nullable IdeVersion mySinceBuild) {
    this.mySinceBuild = mySinceBuild;
  }

  @Override
  @Nullable
  public IdeVersion getUntilBuild() {
    return myUntilBuild;
  }

  void setUntilBuild(@Nullable IdeVersion myUntilBuild) {
    this.myUntilBuild = myUntilBuild;
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

  void setPluginName(@Nullable String myPluginName) {
    this.myPluginName = myPluginName;
  }

  @Override
  @Nullable
  public String getPluginVersion() {
    return myPluginVersion;
  }

  void setPluginVersion(@Nullable String myPluginVersion) {
    this.myPluginVersion = myPluginVersion;
  }

  @Nullable
  @Override
  public String getPluginId() {
    return myPluginId;
  }

  void setPluginId(@Nullable String myPluginId) {
    this.myPluginId = myPluginId;
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

  void setDescription(@Nullable String myDescription) {
    this.myDescription = myDescription;
  }

  @Override
  @Nullable
  public String getVendorEmail() {
    return myVendorEmail;
  }

  void setVendorEmail(@Nullable String myVendorEmail) {
    this.myVendorEmail = myVendorEmail;
  }

  @Override
  @Nullable
  public String getVendorUrl() {
    return myVendorUrl;
  }

  void setVendorUrl(@Nullable String myVendorUrl) {
    this.myVendorUrl = myVendorUrl;
  }

  @Override
  @Nullable
  public String getUrl() {
    return myUrl;
  }

  void setUrl(@Nullable String myUrl) {
    this.myUrl = myUrl;
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

  void setOptionalDescriptors(@NotNull Map<String, Plugin> optionalDescriptors) {
    myOptionalDescriptors.clear();
    myOptionalDescriptors.putAll(optionalDescriptors);
    for (Plugin optDescriptor : optionalDescriptors.values()) {
      myExtensions.putAll(optDescriptor.getExtensions());
    }
  }

  @Override
  @Nullable
  public byte[] getVendorLogo() {
    return myLogoContent == null ? null : myLogoContent.clone();
  }

  @Nullable
  @Override
  public String getVendorLogoUrl() {
    return myLogoUrl;
  }

  @NotNull
  @Override
  public Document getUnderlyingDocument() {
    return myUnderlyingDocument.clone();
  }

  void setUnderlyingDocument(@NotNull Document myUnderlyingDocument) {
    this.myUnderlyingDocument = myUnderlyingDocument;
  }

  @NotNull
  @Override
  public File getPluginFile() {
    return myPluginFile;
  }

  @NotNull
  Map<PluginDependency, String> getOptionalDependenciesConfigFiles() {
    return Collections.unmodifiableMap(myOptionalConfigFiles);
  }

  void addExtension(String epName, Element element) {
    myExtensions.put(epName, element);
  }

  void addReferencedClass(String className) {
    myReferencedClasses.add(className);
  }

  void addModuleDependency(PluginDependency dependency) {
    myModuleDependencies.add(dependency);
  }

  void addDependency(PluginDependency dependency) {
    myDependencies.add(dependency);
  }

  void addOptionalConfigFile(PluginDependency dependency, String configFile) {
    myOptionalConfigFiles.put(dependency, configFile);
  }

  void addDefinedModule(String definedModule) {
    myDefinedModules.add(definedModule);
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
    if (isEmpty(id)) {
      id = myFileName;
    }
    return id + (getPluginVersion() != null ? ":" + getPluginVersion() : "");
  }

}
