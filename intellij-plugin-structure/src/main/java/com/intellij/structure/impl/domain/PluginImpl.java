package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jdom2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PluginImpl implements Plugin {

  private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
  private static final String INTERESTING_STRINGS[] = new String[]{"class", "interface", "implementation", "instance"};

  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";
  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles = new HashMap<PluginDependency, String>();
  private final Map<String, PluginImpl> myOptionalDescriptors = new HashMap<String, PluginImpl>();
  private final Set<String> myReferencedClasses = new HashSet<String>();
  @Nullable private byte[] myLogoContent;
  @Nullable private Resolver myPluginResolver;
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

  PluginImpl() throws IncorrectPluginException {
  }


  private void checkAndSetEntries(@NotNull URL url, @Nullable Element rootElement, boolean checkValidity) throws IncorrectPluginException {
    if (rootElement == null) {
      throw new IncorrectPluginException("Failed to parse plugin.xml: root element <idea-plugin> is not found");
    }

    if (!"idea-plugin".equals(rootElement.getName())) {
      throw new IncorrectPluginException("Invalid plugin.xml: root element must be <idea-plugin>, but it is " + rootElement.getName());
    }

    myPluginName = rootElement.getChildTextTrim("name");
    if (Strings.isNullOrEmpty(myPluginName)) {
      if (checkValidity) {
        throw new IncorrectPluginException("Invalid plugin.xml: 'name' is not specified");
      }
    }

    myPluginId = rootElement.getChildText("id");
    if (myPluginId == null) {
      myPluginId = myPluginName;
    }

    myUrl = StringUtil.notNullize(rootElement.getAttributeValue("url"));

    Element vendorElement = rootElement.getChild("vendor");
    if (vendorElement == null) {
      if (checkValidity) {
        throw new IncorrectPluginException("Invalid plugin.xml: element 'vendor' is not found");
      }
    } else {
      myPluginVendor = vendorElement.getTextTrim();
      myVendorEmail = StringUtil.notNullize(vendorElement.getAttributeValue("email"));
      myVendorUrl = StringUtil.notNullize(vendorElement.getAttributeValue("url"));
      setLogoContent(url, vendorElement);
    }

    myPluginVersion = rootElement.getChildTextTrim("version");
    if (myPluginVersion == null) {
      if (checkValidity) {
        throw new IncorrectPluginException("Invalid plugin.xml: version is not specified");
      }
    }

    Element ideaVersionElement = rootElement.getChild("idea-version");
    if (ideaVersionElement == null) {
      if (checkValidity) {
        throw new IncorrectPluginException("Invalid plugin.xml: element 'idea-version' not found");
      }
    } else {
      setSinceUntilBuilds(ideaVersionElement);
    }

    setComponents(rootElement);

    setPluginDependencies(rootElement);

    setDefinedModules(rootElement);

    String description = rootElement.getChildTextTrim("description");
    if (StringUtil.isNullOrEmpty(description)) {
      if (checkValidity) {
        throw new IncorrectPluginException("Invalid plugin.xml: description is empty");
      }
    } else {
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

  private void setLogoContent(@NotNull URL url, Element vendorElement) {
    String logoPath = vendorElement.getAttributeValue("logo");
    if (logoPath != null) {
      InputStream input = null;
      try {
        URL logoUrl = new URL(url, logoPath);
        input = URLUtil.openStream(logoUrl);
        myLogoContent = IOUtils.toByteArray(input);
      } catch (Exception ignored) {
      } finally {
        IOUtils.closeQuietly(input);
      }
    }
  }

  private void setComponents(@NotNull Element rootElement) {
    processReferencedClasses(rootElement);
    /*
    implement these if necessary
    setExtensions(rootElement);
    setExtensionPoints(rootElement);
    setActions(rootElement);
    setAppComponents(rootElement);
    setProjectComponents(rootElement);
    setModulesComponents(rootElement);
    */
  }

  private void processReferencedClasses(@NotNull Element rootElement) throws IncorrectPluginException {
    Iterator<Content> descendants = rootElement.getDescendants();
    while (descendants.hasNext()) {
      Content next = descendants.next();
      if (next instanceof Element) {
        Element element = (Element) next;

        if (isInterestingName(element.getName())) {
          checkIfClass(element.getTextNormalize());
        }

        for (Attribute attribute : element.getAttributes()) {
          if (isInterestingName(attribute.getName())) {
            checkIfClass(attribute.getValue().trim());
          }
        }
      }
      if (next instanceof Text) {
        Parent parent = next.getParent();
        if (parent instanceof Element) {
          if (isInterestingName(((Element) parent).getName())) {
            checkIfClass(((Text) next).getTextTrim());
          }
        }
      }
    }
  }

  private void checkIfClass(@NotNull String text) {
    Matcher matcher = JAVA_CLASS_PATTERN.matcher(text);
    while (matcher.find()) {
      myReferencedClasses.add(matcher.group().replace('.', '/'));
    }
  }

  private boolean isInterestingName(@NotNull String label) {
    for (String string : INTERESTING_STRINGS) {
      if (StringUtil.containsIgnoreCase(label, string)) {
        return true;
      }
    }
    return false;
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

  private void setPluginDependencies(@NotNull Element rootElement) throws IncorrectPluginException {
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
          myOptionalConfigFiles.put(dependency, configFile);
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

  @NotNull
  @Override
  public Set<String> getAllClassesReferencedFromXml() {
    Set<String> result = new HashSet<String>();
    result.addAll(myReferencedClasses);
    for (PluginImpl plugin : myOptionalDescriptors.values()) {
      result.addAll(plugin.myReferencedClasses);
    }
    return result;
  }

  @Override
  public byte[] getVendorLogo() {
    return myLogoContent;
  }


  void readExternal(@NotNull URL url, boolean checkValidity) throws IncorrectPluginException {
    try {
      Document document = JDOMUtil.loadDocument(url);
      readExternal(document, url, checkValidity);
    } catch (JDOMException e) {
      throw new IncorrectPluginException("Unable to read " + url.getFile(), e);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read " + url.getFile(), e);
    }
  }


  private void readExternal(@NotNull Document document, @NotNull URL url, boolean checkValidity) throws IncorrectPluginException {
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm());
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Unable to read resolve " + url.getFile(), e);
    }
    checkAndSetEntries(url, document.getRootElement(), checkValidity);
  }

  @NotNull
  Map<PluginDependency, String> getOptionalDependenciesConfigFiles() {
    return myOptionalConfigFiles;
  }

  /**
   * @param optionalDescriptors map of (optional file name) to (optional descriptor)
   */
  void setOptionalDescriptors(@NotNull Map<String, PluginImpl> optionalDescriptors) {
    myOptionalDescriptors.clear();
    myOptionalDescriptors.putAll(optionalDescriptors);
  }

  void setResolver(@NotNull Resolver resolver) {
    myPluginResolver = resolver;
  }
}
