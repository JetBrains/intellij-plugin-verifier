package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.resolvers.Resolver;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

class IdePluginImpl implements Plugin {

  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";
  private final Resolver myPluginResolver;
  private final Resolver myLibraryResolver;
  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final URL myMainJarUrl;
  private final Document myPluginXml;
  private final Map<String, Document> myXmlDocumentsInRoot;
  private String myPluginName;
  private String myPluginVersion;
  private String myPluginId;
  private String myPluginVendor;
  private String myVendorEmail;
  private String myVendorUrl;
  private String myVendorLogoPath;
  private String myDescription;
  private String myUrl;
  private String myNotes;
  private IdeVersion mySinceBuild;
  private IdeVersion myUntilBuild;


  IdePluginImpl(@NotNull URL mainJarUrl,
                @NotNull Resolver pluginResolver,
                @NotNull Resolver libraryResolver,
                @NotNull Document pluginXml,
                @NotNull Map<String, Document> xmlDocumentsInRoot) throws IncorrectPluginException {
    myMainJarUrl = mainJarUrl;
    myPluginXml = pluginXml;
    myXmlDocumentsInRoot = xmlDocumentsInRoot;
    myPluginResolver = pluginResolver;
    myLibraryResolver = libraryResolver;

    setEntries(pluginXml);
  }

  private void setEntries(@NotNull Document pluginXml) {
    Element rootElement = pluginXml.getRootElement();
    if (rootElement == null) {
      throw new IncorrectPluginException("Failed to parse plugin.xml: root element not found");
    }

    if (!"idea-plugin".equals(rootElement.getName())) {
      throw new IncorrectPluginException("Invalid plugin.xml: root element must be 'idea-plugin'");
    }

    myPluginName = rootElement.getChildTextTrim("name");
    if (Strings.isNullOrEmpty(myPluginName)) {
      throw new IncorrectPluginException("Invalid plugin.xml: 'name' is not specified");
    }

    myPluginId = rootElement.getChildText("id");
    if (myPluginId == null) {
      myPluginId = myPluginName;
    }

    myUrl = StringUtil.notNullize(rootElement.getAttributeValue("url"));

    Element vendorElement = rootElement.getChild("vendor");
    if (vendorElement == null) {
      throw new IncorrectPluginException("Invalid plugin.xml: element 'vendor' not found");
    }

    myPluginVendor = vendorElement.getTextTrim();
    myVendorEmail = StringUtil.notNullize(vendorElement.getAttributeValue("email"));
    myVendorUrl = StringUtil.notNullize(vendorElement.getAttributeValue("url"));
    myVendorLogoPath = vendorElement.getAttributeValue("logo");

    myPluginVersion = rootElement.getChildTextTrim("version");
    if (myPluginVersion == null) {
      throw new IncorrectPluginException("Invalid plugin.xml: version is not specified");
    }

    Element ideaVersionElement = rootElement.getChild("idea-version");
    if (ideaVersionElement == null) {
      throw new IncorrectPluginException("Invalid plugin.xml: element 'idea-version' not found");
    }
    setSinceUntilBuilds(ideaVersionElement);

    setPluginDependencies(rootElement);

    setDefinedModules(pluginXml);


    String description = rootElement.getChildTextTrim("description");
    if (StringUtil.isNullOrEmpty(description)) {
      throw new IncorrectPluginException("Invalid plugin.xml: description is empty");
    } else {
      myDescription = Jsoup.clean(description, WHITELIST);
    }

    List changeNotes = rootElement.getChildren("change-notes");
    if (changeNotes != null && changeNotes.size() > 0) {
      Object o = changeNotes.get(0);
      if (o instanceof Element) {
        Element currentNote = (Element) o;
        String textTrim = currentNote.getTextTrim();
        if (!StringUtil.isNullOrEmpty(textTrim)) {
          myNotes = Jsoup.clean(textTrim, WHITELIST);
        }
      }
    }

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

  private void setPluginDependencies(@NotNull Element rootElement) {
    final List dependsElements = rootElement.getChildren("depends");

    for (Object dependsObj : dependsElements) {
      Element dependsElement = (Element) dependsObj;

      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      if (pluginId == null) {
        throw new IncorrectPluginException("Invalid plugin.xml: invalid dependency tag");
      }

      PluginDependency dependency = new PluginDependency(pluginId, optional);
      if (pluginId.startsWith(INTELLIJ_MODULES_PREFIX)) {
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
  public String getVendor() {
    return myPluginVendor;
  }

  @Override
  @NotNull
  public Set<String> getDefinedModules() {
    return Collections.unmodifiableSet(myDefinedModules);
  }

  private void setDefinedModules(@NotNull Document pluginXml) {
    @SuppressWarnings("unchecked")
    Iterable<? extends Element> children = (Iterable<? extends Element>) pluginXml.getRootElement().getChildren("module");
    for (Element module : children) {
      myDefinedModules.add(module.getAttributeValue("value"));
    }
  }

  private void setSinceUntilBuilds(@NotNull Element ideaVersion) throws IncorrectPluginException {
    if (ideaVersion.getAttributeValue("min") == null) { // min != null in legacy plugins.
      String sb = ideaVersion.getAttributeValue("since-build");
      try {
        mySinceBuild = IdeVersion.createIdeVersion(sb);
      } catch (IllegalArgumentException e) {
        throw new IncorrectPluginException("<idea version since-build = /> attribute has incorrect value: " + sb);
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
          throw new IncorrectPluginException("<idea-version until-build= /> attribute has incorrect value: " + ub);
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
  public String getDescription() {
    return myDescription;
  }

  @Nullable
  @Override
  public InputStream getResourceFile(@NotNull String relativePath) {
    relativePath = StringUtil.trimStart(relativePath, "/");
    URL url;
    try {
      url = new URL(myMainJarUrl.toExternalForm() + relativePath);
    } catch (MalformedURLException e) {
      throw new IncorrectPluginException("File path is invalid " + relativePath);
    }
    try {
      return URLUtil.openStream(url);
    } catch (IOException e) {
      //consider it doesn't exist
      return null;
    }
  }

  @Override
  public String getVendorEmail() {
    return myVendorEmail;
  }

  @Override
  public String getVendorUrl() {
    return myVendorUrl;
  }

  @Override
  public String getVendorLogoPath() {
    return myVendorLogoPath;
  }

  @Override
  public String getUrl() {
    return myUrl;
  }

  @Override
  public String getNotes() {
    return myNotes;
  }
}
