package mock.plugin;

import com.intellij.openapi.diagnostic.LogUtil;

public class AccessChangedProblem {
  /*expected(PROBLEM)
  Illegal invocation of private constructor com.intellij.openapi.diagnostic.LogUtil.<init>()

  Method mock.plugin.AccessChangedProblem.foo() : void contains an *invokespecial* instruction referencing a private constructor com.intellij.openapi.diagnostic.LogUtil.<init>() inaccessible to a class mock.plugin.AccessChangedProblem. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void foo() {
    LogUtil logUtil = new LogUtil(); //constructor in IDEA 141 is not available (private)
  }
}
