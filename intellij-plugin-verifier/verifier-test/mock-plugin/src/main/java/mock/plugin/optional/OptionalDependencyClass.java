package mock.plugin.optional;

import invokevirtual.Parent;
import removedClasses.RemovedClass;

/*
This class belongs to optional plugin part, which is activated only when "MissingPlugin" is available (see plugin.xml).
 */

public class OptionalDependencyClass {

  /*
   * This problem will be ignored because `OptionalDependencyClass` is optional
   * and "class not found" problems are ignored when such classes are referenced from optional plugin's parts.
   */
  public void referenceMissingClass() {
    Class<RemovedClass> aClass = RemovedClass.class;
  }

  //This problem will not be ignored because only "class not found problems" are ignored for missing optional dependencies.
  /*expected(PROBLEM)
  Invocation of unresolved method invokevirtual.Parent.removedStaticMethod() : void

  Method mock.plugin.optional.OptionalDependencyClass.referenceMissingMethod() : void contains an *invokestatic* instruction referencing an unresolved method invokevirtual.Parent.removedStaticMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.
  */
  public void referenceMissingMethod() {
    Parent.removedStaticMethod();
  }
}
