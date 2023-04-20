package mock.plugin.invokeDynamic;

import java.util.List;

public class SomeClass {
    private List<String> list = List.of("a", "b", "c");

    private List<String> getList() {
        return list;
    }
}