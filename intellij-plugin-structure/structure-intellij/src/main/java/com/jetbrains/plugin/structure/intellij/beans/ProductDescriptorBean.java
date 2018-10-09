package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;

public class ProductDescriptorBean {
  @XmlAttribute(name = "code") public String code;
  @XmlAttribute(name = "release-date") public String releaseDate;
  @XmlAttribute(name = "release-version") public String releaseVersion;
}
