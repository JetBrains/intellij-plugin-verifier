package com.jetbrains.pluginverifier.location;

import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.List;

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

  public CodeLocation(@NotNull String className, @Nullable String methodDescr, @Nullable String fieldName) {
    FailUtil.assertTrue(methodDescr == null || !methodDescr.contains("#"), "Message descriptor " + methodDescr + " is malformed");
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
        return MessageUtils.convertClassName(className) + '.' + fieldName;
      }

      return MessageUtils.convertClassName(className);
    }

    return MessageUtils.convertMethodDescr(methodDescr, className);
  }

  @Override
  public List<Pair<String, String>> serialize() {
    //noinspection unchecked
    return Arrays.asList(Pair.create("class", className), Pair.create("method", methodDescr), Pair.create("field", fieldName));
  }

  @Override
  public ProblemLocation deserialize(String... params) {
    return new CodeLocation(params[0], params[1], params[2]);
  }
}
