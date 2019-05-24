package mock.plugin.removedClasses;

import removedClasses.removedWholePackage.Removed1;

public class User1 extends Removed1 {

  public User1() {
  }

  void usage1() {
    new Removed1();
  }

  void usage2() {
    new Removed1();
  }

  void usage3() {
    new Removed1();
  }
}
