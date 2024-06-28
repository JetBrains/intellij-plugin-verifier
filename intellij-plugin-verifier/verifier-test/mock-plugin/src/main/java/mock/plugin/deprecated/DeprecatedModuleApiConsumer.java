package mock.plugin.deprecated;

public class DeprecatedModuleApiConsumer {
    public void consume() {
        DeprecatedModuleApi api = new DeprecatedModuleApi();
        api.noOp();
    }
}
