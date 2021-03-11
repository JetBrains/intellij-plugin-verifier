package mock.plugin.fusInternalApi;

import com.intellij.internal.statistic.DeviceIdManager;

public class ViolatingFusInternalApiUsage {
  /*expected(PROBLEM)
  Usage of FUS internal API: internal class com.intellij.internal.statistic.DeviceIdManager reference

  Usage of FUS internal API: internal class com.intellij.internal.statistic.DeviceIdManager is referenced in mock.plugin.fusInternalApi.ViolatingFusInternalApiUsage.foo() : String. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the class is not supposed to be used in client code.
  */

  /*expected(PROBLEM)
  Usage of FUS internal API: internal method com.intellij.internal.statistic.DeviceIdManager.getOrGenerateId() invocation

  Usage of FUS internal API: internal method com.intellij.internal.statistic.DeviceIdManager.getOrGenerateId() : java.lang.String is invoked in mock.plugin.fusInternalApi.ViolatingFusInternalApiUsage.foo() : String. This method is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation and indicates that the method is not supposed to be used in client code.
  */
  public String foo() {
    return DeviceIdManager.getOrGenerateId();
  }
}
