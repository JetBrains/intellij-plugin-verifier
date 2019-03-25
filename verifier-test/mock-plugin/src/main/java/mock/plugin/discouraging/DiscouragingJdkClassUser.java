package mock.plugin.discouraging;

//IDEA-205992: an inspection discouraging use of certain JDK classes
//MP-2043: detect usages of discouraging JDK classes in plugins
public class DiscouragingJdkClassUser {

  //This class is neither available in newer JDK nor is it included into IDE
  public void missingInIdeAndNewJdk(javax.activity.ActivityCompletedException exception) {
  }
}
