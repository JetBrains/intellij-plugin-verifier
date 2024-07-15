package mock.plugin.overrideOnly;

@SuppressWarnings("unused")
public class PackageInvokingBox {
    /*expected(DEPRECATED)
    Deprecated method java.lang.Package.getPackage(String) invocation

    Deprecated method java.lang.Package.getPackage(java.lang.String name) : java.lang.Package is invoked in mock.plugin.overrideOnly.PackageInvokingBox.getPackage(String) : Package
    */
    @SuppressWarnings("deprecation")
    public Package getPackage(String pkg) {
        return Package.getPackage(pkg);
    }
}
