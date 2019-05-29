package mock.plugin.removedClasses;

import removedClasses.removedWholePackage.Removed5;

/*expected(PROBLEM)
Package 'removedClasses.removedWholePackage' is not found

Package 'removedClasses.removedWholePackage' is not found along with its 8 classes.
Probably the package 'removedClasses.removedWholePackage' belongs to a library or dependency that is not resolved by the checker.
It is also possible, however, that this package was actually removed from a dependency causing the detected problems. Access to unresolved classes at runtime may lead to **NoSuchClassError**.
The following classes of 'removedClasses.removedWholePackage' are not resolved (only 5 most used classes are shown, 3 hidden):
  Class removedClasses.removedWholePackage.Removed5 is referenced in
    mock.plugin.removedClasses.User5
    mock.plugin.removedClasses.User5.<init>()
    mock.plugin.removedClasses.User5.usage1() : void
    mock.plugin.removedClasses.User5.usage2() : void
    mock.plugin.removedClasses.User5.usage3() : void
    ...and 5 other places...
  Class removedClasses.removedWholePackage.Removed1 is referenced in
    mock.plugin.removedClasses.User1
    mock.plugin.removedClasses.User1.<init>()
    mock.plugin.removedClasses.User1.usage1() : void
    mock.plugin.removedClasses.User1.usage2() : void
    mock.plugin.removedClasses.User1.usage3() : void
  Class removedClasses.removedWholePackage.Removed2 is referenced in
    mock.plugin.removedClasses.User2
    mock.plugin.removedClasses.User2.<init>()
  Class removedClasses.removedWholePackage.Removed3 is referenced in
    mock.plugin.removedClasses.User3
    mock.plugin.removedClasses.User3.<init>()
  Class removedClasses.removedWholePackage.Removed4 is referenced in
    mock.plugin.removedClasses.User4
    mock.plugin.removedClasses.User4.<init>()

*/

public class User5 extends Removed5 {
  void usage1() {
    new Removed5();
  }

  void usage2() {
    new Removed5();
  }

  void usage3() {
    new Removed5();
  }

  void usage4() {
    new Removed5();
  }

  void usage5() {
    new Removed5();
  }

  void usage6() {
    new Removed5();
  }

  void usage7() {
    new Removed5();
  }

  void usage8() {
    new Removed5();
  }
}
