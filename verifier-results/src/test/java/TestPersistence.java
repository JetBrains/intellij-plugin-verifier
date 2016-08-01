import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.CodeLocation;
import com.jetbrains.pluginverifier.location.PluginLocation;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.persistence.GsonHolder;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;

/**
 * @author Sergey Patrikeev
 */
public class TestPersistence {
  private static <T> void assertConversion(T input, Class<T> cls) {
    String json = GsonHolder.INSTANCE.getGSON().toJson(input);
    T t = GsonHolder.INSTANCE.getGSON().fromJson(json, cls);
    Assert.assertEquals(input, t);
  }

  @Test
  public void testUpdateInfo() throws Exception {
    assertConversion(new UpdateInfo("pluginId", "pluginName", "pluginVersion", 1), UpdateInfo.class);
  }

  @Test
  public void testIdeVersion() throws Exception {
    assertConversion(IdeVersion.createIdeVersion("IU-143"), IdeVersion.class);
  }

  @Test
  public void testMultimap() throws Exception {
    assertMultimapConversion(HashMultimap.<Integer, String>create());

    assertMultimapConversion(ImmutableMultimap.<IdeVersion, UpdateInfo>builder()
        .put(IdeVersion.createIdeVersion("1"), getUpdate(1))
        .put(IdeVersion.createIdeVersion("1"), getUpdate(2))
        .put(IdeVersion.createIdeVersion("2"), getUpdate(3))
        .put(IdeVersion.createIdeVersion("3"), getUpdate(4))
        .build());
  }

  @NotNull
  private UpdateInfo getUpdate(int i) {
    return new UpdateInfo("pluginId", "pluginName", "pluginVersion", i);
  }

  @Test
  public void testProblemLocation() throws Exception {
    assertConversion(new PluginLocation(""), ProblemLocation.class);
    assertConversion(new PluginLocation("pluginId"), ProblemLocation.class);
    assertConversion(new CodeLocation("a", null, null), ProblemLocation.class);
    assertConversion(new CodeLocation("a", "b", null), ProblemLocation.class);
    assertConversion(new CodeLocation("a", "b", "c"), ProblemLocation.class);
    assertConversion(new CodeLocation("a", null, "c"), ProblemLocation.class);
  }

  private <K, V> void assertMultimapConversion(Multimap<K, V> expectedMultimap) {
    //we have to explicitly specify multimap type arguments
    Type type = new TypeToken<Multimap<IdeVersion, UpdateInfo>>() {
    }.getType();
    String json = GsonHolder.INSTANCE.getGSON().toJson(expectedMultimap, type);
    Multimap t = GsonHolder.INSTANCE.getGSON().fromJson(json, type);

    //check that contents are equal (multimap-backing classes may differ)
    HashMultimap<K, V> expectedCopy = HashMultimap.create(expectedMultimap);
    HashMultimap actualCopy = HashMultimap.create(t);

    Assert.assertEquals(expectedCopy, actualCopy);
  }
}
