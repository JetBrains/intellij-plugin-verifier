package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.PluginDependency;
import com.intellij.structure.errors.IncorrectPluginException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.structure.impl.utils.StringUtil.*;

class PluginInfoExtractor {
  static final JDOMXIncluder.PathResolver DEFAULT_PLUGIN_XML_PATH_RESOLVER = new PluginXmlPathResolver();
  private static final Logger LOG = LoggerFactory.getLogger(PluginInfoExtractor.class);
  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules.";
  private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
  private static final String[] INTERESTING_STRINGS = new String[]{"class", "interface", "implementation", "instance"};

  private final PluginImpl myPlugin;
  private final Validator myValidator;

  PluginInfoExtractor(PluginImpl plugin, Validator validator) {
    this.myPlugin = plugin;
    this.myValidator = validator;
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

  void readExternalFromIdeSources(@NotNull URL url, @NotNull JDOMXIncluder.PathResolver pathResolver) throws IncorrectPluginException {
    Document document;
    try {
      document = JDOMUtil.loadDocument(url);
    } catch (Exception e) {
      myValidator.onCheckedException("Unable to read XML document by url " + url.toExternalForm(), e);
      return;
    }
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm(), false, pathResolver);
    } catch (Exception e) {
      LOG.error("Unable to resolve xinclude elements", e);
    }
    checkAndSetEntries(url, document);
  }

  void readExternal(@NotNull Document document, @NotNull URL documentUrl) {
    readExternal(document, documentUrl, DEFAULT_PLUGIN_XML_PATH_RESOLVER);
  }

  void readExternal(@NotNull Document document, @NotNull URL documentUrl, JDOMXIncluder.PathResolver pathResolver) {
    try {
      document = JDOMXIncluder.resolve(document, documentUrl.toExternalForm(), false, pathResolver);
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Unable to resolve xml include elements of " + documentUrl.getFile(), e);
    }
    checkAndSetEntries(documentUrl, document);
  }

  private void checkAndSetEntries(@NotNull URL descriptorUrl, @NotNull Document document) throws IncorrectPluginException {
    String fileName = calcDescriptorName(descriptorUrl);
    myPlugin.setUnderlyingDocument(document);
    myPlugin.setFileName(fileName);

    Element rootElement = document.getRootElement();

    if (rootElement == null) {
      myValidator.onIncorrectStructure("Failed to parse " + fileName + ": root element <idea-plugin> is not found");
      return;
    }

    if (!"idea-plugin".equals(rootElement.getName())) {
      myValidator.onIncorrectStructure("Invalid " + fileName + ": root element must be <idea-plugin>, but it is " + rootElement.getName());
      return;
    }

    String pluginName = rootElement.getChildTextTrim("name");
    if (Strings.isNullOrEmpty(pluginName)) {
      myValidator.onMissingConfigElement("Invalid " + fileName + ": 'name' is not specified");
    }
    myPlugin.setPluginName(pluginName);

    String pluginId = rootElement.getChildText("id");
    if (pluginId == null) {
      pluginId = pluginName;
    }
    myPlugin.setPluginId(pluginId);

    myPlugin.setUrl(notNullize(rootElement.getAttributeValue("url")));

    Element vendorElement = rootElement.getChild("vendor");
    if (vendorElement == null) {
      myValidator.onMissingConfigElement("Invalid " + fileName + ": element 'vendor' is not found");
    } else {
      myPlugin.setPluginVendor(vendorElement.getTextTrim());
      myPlugin.setVendorEmail(notNullize(vendorElement.getAttributeValue("email")));
      myPlugin.setVendorUrl(notNullize(vendorElement.getAttributeValue("url")));
      extractLogoContent(descriptorUrl, vendorElement);
    }

    String pluginVersion = rootElement.getChildTextTrim("version");
    myPlugin.setPluginVersion(pluginVersion);
    if (pluginVersion == null) {
      myValidator.onMissingConfigElement("Invalid " + fileName + ": version is not specified");
    }

    Element ideaVersionElement = rootElement.getChild("idea-version");
    if (ideaVersionElement == null) {
      myValidator.onMissingConfigElement("Invalid " + fileName + ": element 'idea-version' not found");
    } else {
      extractSinceUntilBuilds(ideaVersionElement);
    }

    extractComponents(rootElement);

    extractPluginDependencies(rootElement);
    extractDefinedModules(rootElement);
    extractDescription(rootElement);
    extractChangeNotes(rootElement);
  }

  private void extractComponents(@NotNull Element rootElement) {
    extractReferencedClasses(rootElement);
    extractExtensions(rootElement);
    /*
    implement these if necessary
    setExtensionPoints(rootElement);
    setActions(rootElement);
    setAppComponents(rootElement);
    setProjectComponents(rootElement);
    setModulesComponents(rootElement);
    */
  }

  private void extractLogoContent(@NotNull URL descriptorUrl, Element vendorElement) {
    String logoUrlString = vendorElement.getAttributeValue("logo");
    myPlugin.setLogoUrl(logoUrlString);
    if (logoUrlString != null && !logoUrlString.startsWith("http://") && !logoUrlString.startsWith("https://")) {
      //the logo url represents a path inside the plugin => try to extract logo content
      InputStream input = null;
      try {
        URL logoUrl;
        if (logoUrlString.startsWith("/")) {
          //myLogoUrl represents a path from the <plugin_root> (where <plugin_root>/META-INF/plugin.xml)
          logoUrl = new URL(descriptorUrl, ".." + logoUrlString);
        } else {
          //it's a META-INF/ relative path
          logoUrl = new URL(descriptorUrl, logoUrlString);
        }
        try {
          input = URLUtil.openStream(logoUrl);
        } catch (Exception e) {
          //try the following logo path: <plugin_root>/classes/<logo_url>
          if (logoUrlString.startsWith("/")) {
            logoUrl = new URL(descriptorUrl, "../classes" + logoUrlString);
            input = URLUtil.openStream(logoUrl);
          } else {
            //this path is unique => no other variants
            throw e;
          }
        }
        myPlugin.setLogoContent(IOUtils.toByteArray(input));
      } catch (Exception e) {
        String msg = "Unable to find plugin logo file by path " + logoUrlString + " specified in META-INF/plugin.xml";
        myValidator.onMissingLogo(msg);
        LOG.debug(msg, e);
      } finally {
        IOUtils.closeQuietly(input);
      }
    }
  }

  private void extractExtensions(Element rootElement) {
    for (Element extensionsRoot : rootElement.getChildren("extensions")) {
      for (Element element : extensionsRoot.getChildren()) {
        myPlugin.addExtension(extractEPName(element), element);
      }
    }
  }

  private void extractReferencedClasses(@NotNull Element rootElement) throws IncorrectPluginException {
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

  private boolean isInterestingName(@NotNull String label) {
    for (String string : INTERESTING_STRINGS) {
      if (containsIgnoreCase(label, string)) {
        return true;
      }
    }
    return false;
  }

  private void checkIfClass(@NotNull String text) {
    Matcher matcher = JAVA_CLASS_PATTERN.matcher(text);
    while (matcher.find()) {
      myPlugin.addReferencedClass(matcher.group().replace('.', '/'));
    }
  }

  private void extractSinceUntilBuilds(@NotNull Element ideaVersion) throws IncorrectPluginException {
    if (ideaVersion.getAttributeValue("min") == null) { // min != null in legacy plugins.
      String sb = ideaVersion.getAttributeValue("since-build");
      try {
        myPlugin.setSinceBuild(IdeVersion.createIdeVersion(sb));
      } catch (IllegalArgumentException e) {
        myValidator.onIncorrectStructure("'since-build' attribute in <idea-version> has incorrect value: " + sb +
            ". You can see specification of build numbers <a target='_blank' " +
            "href='http://confluence.jetbrains.com/display/IDEADEV/Build+Number+Ranges'>hire</a>");
      }

      String ub = ideaVersion.getAttributeValue("until-build");
      if (!Strings.isNullOrEmpty(ub)) {
        if (ub.endsWith(".*")) {
          int idx = ub.lastIndexOf('.');
          ub = ub.substring(0, idx + 1) + Integer.MAX_VALUE;
        }

        try {
          myPlugin.setUntilBuild(IdeVersion.createIdeVersion(ub));
        } catch (IllegalArgumentException e) {
          myValidator.onIncorrectStructure("<idea-version until-build= /> attribute has incorrect value: " + ub);
        }
      }
    }
  }

  private void extractPluginDependencies(@NotNull Element rootElement) throws IncorrectPluginException {
    final List<Element> dependsElements = rootElement.getChildren("depends");

    for (Element dependsElement : dependsElements) {
      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      if (pluginId == null) {
        myValidator.onIncorrectStructure("Invalid plugin.xml: invalid dependency tag " + dependsElement);
        continue;
      }

      PluginDependency dependency = new PluginDependencyImpl(pluginId, optional);
      if (pluginId.startsWith(INTELLIJ_MODULES_PREFIX)) {
        myPlugin.addModuleDependency(dependency);
      } else {
        myPlugin.addDependency(dependency);
      }

      if (optional) {
        String configFile = dependsElement.getAttributeValue("config-file");
        if (configFile != null) {
          myPlugin.addOptionalConfigFile(dependency, configFile);
        }
      }

    }
  }

  private void extractDefinedModules(@NotNull Element rootElement) {
    List<Element> children = rootElement.getChildren("module");
    for (Element module : children) {
      String value = module.getAttributeValue("value");
      if (value == null) {
        myValidator.onIncorrectStructure("Invalid <module> tag: value is not specified");
        continue;
      }
      myPlugin.addDefinedModule(value);
    }
  }

  private void extractDescription(@NotNull Element rootElement) {
    String description = rootElement.getChildTextTrim("description");
    if (isEmpty(description)) {
      myValidator.onMissingConfigElement("Invalid file: description is empty");
    } else {
      myPlugin.setDescription(Jsoup.clean(description, WHITELIST));
    }
  }

  private void extractChangeNotes(@NotNull Element rootElement) {
    List<Element> changeNotes = rootElement.getChildren("change-notes");
    if (changeNotes != null && changeNotes.size() > 0) {
      Element o = changeNotes.get(0);
      if (o != null) {
        String textTrim = o.getTextTrim();
        if (!isEmpty(textTrim)) {
          myPlugin.setNotes(Jsoup.clean(textTrim, WHITELIST));
        }
      }
    }
  }

  @NotNull
  private String calcDescriptorName(@NotNull URL url) {
    final String path = url.getFile();
    if (path.contains("META-INF/")) {
      return "META-INF/" + substringAfter(path, "META-INF/");
    }
    return path;
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
