package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;

public class ListItemBean {
  @XmlAttribute(name = "value") String value;
}