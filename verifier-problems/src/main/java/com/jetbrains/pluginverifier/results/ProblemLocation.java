package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.utils.Assert;
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

    Assert.assertTrue(methodDescr == null || !methodDescr.contains("#"));
  }

  public static ProblemLocation fromField(@NotNull String className, @NotNull String fieldName) {
    ProblemLocation res = new ProblemLocation();
    res.setClassName(className);
    res.setFieldName(fieldName);
    return res;
  }

  public static ProblemLocation fromMethod(@NotNull String className, @NotNull String methodDescr) {
    ProblemLocation res = new ProblemLocation();
    res.setClassName(className);

    Assert.assertTrue(!methodDescr.contains("#"), methodDescr);
    res.setMethodDescr(methodDescr);

    return res;
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

    return MessageUtils.convertMethodDescr(methodDescr, className);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProblemLocation location = (ProblemLocation)o;

    if (className != null ? !className.equals(location.className) : location.className != null) return false;
    if (fieldName != null ? !fieldName.equals(location.fieldName) : location.fieldName != null) return false;
    return !(methodDescr != null ? !methodDescr.equals(location.methodDescr) : location.methodDescr != null);

  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + (methodDescr != null ? methodDescr.hashCode() : 0);
    result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
    return result;
  }
}
