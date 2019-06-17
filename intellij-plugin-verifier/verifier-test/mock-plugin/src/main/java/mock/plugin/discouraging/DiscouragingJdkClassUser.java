package mock.plugin.discouraging;

import discouragingJdkClass.DiscouragingJdkClass;

//IDEA-205992: an inspection discouraging use of certain JDK classes
//MP-2043: detect usages of discouraging JDK classes in plugins
public class DiscouragingJdkClassUser {

  /*expected(DEPRECATED)
  Usage of JDK 8 specific class discouragingJdkClass.DiscouragingJdkClass

  JDK 8 specific class discouragingJdkClass.DiscouragingJdkClass is referenced in mock.plugin.discouraging.DiscouragingJdkClassUser.missingInIdeAndNewJdk(DiscouragingJdkClass) : void. This class will be temporarily available in IDE distribution for compatibility but you should use another API or provide your own dependency containing the classes.
  */
  //This class is neither available in newer JDK nor is it included into IDE
  public void missingInIdeAndNewJdk(DiscouragingJdkClass discouragingJdkClass) {
  }
}
