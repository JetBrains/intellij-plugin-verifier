package mock.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import non.existing.NonExistingClass;
import removedClasses.RemovedException;

public class MethodProblems {
  public NonExistingClass brokenReturn() {
    return null;
  }

  public void brokenArg(NonExistingClass brokenArg) {

  }

  public void brokenLocalVar() {
    NonExistingClass brokenLocalVar = null;
  }

  public void brokenThrows() throws RemovedException {
    throw new RemovedException();
  }

  public void brokenCatch() {
    try {
      throw new RemovedException();
    } catch (RemovedException e) {
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
