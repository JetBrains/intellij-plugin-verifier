package com.jetbrains.pluginverifier.location;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describe problem location inside plugin
 *
 * @author Sergey Evdokimov
 */
public class CodeLocation extends ProblemLocation {

  @SerializedName("class")
  private String className;

  @SerializedName("method")
  private String methodDescr;

  @SerializedName("field")
  private String fieldName;

  public CodeLocation() {
    //required empty-constructor for XML processing
  }

  public CodeLocation(@NotNull String className, @Nullable String methodDescr, @Nullable String fieldName) {
    Preconditions.checkArgument(methodDescr == null || !methodDescr.contains("#"), "Message descriptor " + methodDescr + " is malformed");
    if (className.contains(".")) {
      throw new IllegalArgumentException("Class name should be in binary form");
    }
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
  public String asString() {
    if (className == null) {
      return "";
    }

    if (methodDescr == null) {
      if (fieldName != null) {
        return MessageUtils.INSTANCE.convertClassName(className) + '.' + fieldName;
      }

      return MessageUtils.INSTANCE.convertClassName(className);
    }

    return MessageUtils.INSTANCE.convertMethodDescr(methodDescr, className);
  }
}
