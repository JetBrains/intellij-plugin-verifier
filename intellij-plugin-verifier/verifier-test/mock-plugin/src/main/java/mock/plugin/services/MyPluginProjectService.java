package mock.plugin.services;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import services.IdeModuleService;
import services.IdeProjectService;

public class MyPluginProjectService {

  // Project injection is allowed, no warning here.
  public MyPluginProjectService(Project project) {
  }

  // Module injection is allowed, no warning here.
  public MyPluginProjectService(Module module) {
  }

  // Injection of other plugin's service is not allowed
  /*expected(WARNING)
  Constructor injection of services is deprecated

  Service mock.plugin.services.MyPluginProjectService2 is injected into constructor mock.plugin.services.MyPluginProjectService.<init>(MyPluginProjectService2 otherPluginService) of mock.plugin.services.MyPluginProjectService
  */
  public MyPluginProjectService(MyPluginProjectService2 otherPluginService) {
  }

  /*expected(WARNING)
  Constructor injection of services is deprecated

  Service services.IdeProjectService is injected into constructor mock.plugin.services.MyPluginProjectService.<init>(IdeProjectService ideProjectService) of mock.plugin.services.MyPluginProjectService
  */
  public MyPluginProjectService(IdeProjectService ideProjectService) {
  }

  /*expected(WARNING)
  Constructor injection of services is deprecated

  Service services.IdeModuleService is injected into constructor mock.plugin.services.MyPluginProjectService.<init>(IdeModuleService ideModuleService) of mock.plugin.services.MyPluginProjectService
  */
  public MyPluginProjectService(IdeModuleService ideModuleService) {
  }
}
