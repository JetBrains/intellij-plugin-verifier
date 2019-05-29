package mock.plugin.discouraging;

//IDEA-205992: an inspection discouraging use of certain JDK classes
//MP-2043: detect usages of discouraging JDK classes in plugins
public class DiscouragingJdkClassUser {

  /*expected(DEPRECATED)
  Usage of JDK 8 specific class javax.activity.ActivityCompletedException

  JDK 8 specific class javax.activity.ActivityCompletedException is referenced in mock.plugin.discouraging.DiscouragingJdkClassUser.missingInIdeAndNewJdk(ActivityCompletedException) : void. This class is neither available in JDK 9+ nor is it available in IDE distribution. This may lead to compatibility problems when running the IDE with newer JDK versions.
  */
  //This class is neither available in newer JDK nor is it included into IDE
  public void missingInIdeAndNewJdk(javax.activity.ActivityCompletedException exception) {
  }
}
