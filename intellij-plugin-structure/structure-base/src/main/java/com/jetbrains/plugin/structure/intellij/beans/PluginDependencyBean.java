package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class PluginDependencyBean {
  @XmlAttribute(name = "optional") public boolean optional;
  @XmlAttribute(name = "config-file") public String configFile;
  @XmlValue public String pluginId;
}