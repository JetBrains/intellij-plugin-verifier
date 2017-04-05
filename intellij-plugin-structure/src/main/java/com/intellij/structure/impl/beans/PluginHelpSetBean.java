package com.intellij.structure.impl.beans;

import javax.xml.bind.annotation.XmlAttribute;

public class PluginHelpSetBean {
  @XmlAttribute(name = "file") public String file;
  @XmlAttribute(name = "path") public String path;
}
