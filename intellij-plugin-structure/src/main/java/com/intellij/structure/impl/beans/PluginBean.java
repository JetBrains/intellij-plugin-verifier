package com.intellij.structure.impl.beans;

import com.google.common.collect.Multimap;
import org.jdom2.Element;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlSeeAlso(ListItemBean.class)
@XmlRootElement(name = "idea-plugin")
public class PluginBean {
  @XmlElement(name = "name") public String name;
  @XmlElement(name = "id") public String id;
  @XmlElement(name = "description") public String description;
  @XmlAttribute(name = "version") public String formatVersion;
  @XmlElement(name = "version") public String pluginVersion;
  @XmlElement(name = "vendor") public PluginVendorBean vendor;
  @XmlElement(name = "idea-version") public IdeaVersionBean ideaVersion;
  @XmlElement(name = "is-internal") public boolean isInternal = true;
  @XmlElement(name = "depends") public List<PluginDependencyBean> dependencies = new ArrayList<PluginDependencyBean>();
  @XmlElement(name = "helpset") public List<PluginHelpSetBean> helpSets = new ArrayList<PluginHelpSetBean>();
  @XmlElement(name = "category") public String category;
  @XmlElement(name = "resource-bundle") public String resourceBundle;
  @XmlElement(name = "change-notes") public String changeNotes;
  @XmlAttribute(name = "url") public String url = "";
  @XmlAttribute(name = "use-idea-classloader") public boolean useIdeaClassLoader;
  @XmlAttribute(name = "allow-bundled-update") public boolean allowBundledUpdate;

  @XmlElement(name = "module")
  @XmlJavaTypeAdapter(ListItemAdapter.class)
  public List<String> modules = new ArrayList<String>();

  @XmlTransient public Multimap<String, Element> extensions;
  @XmlTransient public List<String> classes;
}
