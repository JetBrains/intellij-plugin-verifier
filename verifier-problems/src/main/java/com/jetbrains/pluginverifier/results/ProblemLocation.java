package com.jetbrains.pluginverifier.results;

import com.jetbrains.pluginverifier.utils.Assert;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.objectweb.asm.tree.MethodNode;

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
    //required empty-constructor for XML processing
  }

  private ProblemLocation(@NotNull String className, @Nullable String methodDescr, @Nullable String fieldName) {
    Assert.assertTrue(methodDescr == null || !methodDescr.contains("#"), "Message descriptor " + methodDescr + " is malformed");
    this.className = className;
    this.methodDescr = methodDescr;
    this.fieldName = fieldName;
  }

  @NotNull
  public static ProblemLocation fromClass(@NotNull String className) {
    return new ProblemLocation(className, null, null);
  }

  @NotNull
  public static ProblemLocation fromField(@NotNull String className, @NotNull String fieldName) {
    return new ProblemLocation(className, null, fieldName);
  }

  @NotNull
  @TestOnly
  public static ProblemLocation fromMethod(@NotNull String className, @NotNull String methodDescr) {
    return new ProblemLocation(className, methodDescr, null);
  }

  @NotNull
  public static ProblemLocation fromMethod(@NotNull String className, @NotNull MethodNode methodNode) {
    return new ProblemLocation(className, getMethodDescr(methodNode), null);
  }

  @NotNull
  private static String getMethodDescr(@NotNull MethodNode methodNode) {
    Assert.assertTrue(methodNode.name != null);
    Assert.assertTrue(methodNode.desc != null);
    return methodNode.name + methodNode.desc;
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
