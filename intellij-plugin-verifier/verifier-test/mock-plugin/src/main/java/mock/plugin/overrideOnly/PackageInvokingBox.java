package mock.plugin.overrideOnly;

public class PackageInvokingBox {
    @SuppressWarnings("deprecation")
    public Package getPackage(String pkg) {
        return Package.getPackage(pkg);
    }
}
