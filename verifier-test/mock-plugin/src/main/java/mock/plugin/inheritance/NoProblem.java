package mock.plugin.inheritance;

import inheritance.MultipleDefaultMethod1;
import inheritance.MultipleDefaultMethod2;

/**
 * Despite of inheriting the two default implementations of method foo() from two distinct interfaces
 * this class overrides the method foo() which makes it perfect to invoke the foo() on this class.
 * This is due to the JVM resolution strategy of invokeinterface methods:
 * it prefers class methods to interface default methods.
 */
public class NoProblem implements MultipleDefaultMethod1, MultipleDefaultMethod2 {
  public void foo() {

  }

}
