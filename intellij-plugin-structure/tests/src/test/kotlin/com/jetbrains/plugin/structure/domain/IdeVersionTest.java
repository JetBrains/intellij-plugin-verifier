package com.jetbrains.plugin.structure.domain;


import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static com.jetbrains.plugin.structure.intellij.version.IdeVersion.createIdeVersion;
import static org.junit.Assert.*;

public class IdeVersionTest {

  private static void assertParsed(IdeVersion n, int expectedBaseline, int expectedBuildNumber, String asString) {
    assertEquals(expectedBaseline, n.getBaselineVersion());
    assertEquals(expectedBuildNumber, n.getBuild());
    assertEquals(asString, n.asString());
  }

  @Test
  public void testWithoutProductCode() {
    //test that no exception
    version("144.2608.2");
    version("139.144");
    version("139.SNAPSHOT");
    version("139.SNAPSHOT");
    version("6.0");
  }

  @Test
  public void testTypicalBuild() {
    IdeVersion ideVersion = version("IU-138.1042");
    assertEquals(138, ideVersion.getBaselineVersion());
    assertEquals(1042, ideVersion.getBuild());
    assertEquals("IU", ideVersion.getProductCode());
    assertFalse(ideVersion.isSnapshot());
    assertEquals("IU-138.1042", ideVersion.asString());
    assertArrayEquals(new int[]{138, 1042}, ideVersion.getComponents());
  }

  @Test
  public void testBuildWithAttempt() {
    IdeVersion ideVersion = version("IU-138.1042.1");
    assertEquals(138, ideVersion.getBaselineVersion());
    assertEquals(1042, ideVersion.getBuild());
    assertEquals("IU", ideVersion.getProductCode());
    assertFalse(ideVersion.isSnapshot());
    assertEquals("IU-138.1042.1", ideVersion.asString(true, true));
    assertArrayEquals(new int[]{138, 1042, 1}, ideVersion.getComponents());
  }

  @Test
  public void testRiderTypicalBuild() {
    IdeVersion updateBuild = version("RS-144.4713");
    assertEquals(144, updateBuild.getBaselineVersion());
    assertEquals(4713, updateBuild.getBuild());
    assertEquals("RS", updateBuild.getProductCode());
//    assertEquals("rider", updateBuild.getProductName());
    assertFalse(updateBuild.isSnapshot());
    assertArrayEquals(new int[]{144, 4713}, updateBuild.getComponents());
  }

  /*@Test(expected = IllegalArgumentException.class)
  public void testUnsupportedProduct() {
    IdeVersion.createIdeVersion("XX-138.SNAPSHOT");
  }*/

  @Test
  public void testSnapshotBuild() {
    IdeVersion ideVersion = version("PS-136.SNAPSHOT");
    assertEquals(136, ideVersion.getBaselineVersion());
    assertEquals(Integer.MAX_VALUE, ideVersion.getBuild());
    assertEquals("PS", ideVersion.getProductCode());
    assertTrue(ideVersion.isSnapshot());
    assertEquals("PS-136.SNAPSHOT", ideVersion.asString());
    assertEquals("136.SNAPSHOT", ideVersion.asStringWithoutProductCode());
    assertEquals("136", ideVersion.asStringWithoutProductCodeAndSnapshot());

  }

  @Test
  public void testCLionTypicalBuild() {
    IdeVersion ideVersion = version("CL-140.1197");
    assertEquals(140, ideVersion.getBaselineVersion());
    assertEquals(1197, ideVersion.getBuild());
    assertEquals("CL", ideVersion.getProductCode());
    assertFalse(ideVersion.isSnapshot());
    assertEquals("CL-140.1197", ideVersion.asString());
  }

  @Test
  public void testOneNumberActualBuild() {
    IdeVersion updateBuild = version("133");
    assertEquals(133, updateBuild.getBaselineVersion());
    assertEquals(0, updateBuild.getBuild());
    assertEquals("", updateBuild.getProductCode());
    assertFalse(updateBuild.isSnapshot());
    assertEquals("133.0", updateBuild.asString());
    assertArrayEquals(new int[]{133, 0}, updateBuild.getComponents());
  }

  @Test
  public void testLegacyBuild() {
    IdeVersion updateBuild = version("8987");
    assertEquals(80, updateBuild.getBaselineVersion());
    assertEquals(8987, updateBuild.getBuild());
    assertEquals("", updateBuild.getProductCode());
    assertFalse(updateBuild.isSnapshot());
    assertEquals("80.8987", updateBuild.asString());
    assertArrayEquals(new int[]{80, 8987}, updateBuild.getComponents());
  }

  @Test
  public void testEqualsAndHashCode() {
    IdeVersion ic1 = version("IC-144.1532.2");
    IdeVersion ic2 = version("IC-144.1532.2");
    IdeVersion iu1 = version("IU-144.1532.2");
    assertEquals(ic1.hashCode(), ic2.hashCode());
    assertEquals(ic1, ic2);
    assertNotEquals(ic1.hashCode(), iu1.hashCode());
    assertNotEquals(ic1, iu1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void empty() {
    version(" ");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithTrailingDot() {
    version("139.");
  }

  @Test
  public void historicBuild() {
    assertEquals("75.7512", version("7512").asString());
  }

  @Test
  public void branchBasedBuild() {
    assertParsed(version("145"), 145, 0, "145.0");
    assertParsed(version("145.1"), 145, 1, "145.1");
    assertParsed(version("145.1.2"), 145, 1, "145.1.2");
    assertParsed(version("IU-145.1.2"), 145, 1, "IU-145.1.2");
    assertParsed(version("IU-145.*"), 145, Integer.MAX_VALUE, "IU-145.*");
    assertParsed(version("IU-145.SNAPSHOT"), 145, Integer.MAX_VALUE, "IU-145.SNAPSHOT");
    assertParsed(version("IU-145.1.*"), 145, 1, "IU-145.1.*");
    assertParsed(version("IU-145.1.SNAPSHOT"), 145, 1, "IU-145.1.SNAPSHOT");

    assertParsed(version("IU-145.1.2.3.4"), 145, 1, "IU-145.1.2.3.4");
    assertParsed(version("IU-145.1000.2000.3000.4000"), 145, 1000, "IU-145.1000.2000.3000.4000");
  }

  @Test
  public void components() {
    assertArrayEquals(new int[]{162, 1, 2, 3, 4, 5, 6}, version("IU-162.1.2.3.4.5.6").getComponents());
  }

  @Test
  public void comparingVersion() {
    assertTrue(version("145.1").compareTo(version("145.*")) < 0);
    assertTrue(version("145.1.1").compareTo(version("145.*")) < 0);
    assertTrue(version("145.1.1.1.1").compareTo(version("145.*")) < 0);
    assertTrue(version("145.1").compareTo(version("146.*")) < 0);
    assertTrue(version("145.1").compareTo(version("144.*")) > 0);
    assertTrue(version("145.1.1.1").compareTo(version("145.1.1.1.1")) < 0);
    assertTrue(version("145.1.1.2").compareTo(version("145.1.1.1.1")) > 0);
    assertTrue(version("145.2.2.2.2").compareTo(version("145.2.*")) < 0);
    assertTrue(version("145.2.*").compareTo(version("145.2.2.2.2")) > 0);

  }

  @Test
  public void studio() {
    IdeVersion version = version("Studio-1.0");
    assertEquals("Studio", version.getProductCode());
    assertEquals(1, version.getBaselineVersion());
    assertEquals(0, version.getBuild());
  }

  @Test
  public void fbIc() {
    IdeVersion version = version("FB-IC-143.157");
    assertEquals("FB-IC", version.getProductCode());
    assertEquals(143, version.getBaselineVersion());
    assertEquals(157, version.getBuild());
  }

  @Test
  public void isSnapshot() {
    assertTrue(version("SNAPSHOT").isSnapshot());
    assertTrue(version("__BUILD_NUMBER__").isSnapshot());
    assertTrue(version("IU-90.SNAPSHOT").isSnapshot());
    assertTrue(version("IU-145.1.2.3.4.SNAPSHOT").isSnapshot());
    assertFalse(version("IU-145.1.2.3.4").isSnapshot());

    assertFalse(version("IC-90.*").isSnapshot());
    assertFalse(version("90.9999999").isSnapshot());
  }

  @Test
  public void devSnapshotVersion() {
    IdeVersion b = version("__BUILD_NUMBER__");
    assertTrue(b.asString(), b.getBaselineVersion() >= 145 && b.getBaselineVersion() <= 3000);
    assertTrue(b.isSnapshot());

    assertEquals(version("__BUILD_NUMBER__"), version("SNAPSHOT"));
  }

  @Test
  public void compareIdeVersionsWithDifferentProductCodes() {
    assertTrue(version("IU-1.1").compareTo(version("IC-181.1")) > 0);
    assertTrue(version("IU-181.1").compareTo(version("IC-181.1")) > 0);
    assertTrue(version("IU-183.1").compareTo(version("IC-181.1")) > 0);
  }

  @Test
  public void snapshotDomination() {
    assertTrue(version("90.SNAPSHOT").compareTo(version("90.12345")) > 0);
    assertTrue(version("IU-90.SNAPSHOT").compareTo(version("IU-90.12345")) > 0);
    assertTrue(version("IU-90.SNAPSHOT").compareTo(version("IU-100.12345")) < 0);
    assertTrue(version("IU-90.SNAPSHOT").compareTo(version("IU-100.SNAPSHOT")) < 0);
    assertEquals(0, version("IU-90.SNAPSHOT").compareTo(version("IU-90.SNAPSHOT")));

    assertTrue(version("145.SNAPSHOT").compareTo(version("145.1")) > 0);
    assertTrue(version("145.1").compareTo(version("145.SNAPSHOT")) < 0);

    assertEquals(0, version("145.SNAPSHOT").compareTo(version("145.*")));
    assertEquals(0, version("145.*").compareTo(version("145.SNAPSHOT")));

    assertTrue(version("145.SNAPSHOT").compareTo(version("145.1.*")) > 0);
    assertTrue(version("145.1.*").compareTo(version("145.SNAPSHOT")) < 0);

    assertEquals(0, version("145.SNAPSHOT").compareTo(version("145.SNAPSHOT")));

    assertEquals(0, version("145.1.SNAPSHOT").compareTo(version("145.1.*")));
    assertEquals(0, version("145.1.*").compareTo(version("145.1.SNAPSHOT")));

    assertTrue(version("145.1.SNAPSHOT").compareTo(version("145.*")) < 0);
    assertTrue(version("145.*").compareTo(version("145.1.SNAPSHOT")) > 0);

    assertTrue(version("145.1.SNAPSHOT").compareTo(version("145.1.1")) > 0);
    assertTrue(version("145.1.1").compareTo(version("145.1.SNAPSHOT")) < 0);

    assertEquals(0, version("145.1.SNAPSHOT").compareTo(version("145.1.SNAPSHOT")));

    assertTrue(version("145.SNAPSHOT.1").compareTo(version("145.1.1")) > 0);
    assertTrue(version("145.1.1").compareTo(version("145.SNAPSHOT.1")) < 0);

    assertTrue(version("145.SNAPSHOT.1").compareTo(version("145.1.SNAPSHOT")) > 0);
    assertTrue(version("145.1.SNAPSHOT").compareTo(version("145.SNAPSHOT.1")) < 0);

    assertEquals(0, version("145.SNAPSHOT.1").compareTo(version("145.SNAPSHOT.SNAPSHOT")));
    assertEquals(0, version("145.SNAPSHOT.SNAPSHOT").compareTo(version("145.SNAPSHOT.1")));
  }

  @NotNull
  private IdeVersion version(String s) {
    return createIdeVersion(s);
  }

  @Test
  public void currentVersion() {
    IdeVersion current = version("IU-146.SNAPSHOT");
    assertTrue(current.isSnapshot());

    assertTrue(current.compareTo(version("7512")) > 0);
    assertTrue(current.compareTo(version("145")) > 0);
    assertTrue(current.compareTo(version("145.12")) > 0);
  }

  @Test
  public void validAndInvalidIdeVersions() {
    assertTrue(IdeVersion.isValidIdeVersion("IU-163.1"));
    assertTrue(IdeVersion.isValidIdeVersion("IU-163.SNAPSHOT"));

    assertFalse(IdeVersion.isValidIdeVersion("IU-163."));
    assertFalse(IdeVersion.isValidIdeVersion("SNAPSHOT.163"));
    assertFalse(IdeVersion.isValidIdeVersion("172-4144.1"));
    assertFalse(IdeVersion.isValidIdeVersion("-AB-4144.1"));
    assertFalse(IdeVersion.isValidIdeVersion("-AB--4144.1"));
    assertFalse(IdeVersion.isValidIdeVersion("A--B-4144.1"));
    assertFalse(IdeVersion.isValidIdeVersion("A1-4144.1"));

    assertNotNull(IdeVersion.createIdeVersionIfValid("IU-163.1"));
    assertNotNull(IdeVersion.createIdeVersionIfValid("IU-163.SNAPSHOT"));
  }
}