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

  /*expected(PROBLEM)
  Access to unresolved class removedClasses.RemovedException

  Method mock.plugin.MethodProblems.brokenThrows() : void references an unresolved class removedClasses.RemovedException. This can lead to **NoSuchClassError** exception at runtime.
   */
  public void brokenThrows() throws RemovedException {
    throw new RemovedException();
  }

  /*expected(PROBLEM)
  Access to unresolved class removedClasses.RemovedException

  Method mock.plugin.MethodProblems.brokenCatch() : void references an unresolved class removedClasses.RemovedException. This can lead to **NoSuchClassError** exception at runtime.
  */
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

  /*expected(PROBLEM)
  Invocation of unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void

  Method mock.plugin.MethodProblems.brokenNonFoundMethod() : void contains an *invokestatic* instruction referencing an unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.
  */
  public void brokenNonFoundMethod() {
    AnAction.nonExistingMethod();
  }

  public void nonExistingInvocation() {
  }
}
