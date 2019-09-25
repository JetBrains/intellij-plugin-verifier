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

}
