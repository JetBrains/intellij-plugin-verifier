package com.jetbrains.pluginverifier.domain;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.resolvers.ClassPoolResolver;
import com.jetbrains.pluginverifier.resolvers.CombiningResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;
import com.jetbrains.pluginverifier.util.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class IdeaPlugin {
  public static final String PLUGIN_XML_ENTRY_NAME = "META-INF/plugin.xml";
  private final Idea myIdea;
  private final File myPluginDirectory;
  private final List<JarFile> myJarFiles;
  private final Document myPluginXml;
  private final ClassPool myClassPool;
  private final String myId;
  private Resolver myResolver;
  private final List<PluginDependency> myDependencies;

  public IdeaPlugin(Idea idea, File pluginDirectory) throws IOException, JDOMException {
    myIdea = idea;
    myPluginDirectory = pluginDirectory;

    myJarFiles = getPluginJars(pluginDirectory);
    myClassPool = Util.makeClassPool(pluginDirectory.getPath(), myJarFiles);

    myPluginXml = getPluginXml(myJarFiles);
    if (myPluginXml == null)
      throw new RuntimeException("No plugin.xml found for plugin " + pluginDirectory);

    myId = getPluginId(myPluginXml);
    myDependencies = getPluginDependencies(myPluginXml);
  }

  private static Document getPluginXml(List<JarFile> jarFiles) throws IOException, JDOMException {
    for (JarFile jarFile : jarFiles) {
      final ZipEntry pluginXmlEntry = jarFile.getEntry(PLUGIN_XML_ENTRY_NAME);
      if (pluginXmlEntry != null) {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(jarFile.getInputStream(pluginXmlEntry));
      }
    }

    return null;
  }

  private static String getPluginId(Document pluginXml) {
    final String id = pluginXml.getRootElement().getChildText("id");

    if (id == null || id.length() == 0) {
      final String name = pluginXml.getRootElement().getChildText("name");

      if (name == null || name.length() == 0)
        throw new RuntimeException("No id or name in plugin.xml");

      return name;
    }

    return id;
  }

  private static List<PluginDependency> getPluginDependencies(Document pluginXml) {
    final List dependsElements = pluginXml.getRootElement().getChildren("depends");

    List<PluginDependency> dependencies = new ArrayList<PluginDependency>();
    for (Object dependsObj : dependsElements) {
      Element dependsElement = (Element) dependsObj;

      final boolean optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      final String pluginId = dependsElement.getTextTrim();

      dependencies.add(new PluginDependency(pluginId, optional));
    }

    return dependencies;
  }

  private static List<JarFile> getPluginJars(File pluginDirectory) throws IOException {
    final File lib = new File(pluginDirectory, "lib");
    if (!lib.isDirectory())
      throw new RuntimeException("Plugin lib is not found: " + lib);

    final List<JarFile> jars = Util.getJars(lib);
    if (jars.size() == 0)
      throw new RuntimeException("No jar files found under " + lib);

    return jars;
  }

  public String getId() {
    return myId;
  }

  public Document getPluginXml() {
    return myPluginXml;
  }

  public List<PluginDependency> getDependencies() {
    return myDependencies;
  }

  public ClassPool getClassPool() {
    return myClassPool;
  }

  public Resolver getResolver() {
    if (myResolver == null) {
      List<Resolver> resolvers = new ArrayList<Resolver>();

      resolvers.add(new ClassPoolResolver(getClassPool()));
      resolvers.add(myIdea.getResolver());

      for (PluginDependency pluginDependency : getDependencies()) {
        final IdeaPlugin plugin = myIdea.getBundledPlugin(pluginDependency.getId());
        if (plugin != null)
          resolvers.add(plugin.getResolver());
      }

      myResolver = new CombiningResolver(resolvers);
    }

    return myResolver;
  }

  public Resolver getResolverOfDependecies() {
    List<Resolver> resolvers = new ArrayList<Resolver>();

    resolvers.add(myIdea.getResolver());

    for (PluginDependency pluginDependency : getDependencies()) {
      final IdeaPlugin plugin = myIdea.getBundledPlugin(pluginDependency.getId());
      if (plugin != null)
        resolvers.add(plugin.getResolver());
    }

    return new CombiningResolver(resolvers);
  }

  public Idea getIdea() {
    return myIdea;
  }
}

