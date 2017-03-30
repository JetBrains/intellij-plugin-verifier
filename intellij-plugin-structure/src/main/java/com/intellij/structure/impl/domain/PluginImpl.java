package com.intellij.structure.impl.domain;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
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

  private final Set<String> myDefinedModules;
  private final List<PluginDependency> myDependencies;
  private final List<PluginDependency> myModuleDependencies;
  private final Map<PluginDependency, String> myOptionalConfigFiles;
  private final Map<String, Plugin> myOptionalDescriptors;
  private final Set<String> myReferencedClasses;
  private final Multimap<String, Element> myExtensions;
  @NotNull private final File myPluginFile;
  private final List<String> myHints;
  @NotNull private final Document myUnderlyingDocument;
  @NotNull private final String myFileName;
  @Nullable private final byte[] myLogoContent;
  @Nullable private final String myLogoUrl;
  @Nullable private final String myPluginName;
  @Nullable private final String myPluginVersion;
  @Nullable private final String myPluginId;
  @Nullable private final String myPluginVendor;
  @Nullable private final String myVendorEmail;
  @Nullable private final String myVendorUrl;
  @Nullable private final String myDescription;
  @Nullable private final String myUrl;
  @Nullable private final String myNotes;
  @Nullable private final IdeVersion mySinceBuild;
  @Nullable private final IdeVersion myUntilBuild;

  PluginImpl(Set<String> myDefinedModules, List<PluginDependency> myDependencies,
             List<PluginDependency> myModuleDependencies, Map<PluginDependency, String> myOptionalConfigFiles,
             Map<String, Plugin> myOptionalDescriptors, Set<String> myReferencedClasses,
             Multimap<String, Element> myExtensions, @NotNull File pluginFile, List<String> myHints,
             @NotNull Document myUnderlyingDocument, @NotNull String myFileName, @Nullable byte[] myLogoContent,
             @Nullable String myLogoUrl, @Nullable String myPluginName, @Nullable String myPluginVersion,
             @Nullable String myPluginId, @Nullable String myPluginVendor, @Nullable String myVendorEmail,
             @Nullable String myVendorUrl, @Nullable String myDescription, @Nullable String myUrl,
             @Nullable String myNotes, @Nullable IdeVersion mySinceBuild, @Nullable IdeVersion myUntilBuild) {
    this.myDefinedModules = myDefinedModules;
    this.myDependencies = myDependencies;
    this.myModuleDependencies = myModuleDependencies;
    this.myOptionalConfigFiles = myOptionalConfigFiles;
    this.myOptionalDescriptors = myOptionalDescriptors;
    this.myReferencedClasses = myReferencedClasses;
    this.myExtensions = myExtensions;
    myPluginFile = pluginFile;
    this.myHints = myHints;
    this.myUnderlyingDocument = myUnderlyingDocument;
    this.myFileName = myFileName;
    this.myLogoContent = myLogoContent;
    this.myLogoUrl = myLogoUrl;
    this.myPluginName = myPluginName;
    this.myPluginVersion = myPluginVersion;
    this.myPluginId = myPluginId;
    this.myPluginVendor = myPluginVendor;
    this.myVendorEmail = myVendorEmail;
    this.myVendorUrl = myVendorUrl;
    this.myDescription = myDescription;
    this.myUrl = myUrl;
    this.myNotes = myNotes;
    this.mySinceBuild = mySinceBuild;
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
  public List<String> getHints() {
    return Collections.unmodifiableList(myHints);
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
