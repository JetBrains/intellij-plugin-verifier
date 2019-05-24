package mock.plugin.experimental;

import experimental.ExperimentalApiClass;
import experimental.ExperimentalApiField;
import experimental.ExperimentalApiMethod;

public class ExperimentalApiUser {
  public void field(ExperimentalApiField field) {
    int x = field.x;
  }

  public void method(ExperimentalApiMethod method) {
    method.foo(0);
  }

  public void clazz() {
    new ExperimentalApiClass();
  }

  /**
   * Verifier should report {@link ExperimentalApiClass} usage
   */
  public void staticFunOfDeprecatedClass() {
    ExperimentalApiClass.staticFun();
  }

  /**
   * Verifier should report {@link ExperimentalApiClass} usage
   */
  public void staticFieldOfDeprecatedClass() {
    ExperimentalApiClass.staticField = "";
  }


}
