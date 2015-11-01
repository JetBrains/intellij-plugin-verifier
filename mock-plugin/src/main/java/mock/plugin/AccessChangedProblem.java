package mock.plugin;

import com.intellij.openapi.diagnostic.LogUtil;

/**
 * @author Sergey Patrikeev
 */
public class AccessChangedProblem {
  public void foo() {
    LogUtil logUtil = new LogUtil(); //constructor in IDEA 141 is not available (private)
  }
}
