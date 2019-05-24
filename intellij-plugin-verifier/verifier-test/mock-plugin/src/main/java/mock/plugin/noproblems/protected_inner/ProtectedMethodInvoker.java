package mock.plugin.noproblems.protected_inner;

import mock.plugin.noproblems.protected_inner.other.ProtectedMethodHolder;

public class ProtectedMethodInvoker extends ProtectedMethodHolder {

  public void bar() {
    foo();
    new Runnable() {
      @Override
      public void run() {
        foo();
      }
    };

  }

  private static class InnerStatic {
    public void baf() {
      new ProtectedMethodInvoker().foo();
    }
  }

  private class Inner {
    public void baz() {
      foo();
    }
  }

}
