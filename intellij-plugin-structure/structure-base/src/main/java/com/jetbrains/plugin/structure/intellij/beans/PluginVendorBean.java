package com.jetbrains.plugin.structure.intellij.beans;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class PluginVendorBean {
  @XmlAttribute(name = "url") public String url = "";
  @XmlAttribute(name = "email") public String email = "";
  @XmlAttribute(name = "logo") public String logo;
  @XmlValue public String name;
}

