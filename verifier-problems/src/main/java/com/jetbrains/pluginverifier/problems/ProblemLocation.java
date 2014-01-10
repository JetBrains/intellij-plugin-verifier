package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Describe problem location inside plugin
 *
 * @author Sergey Evdokimov
 */
@XmlRootElement
public class ProblemLocation {

  private String className;

  private String methodDescr;

  private String fieldName;

  public ProblemLocation() {

  }

  public ProblemLocation(@NotNull String className) {
    this(className, null);
  }

  public ProblemLocation(@NotNull String className, @Nullable String methodDescr) {
    this.className = className;
    this.methodDescr = methodDescr;

    assert methodDescr == null || !methodDescr.contains("#");
  }

  @Nullable
  public String getMethodDescr() {
    return methodDescr;
  }

  public void setMethodDescr(String methodDescr) {
    this.methodDescr = methodDescr;
  }

  @Nullable
  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public String toString() {
    if (className == null) {
      return "";
    }

    if (methodDescr == null) {
      if (fieldName != null) {
        return MessageUtils.convertClassName(className) + '.' + fieldName;
      }

      return MessageUtils.convertClassName(className);
    }

    return MessageUtils.convertClassName(className) + '#' + methodDescr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProblemLocation location = (ProblemLocation)o;

    if (className != null ? !className.equals(location.className) : location.className != null) return false;
    if (fieldName != null ? !fieldName.equals(location.fieldName) : location.fieldName != null) return false;
    if (methodDescr != null ? !methodDescr.equals(location.methodDescr) : location.methodDescr != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (methodDescr != null ? methodDescr.hashCode() : 0);
    result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
    return result;
  }
}
