package com.jetbrains.pluginverifier.utils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "results")
public class ResultsElement {

  private String ide;

  private List<UpdateElement> update = new ArrayList<UpdateElement>();

  @XmlAttribute
  public String getIde() {
    return ide;
  }

  public void setIde(String ide) {
    this.ide = ide;
  }

  public List<UpdateElement> getUpdate() {
    return update;
  }

  public void setUpdate(List<UpdateElement> update) {
    this.update = update;
  }
}
