package mock.plugin.deprecated;

import deprecated.ScheduledForRemovalClass;

/*expected(DEPRECATED)
  Deprecated class deprecated.ScheduledForRemovalClass reference

  Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalClassInheritor.<init>(). This class will be removed in 2018.1
*/

/*expected(DEPRECATED)
  Deprecated class deprecated.ScheduledForRemovalClass reference

  Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalClassInheritor. This class will be removed in 2018.1
*/
public class ScheduledForRemovalClassInheritor extends ScheduledForRemovalClass {
}
