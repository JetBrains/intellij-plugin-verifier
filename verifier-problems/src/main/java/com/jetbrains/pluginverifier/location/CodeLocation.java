package com.jetbrains.pluginverifier.location;

import com.jetbrains.pluginverifier.utils.FailUtil;
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
public class CodeLocation extends ProblemLocation {

  private String className;

  private String methodDescr;

  private String fieldName;

  public CodeLocation() {
    //required empty-constructor for XML processing
  }

  CodeLocation(@NotNull String className, @Nullable String methodDescr, @Nullable String fieldName) {
    FailUtil.assertTrue(methodDescr == null || !methodDescr.contains("#"), "Message descriptor " + methodDescr + " is malformed");
    this.className = className;
    this.methodDescr = methodDescr;
    this.fieldName = fieldName;
  }

  @Nullable
  public String getMethodDescriptor() {
    return methodDescr;
  }

  public void setMethodDescriptor(@NotNull String methodDescr) {
    this.methodDescr = methodDescr;
  }

  @Nullable
  public String getClassName() {
    return className;
  }

  public void setClassName(@NotNull String className) {
    this.className = className;
  }

  @Nullable
  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(@NotNull String fieldName) {
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

    CodeLocation location = (CodeLocation) o;

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
