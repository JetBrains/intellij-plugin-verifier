package mock.plugin.bundle;

import bundle.IdeBundle;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BundleUser {
  public void existingProp() {
    IdeBundle.getMessage("existing.property");
  }

  public void removedProp() {
    /*expected(PROBLEM)
    Reference to a missing property removed.property of resource bundle messages.IdeBundle

    Method mock.plugin.bundle.BundleUser.removedProp() : void references property removed.property that is not found in resource bundle messages.IdeBundle. This can lead to **MissingResourceException** exception at runtime.
    */
    IdeBundle.getMessage("removed.property");
  }

  public void deprecatedProp() {
    // The "deprecated.property" used to be declared in the
    // IdeBundle.properties but now it is moved to the CoreDeprecatedMessagesBundle.properties

    /*expected(WARNING)
    Reference to a deprecated property deprecated.property of resource bundle messages.IdeBundle, which was moved to messages.CoreDeprecatedMessagesBundle

    Method mock.plugin.bundle.BundleUser.deprecatedProp() : void references deprecated property deprecated.property that was moved from the resource bundle messages.IdeBundle to messages.CoreDeprecatedMessagesBundle. The clients will continue to get the correct value of the property but they are encouraged to place the property to their own resource bundle
    */

    //noinspection UnresolvedPropertyKey
    IdeBundle.getMessage("deprecated.property");
  }

}
