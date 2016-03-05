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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

class PluginImpl implements Plugin {

  private static final Whitelist WHITELIST = Whitelist.basicWithImages();
  private static final String INTELLIJ_MODULES_PREFIX = "com.intellij.modules";
  private final Set<String> myDefinedModules = new HashSet<String>();
  private final List<PluginDependency> myDependencies = new ArrayList<PluginDependency>();
  private final List<PluginDependency> myModuleDependencies = new ArrayList<PluginDependency>();
  private File myPluginPath;
  private Resolver myPluginResolver;
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
  private String myResourceBundleBaseName;
  private Map<String, String> myOptionalConfigs;
  private Map<String, PluginImpl> myOptionalDescriptors;


  PluginImpl(@NotNull File pluginPath) throws IncorrectPluginException {
    myPluginPath = pluginPath;
  }


  private void setEntries(@Nullable Element rootElement) {
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
    final List dependsElements = rootElement.getChildren("depends");

    for (Object dependsObj : dependsElements) {
      Element dependsElement = (Element) dependsObj;

      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      if (pluginId == null) {
        throw new IncorrectPluginException("Invalid plugin.xml: invalid dependency tag");
      }

      PluginDependency dependency = new PluginDependencyImpl(pluginId, optional);
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
    @SuppressWarnings("unchecked")
    Iterable<? extends Element> children = rootElement.getChildren("module");
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
    return myPluginResolver;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public String getVendorEmail() {
    return myVendorEmail;
  }

  @Override
  public String getVendorUrl() {
    return myVendorUrl;
  }

  @Nullable
  @Override
  public String getResourceBundleBaseName() {
    //TODO: implement
    return myResourceBundleBaseName;
  }

  @Nullable
  @Override
  public InputStream getVendorLogo() {
    //TODO: implement
    return null;
  }


  @Override
  public String getUrl() {
    return myUrl;
  }

  @Override
  public String getChangeNotes() {
    return myNotes;
  }

  @NotNull
  @Override
  public File getPluginPath() {
    return myPluginPath;
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
      throw new IncorrectPluginException("Unable to read " + url, e);
    }
    setEntries(document.getRootElement());
  }

  void setPath(File path) {
    myPluginPath = path;
  }

  Map<String, String> getOptionalConfigs() {
    return myOptionalConfigs;
  }

  public void setOptionalDescriptors(Map<String, PluginImpl> optionalDescriptors) {
    myOptionalDescriptors = optionalDescriptors;
  }


    /*@Nullable
  private PluginImpl loadDescriptorFromZip(@NotNull File rootZip, @NotNull ZipFile zipFile, @NotNull String fileName) {

    List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
    Map<String, ZipEntry> entriesMap = fillEntriesMap(entries);

    List<ZipEntry> rootDirs = filterEntries(entries, new ZipEntryFilter() {
      @Override
      public boolean accept(ZipEntry entry) {
        return StringUtil.countChars(entry.getName(), '/') == 1;
      }
    });

    //find Sample.zip/Sample/META-INF/plugin.xml if exists
    for (ZipEntry rootDir : rootDirs) {
      final String path = rootDir.getName() + META_INF + fileName;

      if (entriesMap.containsKey(path)) {
        URL url;
        try {
          url = new URL("jar:" + StringUtil.replace(rootZip.toURI().toASCIIString(), "!", "%21") + "!" + path);
        } catch (MalformedURLException e) {
          throw new IncorrectPluginException("Unable to read " + path);
        }

        ZipEntry entry = entriesMap.get(path);
        Document document;
        try {
          document = JDOMUtil.loadDocument(zipFile.getInputStream(entry));
        } catch (JDOMException e) {
          throw new IncorrectPluginException("Unable to read file " + path, e);
        } catch (IOException e) {
          throw new IncorrectPluginException("Unable to read file " + path, e);
        }

        PluginImpl descriptor = new PluginImpl(rootZip);
        descriptor.readExternal(document, url);
        return descriptor;
      }
    }

    //find Sample/lib/Sample.jar/META-INF/plugin.xml if exists
    String lib = null;
    for (ZipEntry rootDir : rootDirs) {
      lib = rootDir.getName() + "lib/";
      if (entriesMap.containsKey(lib)) {
        break;
      }
    }

    if (lib == null) {
      throw new IncorrectPluginException("Plugin `lib` directory is not found");
    }

    final String libDirPath = lib;

    List<ZipEntry> libSubdir = filterEntries(entries, new ZipEntryFilter() {
      @Override
      public boolean accept(ZipEntry entry) {
        if (entry.getName().equals(libDirPath)) {
          //lib dir itself
          return false;
        }
        if (entry.getName().startsWith(libDirPath)) {
          int slashes = StringUtil.countChars(entry.getName(), '/', libDirPath.length(), false);

          //it is a directory or a file directly under the ../lib/
          //e.g. "../lib/a.jar" or "../lib/b/" but not "../lib/c/d/e.jar"
          return slashes <= 1;
        }
        return false;
      }
    });

    if (libSubdir.isEmpty()) {
      throw new IncorrectPluginException("Plugin `lib` directory is empty");
    }

    //move plugin-jar to the beginning: Sample.jar goes first (if Sample is a plugin name)
    final String pluginName = StringUtil.trimEnd(libDirPath, "/lib");
    Collections.sort(libSubdir, new Comparator<ZipEntry>() {
      @Override
      public int compare(@NotNull ZipEntry o1, @NotNull ZipEntry o2) {
        String o1n = StringUtil.trimStart(o1.getName(), libDirPath);
        String o2n = StringUtil.trimStart(o2.getName(), libDirPath);
        if (o2n.startsWith(pluginName)) return Integer.MAX_VALUE;
        if (o1n.startsWith(pluginName)) return -Integer.MAX_VALUE;
        if (o2n.startsWith("resources")) return -Integer.MAX_VALUE;
        if (o1n.startsWith("resources")) return Integer.MAX_VALUE;
        return 0;
      }
    });

    PluginImpl descriptor = null;
    for (ZipEntry entry : libSubdir) {
      String name = entry.getName();
      boolean isJarOrZip = StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
      boolean isDirectory = name.endsWith("/");
      if (isJarOrZip) {
        descriptor = loadDescriptorFromZip()
      }
    }

    return null;
    TODO:
  }*/

}
