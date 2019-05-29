package mock.plugin.news;

import misc.BecomeAbstract;
import misc.BecomeInterface;


public class NewProblems {
  public void abstractClass() {

  /*expected(PROBLEM)
  Instantiation of an abstract class misc.BecomeAbstract

  Method mock.plugin.news.NewProblems.abstractClass() : void has instantiation *new* instruction referencing an abstract class misc.BecomeAbstract. This can lead to **InstantiationError** exception at runtime.
  */
    new BecomeAbstract();
  }

  /*expected(PROBLEM)
  Instantiation of an interface misc.BecomeInterface

  Method mock.plugin.news.NewProblems.newInterface() : void has instantiation *new* instruction referencing an interface misc.BecomeInterface. This can lead to **InstantiationError** exception at runtime.
  */

  /*expected(PROBLEM)
  Invocation of unresolved constructor misc.BecomeInterface.<init>()

  Method mock.plugin.news.NewProblems.newInterface() : void contains an *invokespecial* instruction referencing an unresolved constructor misc.BecomeInterface.<init>(). This can lead to **NoSuchMethodError** exception at runtime.
  */
  public void newInterface() {
    new BecomeInterface();
  }
}
