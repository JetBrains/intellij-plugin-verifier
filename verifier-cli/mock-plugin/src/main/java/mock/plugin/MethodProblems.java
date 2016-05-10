package mock.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import non.existing.NonExistingClass;
import non.existing.NonExistingException;

/**
 * @author Sergey Patrikeev
 */
public class MethodProblems {
  public NonExistingClass brokenReturn() {
    return null;
  }

  public void brokenArg(NonExistingClass brokenArg) {

  }

  public void brokenLocalVar() {
    NonExistingClass brokenLocalVar = null;
  }

  public void brokenThrows() throws NonExistingException {

  }

  public void brokenCatch() {
    try {
      throw new NonExistingException();
    } catch (NonExistingException e) {
      e.printStackTrace();
    }
  }

  public void brokenDotClass() {
    Class<NonExistingClass> aClass = NonExistingClass.class;
  }

  public void brokenMultiArray() {
    NonExistingClass[][] brokenArray = new NonExistingClass[2][3];
  }

  public void brokenInvocation() {
    NonExistingClass.nonExistingMethod();
  }

  public void brokenNonFoundMethod() {
    AnAction.nonExistingMethod();
  }

  public void nonExistingInvocation() {
  }
}
