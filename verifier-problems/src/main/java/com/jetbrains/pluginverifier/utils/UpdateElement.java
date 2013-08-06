package com.jetbrains.pluginverifier.utils;

import com.jetbrains.pluginverifier.problems.Problem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
@XmlRootElement(name = "update")
public class UpdateElement {

  private int id;

  private List<Problem> problem;

  @XmlAttribute
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @XmlElementRef
  public List<Problem> getProblem() {
    return problem;
  }

  public void setProblem(List<Problem> problem) {
    this.problem = problem;
  }
}
