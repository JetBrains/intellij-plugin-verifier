package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import com.intellij.structure.resolvers.Resolver;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.URL;
import java.util.*;

class PluginImpl implements Plugin {

  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";
  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private Map<PluginDependency, String> myOptionalConfigs = new HashMap<PluginDependency, String>();
  private Resolver myPluginResolver;
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
  private Map<String, Plugin> myOptionalDescriptors;


  PluginImpl() throws IncorrectPluginException {
  }


  private void checkAndSetInfo(@Nullable Element rootElement) throws IncorrectPluginException {
    if (rootElement == null) {
      throw new IncorrectPluginException("Failed to parse plugin.xml: root element <idea-plugin> is not found");
    }

    if (!"idea-plugin".equals(rootElement.getName())) {
      throw new IncorrectPluginException("Invalid plugin.xml: root element must be <idea-plugin>, but it is " + rootElement.getName());
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
      throw new IncorrectPluginException("Invalid plugin.xml: element 'vendor' is not found");
    }

    myPluginVendor = vendorElement.getTextTrim();
    myVendorEmail = StringUtil.notNullize(vendorElement.getAttributeValue("email"));
    myVendorUrl = StringUtil.notNullize(vendorElement.getAttributeValue("url"));
//    myVendorLogoPath = vendorElement.getAttributeValue("logo");

    myPluginVersion = rootElement.getChildTextTrim("version");
/*
    In idea bundled plugins it may be null (consider throwing an exception)
    if (myPluginVersion == null) {
      throw new IncorrectPluginException("Invalid plugin.xml: version is not specified");
    }
*/

    Element ideaVersionElement = rootElement.getChild("idea-version");
/*
    In idea bundled plugins it may be null
    if (ideaVersionElement == null) {
      throw new IncorrectPluginException("Invalid plugin.xml: element 'idea-version' not found");
    }
*/
    if (ideaVersionElement != null) {
      setSinceUntilBuilds(ideaVersionElement);
    }

    setPluginDependencies(rootElement);

    setDefinedModules(rootElement);


    String description = rootElement.getChildTextTrim("description");
//  In idea bundled plugins it may be null
//  throw new IncorrectPluginException("Invalid plugin.xml: description is empty");
    if (!StringUtil.isNullOrEmpty(description)) {
      myDescription = Jsoup.clean(description, WHITELIST);
    }

    List<Element> changeNotes = rootElement.getChildren("change-notes");
    if (changeNotes != null && changeNotes.size() > 0) {
      Element o = changeNotes.get(0);
      if (o != null) {
        String textTrim = o.getTextTrim();
        if (!StringUtil.isNullOrEmpty(textTrim)) {
          myNotes = Jsoup.clean(textTrim, WHITELIST);
        }
      }
    }

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
    final List<Element> dependsElements = rootElement.getChildren("depends");

    for (Element dependsElement : dependsElements) {
      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      if (pluginId == null) {
        throw new IncorrectPluginException("Invalid plugin.xml: invalid dependency tag " + dependsElement);
      }

      PluginDependency dependency = new PluginDependencyImpl(pluginId, optional);
      if (pluginId.startsWith(INTELLIJ_MODULES_PREFIX)) {
        myModuleDependencies.add(dependency);
      } else {
        myDependencies.add(dependency);
      }

      if (optional) {
        String configFile = dependsElement.getAttributeValue("config-file");
        if (configFile != null) {
          myOptionalConfigs.put(dependency, configFile);
        }
      }


    }
  }

  @Override
  public boolean isCompatibleWithIde(@NotNull IdeVersion ideVersion) {
    //noinspection SimplifiableIfStatement
    if (mySinceBuild == null) return true;

    return mySinceBuild.compareTo(ideVersion) <= 0 && (myUntilBuild == null || ideVersion.compareTo(myUntilBuild) <= 0);
  }

  @Override
  @NotNull
  public String getPluginName() {
    return myPluginName;
  }

  @Override
  @Nullable
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

  private void setDefinedModules(@NotNull Element rootElement) {
    List<Element> children = rootElement.getChildren("module");
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
  public Resolver getPluginResolver() {
    return myPluginResolver == null ? Resolver.getEmptyResolver() : myPluginResolver;
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


  void readExternal(@NotNull URL url) throws IncorrectPluginException {
    try {
      Document document = JDOMUtil.loadDocument(url);
      readExternal(document, url);
    } catch (JDOMException e) {
      throw new IncorrectPluginException("Unable to read " + url.getFile(), e);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read " + url.getFile(), e);
    }
  }


  void readExternal(@NotNull Document document, @NotNull URL url) throws IncorrectPluginException {
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm());
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Unable to read resolve " + url.getFile(), e);
    }
    checkAndSetInfo(document.getRootElement());
  }

/*
  @NotNull
  Map<PluginDependency, String> getOptionalDepConfigFiles() {
    return myOptionalConfigs;
  }

  void setOptionalDescriptors(@NotNull Map<String, PluginImpl> optionalDescriptors) {
    myOptionalDescriptors = optionalDescriptors;
  }
*/


  void setResolver(@NotNull Resolver resolver) {
    myPluginResolver = resolver;
  }
}
