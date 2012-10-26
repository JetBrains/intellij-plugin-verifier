package com.jetbrains.pluginverifier.problems;

import com.jetbrains.pluginverifier.problems.ClassProblem;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InterfaceNotFoundProblem extends ClassProblem {

  private String myInterfaceName;

  public InterfaceNotFoundProblem() {

  }

  public InterfaceNotFoundProblem(@NotNull String className, @NotNull String interfaceName) {
    super(className);
    myInterfaceName = interfaceName;
  }

  @Override
  public String getDescription() {
    return "implementing unknown interface: " + MessageUtils.convertClassName(myInterfaceName) + " in class " + getClassNameHuman();
  }

  @NotNull
  @Override
  public String evaluateUID() {
    return evaluateUID(myInterfaceName, getClassName());
  }

  public String getInterfaceName() {
    return myInterfaceName;
  }

  public void setInterfaceName(String interfaceName) {
    myInterfaceName = interfaceName;
    cleanUid();
  }
}
