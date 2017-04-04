package com.intellij.structure.impl.domain;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.domain.PluginProblem;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import org.jdom2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static com.intellij.structure.impl.utils.StringUtil.*;

public class PluginImpl implements Plugin {

  static final JDOMXIncluder.PathResolver DEFAULT_PLUGIN_XML_PATH_RESOLVER = new PluginXmlPathResolver();

  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles  = new HashMap<PluginDependency, String>();
  private final Map<String, Plugin> myOptionalDescriptors = new HashMap<String, Plugin>();
  private final Set<String> myReferencedClasses = new HashSet<String>();
  private final Multimap<String, Element> myExtensions = ArrayListMultimap.create();
  @NotNull private final File myPluginFile;
  @NotNull private Document myUnderlyingDocument;
  @NotNull private String myFileName;
  @Nullable private byte[] myLogoContent;
  @Nullable private String myLogoUrl;
  @Nullable private String myPluginName;
  @Nullable private String myPluginVersion;
  @Nullable private String myPluginId;
  @Nullable private String myPluginVendor;
  @Nullable private String myVendorEmail;
  @Nullable private String myVendorUrl;
  @Nullable private String myDescription;
  @Nullable private String myUrl;
  @Nullable private String myNotes;
  @Nullable private IdeVersion mySinceBuild;
  @Nullable private IdeVersion myUntilBuild;

  PluginImpl(File pluginFile) {
    myPluginFile = pluginFile;
  }

  public void setOptionalDescriptors(@NotNull Map<String, Plugin> optionalDescriptors) {
    myOptionalDescriptors.clear();
    myOptionalDescriptors.putAll(optionalDescriptors);
    for (Plugin optDescriptor : optionalDescriptors.values()) {
      myExtensions.putAll(optDescriptor.getExtensions());
    }
  }

  public void setUnderlyingDocument(@NotNull Document myUnderlyingDocument) {
    this.myUnderlyingDocument = myUnderlyingDocument;
  }

  public void setFileName(@NotNull String myFileName) {
    this.myFileName = myFileName;
  }

  public void setLogoContent(@Nullable byte[] myLogoContent) {
    this.myLogoContent = myLogoContent;
  }

  public void setLogoUrl(@Nullable String myLogoUrl) {
    this.myLogoUrl = myLogoUrl;
  }

  public void setPluginName(@Nullable String myPluginName) {
    this.myPluginName = myPluginName;
  }

  public void setPluginVersion(@Nullable String myPluginVersion) {
    this.myPluginVersion = myPluginVersion;
  }

  public void setPluginId(@Nullable String myPluginId) {
    this.myPluginId = myPluginId;
  }

  public void setPluginVendor(@Nullable String myPluginVendor) {
    this.myPluginVendor = myPluginVendor;
  }

  public void setVendorEmail(@Nullable String myVendorEmail) {
    this.myVendorEmail = myVendorEmail;
  }

  public void setVendorUrl(@Nullable String myVendorUrl) {
    this.myVendorUrl = myVendorUrl;
  }

  public void setDescription(@Nullable String myDescription) {
    this.myDescription = myDescription;
  }

  public void setUrl(@Nullable String myUrl) {
    this.myUrl = myUrl;
  }

  public void setNotes(@Nullable String myNotes) {
    this.myNotes = myNotes;
  }

  public void setSinceBuild(@Nullable IdeVersion mySinceBuild) {
    this.mySinceBuild = mySinceBuild;
  }

  public void setUntilBuild(@Nullable IdeVersion myUntilBuild) {
    this.myUntilBuild = myUntilBuild;
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

  @NotNull
  @Override
  public File getPluginFile() {
    return myPluginFile;
  }

  @NotNull
  Map<PluginDependency, String> getOptionalDependenciesConfigFiles() {
    return Collections.unmodifiableMap(myOptionalConfigFiles);
  }

  public void addExtension(String epName, Element element) {
    myExtensions.put(epName, element);
  }

  public void addReferencedClass(String className) {
    myReferencedClasses.add(className);
  }

  public void addModuleDependency(PluginDependency dependency) {
    myModuleDependencies.add(dependency);
  }

  public void addDependency(PluginDependency dependency) {
    myDependencies.add(dependency);
  }

  public void addOptionalConfigFile(PluginDependency dependency, String configFile) {
    myOptionalConfigFiles.put(dependency, configFile);
  }

  public void addDefinedModule(String definedModule) {
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

  static class PluginXmlPathResolver extends JDOMXIncluder.DefaultPathResolver {

    private final List<URL> myPluginMetaInfUrls;

    private PluginXmlPathResolver() {
      myPluginMetaInfUrls = Collections.emptyList();
    }

    PluginXmlPathResolver(List<URL> metaInfUrl) {
      myPluginMetaInfUrls = new ArrayList<URL>(metaInfUrl);
    }

    @NotNull
    private URL defaultResolve(@NotNull String relativePath, @Nullable String base) {
      if (base != null && relativePath.startsWith("/META-INF/")) {
        //for plugin descriptor the root is a directory containing the META-INF
        try {
          return new URL(new URL(base), ".." + relativePath);
        } catch (MalformedURLException e) {
          throw new XIncludeException(e);
        }
      }
      return super.resolvePath(relativePath, base);
    }

    @NotNull
    private URL getMetaInfRelativeUrl(@NotNull URL metaInf, @NotNull String relativePath) throws MalformedURLException {
      if (relativePath.startsWith("/")) {
        return new URL(metaInf, ".." + relativePath);
      } else {
        return new URL(metaInf, relativePath);
      }
    }

    @NotNull
    @Override
    public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
      URL url = defaultResolve(relativePath, base);
      if (!URLUtil.resourceExists(url)) {
        for (URL metaInf : myPluginMetaInfUrls) {
          try {
            URL entryUrl = getMetaInfRelativeUrl(metaInf, relativePath);
            if (URLUtil.resourceExists(entryUrl)) {
              return entryUrl;
            }
          } catch (MalformedURLException ignored) {
          }
        }
      }
      return url;
    }
  }

}
