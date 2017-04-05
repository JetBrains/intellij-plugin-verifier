package com.intellij.structure.impl.beans;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class PluginVendorBean {
  @XmlAttribute(name = "url") private String url;
  @XmlAttribute(name = "email") private String email;
  @XmlAttribute(name = "logo") private String logo;
  @XmlValue private String name;
}

