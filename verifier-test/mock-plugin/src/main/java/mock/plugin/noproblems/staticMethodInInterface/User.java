package mock.plugin.noproblems.staticMethodInInterface;

import statics.NoProblems;

public class User {
  public void foo() {
    NoProblems.staticFoo();
  }
}
