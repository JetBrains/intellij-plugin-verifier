package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.validators.Validator;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import org.apache.commons.io.IOUtils;
import org.jdom2.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import java.io.File;
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
  private static final Document EMPTY_DOCUMENT = new Document();
  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private final Map<PluginDependency, String> myOptionalConfigFiles = new HashMap<PluginDependency, String>();
  private final Map<String, Plugin> myOptionalDescriptors = new HashMap<String, Plugin>();
  private final Set<String> myReferencedClasses = new HashSet<String>();
  private final Multimap<String, Element> myExtensions = ArrayListMultimap.create();
  private File myPluginFile;
  @NotNull private Document myUnderlyingDocument = EMPTY_DOCUMENT;
  @NotNull private String myFileName = "(unknown)";
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

  PluginImpl(@NotNull File pluginFile) throws IncorrectPluginException {
    myPluginFile = pluginFile;
  }

  private static String extractEPName(final Element extensionElement) {
    String epName = extensionElement.getAttributeValue("point");

    if (epName == null) {
      final Element parentElement = extensionElement.getParentElement();
      final String ns = parentElement != null ? parentElement.getAttributeValue("defaultExtensionNs") : null;

      if (ns != null) {
        epName = ns + '.' + extensionElement.getName();
      } else {
        Namespace namespace = extensionElement.getNamespace();
        epName = namespace.getURI() + '.' + extensionElement.getName();
      }
    }
    return epName;
  }

  @Override
  @NotNull
  public Multimap<String, Element> getExtensions() {
    return Multimaps.unmodifiableMultimap(myExtensions);
  }

  private void setExtensions(Element rootElement) {
    for (Element extensionsRoot : rootElement.getChildren("extensions")) {
      for (Element element : extensionsRoot.getChildren()) {
        myExtensions.put(extractEPName(element), element);
      }
    }
  }

  private void checkAndSetEntries(@NotNull URL descriptorUrl, @NotNull Document document, @NotNull Validator validator) throws IncorrectPluginException {
    myUnderlyingDocument = document;
    myFileName = calcDescriptorName(descriptorUrl);

    Element rootElement = document.getRootElement();

    if (rootElement == null) {
      validator.onIncorrectStructure("Failed to parse " + myFileName + ": root element <idea-plugin> is not found");
      return;
    }

    if (!"idea-plugin".equals(rootElement.getName())) {
      validator.onIncorrectStructure("Invalid " + myFileName + ": root element must be <idea-plugin>, but it is " + rootElement.getName());
      return;
    }

    myPluginName = rootElement.getChildTextTrim("name");
    if (Strings.isNullOrEmpty(myPluginName)) {
      validator.onMissingConfigElement("Invalid " + myFileName + ": 'name' is not specified");
    }

    myPluginId = rootElement.getChildText("id");
    if (myPluginId == null) {
      myPluginId = myPluginName;
    }

    myUrl = StringUtil.notNullize(rootElement.getAttributeValue("url"));

    Element vendorElement = rootElement.getChild("vendor");
    if (vendorElement == null) {
      validator.onMissingConfigElement("Invalid " + myFileName + ": element 'vendor' is not found");
    } else {
      myPluginVendor = vendorElement.getTextTrim();
      myVendorEmail = StringUtil.notNullize(vendorElement.getAttributeValue("email"));
      myVendorUrl = StringUtil.notNullize(vendorElement.getAttributeValue("url"));
      setLogoContent(descriptorUrl, vendorElement);
    }

    myPluginVersion = rootElement.getChildTextTrim("version");
    if (myPluginVersion == null) {
      validator.onMissingConfigElement("Invalid " + myFileName + ": version is not specified");
    }

    Element ideaVersionElement = rootElement.getChild("idea-version");
    if (ideaVersionElement == null) {
      validator.onMissingConfigElement("Invalid " + myFileName + ": element 'idea-version' not found");
    } else {
      setSinceUntilBuilds(ideaVersionElement, validator);
    }

    setComponents(rootElement);

    setPluginDependencies(rootElement, validator);

    setDefinedModules(rootElement, validator);

    String description = rootElement.getChildTextTrim("description");
    if (StringUtil.isEmpty(description)) {
      validator.onMissingConfigElement("Invalid " + myFileName + ": description is empty");
    } else {
      myDescription = Jsoup.clean(description, WHITELIST);
    }

    List<Element> changeNotes = rootElement.getChildren("change-notes");
    if (changeNotes != null && changeNotes.size() > 0) {
      Element o = changeNotes.get(0);
      if (o != null) {
        String textTrim = o.getTextTrim();
        if (!StringUtil.isEmpty(textTrim)) {
          myNotes = Jsoup.clean(textTrim, WHITELIST);
        }
      }
    }
  }

  @NotNull
  private String calcDescriptorName(@NotNull URL url) {
    final String path = url.getFile();
    if (path.contains("META-INF/")) {
      return "META-INF/" + StringUtil.substringAfter(path, "META-INF/");
    }
    return path;
  }

  private void setLogoContent(@NotNull URL descriptorUrl, Element vendorElement) {
    myLogoUrl = vendorElement.getAttributeValue("logo");
    if (myLogoUrl != null && !myLogoUrl.startsWith("http://") && !myLogoUrl.startsWith("https://")) {
      //the logo url represents a path inside the plugin => try to extract logo content
      InputStream input = null;
      try {
        URL logoUrl;
        if (myLogoUrl.startsWith("/")) {
          //myLogoUrl represents a path from the <plugin_root> (where <plugin_root>/META-INF/plugin.xml)
          logoUrl = new URL(descriptorUrl, ".." + myLogoUrl);
        } else {
          //it's a META-INF/ relative path
          logoUrl = new URL(descriptorUrl, myLogoUrl);
        }
        try {
          input = URLUtil.openStream(logoUrl);
        } catch (Exception e) {
          //try the following logo path: <plugin_root>/classes/<logo_url>
          if (myLogoUrl.startsWith("/")) {
            logoUrl = new URL(descriptorUrl, "../classes" + myLogoUrl);
            input = URLUtil.openStream(logoUrl);
          } else {
            //this path is unique => no other variants
            throw e;
          }
        }
        myLogoContent = IOUtils.toByteArray(input);
      } catch (Exception e) {
        System.err.println("Unable to extract plugin logo content by path " + myLogoUrl + " because " + e.getLocalizedMessage());
      } finally {
        IOUtils.closeQuietly(input);
      }
    }
  }

  private void setComponents(@NotNull Element rootElement) {
    processReferencedClasses(rootElement);
    setExtensions(rootElement);
    /*
    implement these if necessary
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

  private void setPluginDependencies(@NotNull Element rootElement, @NotNull Validator validator) throws IncorrectPluginException {
    final List<Element> dependsElements = rootElement.getChildren("depends");

    for (Element dependsElement : dependsElements) {
      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      if (pluginId == null) {
        validator.onIncorrectStructure("Invalid plugin.xml: invalid dependency tag " + dependsElement);
        continue;
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

  private void setDefinedModules(@NotNull Element rootElement, @NotNull Validator validator) {
    List<Element> children = rootElement.getChildren("module");
    for (Element module : children) {
      String value = module.getAttributeValue("value");
      if (value == null) {
        validator.onIncorrectStructure("Invalid <module> tag: value is not specified");
        continue;
      }
      myDefinedModules.add(value);
    }
  }

  private void setSinceUntilBuilds(@NotNull Element ideaVersion, @NotNull Validator validator) throws IncorrectPluginException {
    if (ideaVersion.getAttributeValue("min") == null) { // min != null in legacy plugins.
      String sb = ideaVersion.getAttributeValue("since-build");
      try {
        mySinceBuild = IdeVersion.createIdeVersion(sb);
      } catch (IllegalArgumentException e) {
        validator.onIncorrectStructure("'since-build' attribute in <idea-version> has incorrect value: " + sb +
            ". You can see specification of build numbers <a target='_blank' " +
            "href='http://confluence.jetbrains.com/display/IDEADEV/Build+Number+Ranges'>hire</a>");
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
          validator.onIncorrectStructure("<idea-version until-build= /> attribute has incorrect value: " + ub);
        }
      }
    }
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

  /**
   * @param optionalDescriptors map of (optional file name) to (optional descriptor)
   */
  void setOptionalDescriptors(@NotNull Map<String, PluginImpl> optionalDescriptors) {
    myOptionalDescriptors.clear();
    myOptionalDescriptors.putAll(optionalDescriptors);
    for (PluginImpl optDescriptor : optionalDescriptors.values()) {
      mergeOptionalConfig(optDescriptor);
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

  @NotNull
  @Override
  public File getPluginFile() {
    return myPluginFile;
  }

  void readExternalFromIdeSources(@NotNull URL url, @NotNull Validator validator, @NotNull JDOMXIncluder.PathResolver pathResolver) throws IncorrectPluginException {
    Document document;
    try {
      document = JDOMUtil.loadDocument(url);
    } catch (Exception e) {
      validator.onCheckedException("Unable to read XML document by url " + url.toExternalForm(), e);
      return;
    }
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm(), false, pathResolver);
    } catch (Exception e) {
      System.err.println("Unable to resolve xinclude elements");
      e.printStackTrace();
    }
    checkAndSetEntries(url, document, validator);
  }

  void readExternal(@NotNull URL url, @NotNull Validator validator) throws IncorrectPluginException {
    try {
      Document document = JDOMUtil.loadDocument(url);
      readExternal(document, url, validator);
    } catch (Exception e) {
      validator.onCheckedException("Unable to read " + url, e);
    }
  }

  void readExternal(@NotNull Document document, @NotNull URL url, Validator validator) throws IncorrectPluginException {
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm());
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Unable to read resolve " + url.getFile(), e);
    }
    checkAndSetEntries(url, document, validator);
  }

  @NotNull
  Map<PluginDependency, String> getOptionalDependenciesConfigFiles() {
    return Collections.unmodifiableMap(myOptionalConfigFiles);
  }

  private void mergeOptionalConfig(@NotNull PluginImpl optDescriptor) {
    myExtensions.putAll(optDescriptor.getExtensions());
  }

  @Override
  public String toString() {
    String id = myPluginId;
    if (StringUtil.isEmpty(id)) {
      id = myPluginName;
    }
    if (StringUtil.isEmpty(id)) {
      id = myUrl;
    }
    if (StringUtil.isEmpty(id)) {
      id = myFileName;
    }
    return id + (getPluginVersion() != null ? ":" + getPluginVersion() : "");
  }
}
