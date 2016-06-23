package com.intellij.structure.domain;


import org.junit.Assert;
import org.junit.Test;

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
    IdeVersion.createIdeVersion("144.2608.2");
    IdeVersion.createIdeVersion("139.144");
    IdeVersion.createIdeVersion("139.SNAPSHOT");
    IdeVersion.createIdeVersion("139.SNAPSHOT");
    IdeVersion.createIdeVersion("6.0");
  }

  @Test
  public void testTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042");
    Assert.assertEquals(138, ideVersion.getBaselineVersion());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("IU-138.1042", ideVersion.asString());
    Assert.assertArrayEquals(new int[]{138, 1042}, ideVersion.getComponents());
  }

  @Test
  public void testBuildWithAttempt() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("IU-138.1042.1");
    Assert.assertEquals(138, ideVersion.getBaselineVersion());
    Assert.assertEquals(1042, ideVersion.getBuild());
    Assert.assertEquals("IU", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("IU-138.1042.1", ideVersion.asString(true, true));
    Assert.assertArrayEquals(new int[]{138, 1042, 1}, ideVersion.getComponents());
  }

  @Test
  public void testRiderTypicalBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("RS-144.4713");
    Assert.assertEquals(144, updateBuild.getBaselineVersion());
    Assert.assertEquals(4713, updateBuild.getBuild());
    Assert.assertEquals("RS", updateBuild.getProductCode());
//    Assert.assertEquals("rider", updateBuild.getProductName());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertArrayEquals(new int[]{144, 4713}, updateBuild.getComponents());
  }

  /*@Test(expected = IllegalArgumentException.class)
  public void testUnsupportedProduct() {
    IdeVersion.createIdeVersion("XX-138.SNAPSHOT");
  }*/

  @Test
  public void testSnapshotBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("PS-136.SNAPSHOT");
    Assert.assertEquals(136, ideVersion.getBaselineVersion());
    Assert.assertEquals(Integer.MAX_VALUE, ideVersion.getBuild());
    Assert.assertEquals("PS", ideVersion.getProductCode());
    Assert.assertEquals(true, ideVersion.isSnapshot());
    Assert.assertEquals("PS-136.SNAPSHOT", ideVersion.asString());
    Assert.assertEquals("136.SNAPSHOT", ideVersion.asStringWithoutProductCode());
    Assert.assertEquals("136", ideVersion.asStringWithoutProductCodeAndSnapshot());

  }

  @Test
  public void testCLionTypicalBuild() {
    IdeVersion ideVersion = IdeVersion.createIdeVersion("CL-140.1197");
    Assert.assertEquals(140, ideVersion.getBaselineVersion());
    Assert.assertEquals(1197, ideVersion.getBuild());
    Assert.assertEquals("CL", ideVersion.getProductCode());
    Assert.assertEquals(false, ideVersion.isSnapshot());
    Assert.assertEquals("CL-140.1197", ideVersion.asString());
  }

  @Test
  public void testOneNumberActualBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("133");
    Assert.assertEquals(133, updateBuild.getBaselineVersion());
    Assert.assertEquals(0, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("133.0", updateBuild.asString());
    Assert.assertArrayEquals(new int[]{133, 0}, updateBuild.getComponents());
  }

  @Test
  public void testLegacyBuild() {
    IdeVersion updateBuild = IdeVersion.createIdeVersion("8987");
    Assert.assertEquals(80, updateBuild.getBaselineVersion());
    Assert.assertEquals(8987, updateBuild.getBuild());
    Assert.assertEquals("", updateBuild.getProductCode());
    Assert.assertEquals(false, updateBuild.isSnapshot());
    Assert.assertEquals("80.8987", updateBuild.asString());
    Assert.assertArrayEquals(new int[]{80, 8987}, updateBuild.getComponents());
  }

  @Test
  public void testEqualsAndHashCode() throws Exception {
    IdeVersion ic1 = IdeVersion.createIdeVersion("IC-144.1532.2");
    IdeVersion ic2 = IdeVersion.createIdeVersion("IC-144.1532.2");
    IdeVersion iu1 = IdeVersion.createIdeVersion("IU-144.1532.2");
    Assert.assertEquals(ic1.hashCode(), ic2.hashCode());
    Assert.assertEquals(ic1, ic2);
    Assert.assertNotEquals(ic1.hashCode(), iu1.hashCode());
    Assert.assertNotEquals(ic1, iu1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void empty() {
    IdeVersion.createIdeVersion(" ");
  }

  @Test
  public void historicBuild() {
    assertEquals("75.7512", IdeVersion.createIdeVersion("7512").asString());
  }

  @Test
  public void branchBasedBuild() throws Exception {
    assertParsed(IdeVersion.createIdeVersion("145"), 145, 0, "145.0");
    assertParsed(IdeVersion.createIdeVersion("145.1"), 145, 1, "145.1");
    assertParsed(IdeVersion.createIdeVersion("145.1.2"), 145, 1, "145.1.2");
    assertParsed(IdeVersion.createIdeVersion("IU-145.1.2"), 145, 1, "IU-145.1.2");
    assertParsed(IdeVersion.createIdeVersion("IU-145.*"), 145, Integer.MAX_VALUE, "IU-145.*");
    assertParsed(IdeVersion.createIdeVersion("IU-145.SNAPSHOT"), 145, Integer.MAX_VALUE, "IU-145.SNAPSHOT");
    assertParsed(IdeVersion.createIdeVersion("IU-145.1.*"), 145, 1, "IU-145.1.*");
    assertParsed(IdeVersion.createIdeVersion("IU-145.1.SNAPSHOT"), 145, 1, "IU-145.1.SNAPSHOT");

    assertParsed(IdeVersion.createIdeVersion("IU-145.1.2.3.4"), 145, 1, "IU-145.1.2.3.4");
    assertParsed(IdeVersion.createIdeVersion("IU-145.1000.2000.3000.4000"), 145, 1000, "IU-145.1000.2000.3000.4000");
  }

  @Test
  public void components() throws Exception {
    Assert.assertArrayEquals(new int[]{162, 1, 2, 3, 4, 5, 6}, IdeVersion.createIdeVersion("IU-162.1.2.3.4.5.6").getComponents());
  }

  @Test
  public void comparingVersion() throws Exception {
    assertTrue(IdeVersion.createIdeVersion("145.1").compareTo(IdeVersion.createIdeVersion("145.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1").compareTo(IdeVersion.createIdeVersion("145.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1.1.1").compareTo(IdeVersion.createIdeVersion("145.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.1").compareTo(IdeVersion.createIdeVersion("146.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.1").compareTo(IdeVersion.createIdeVersion("144.*")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1.1").compareTo(IdeVersion.createIdeVersion("145.1.1.1.1")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1.2").compareTo(IdeVersion.createIdeVersion("145.1.1.1.1")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.2.2.2.2").compareTo(IdeVersion.createIdeVersion("145.2.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.2.*").compareTo(IdeVersion.createIdeVersion("145.2.2.2.2")) > 0);

  }

  @Test
  public void studio() throws Exception {
    IdeVersion version = IdeVersion.createIdeVersion("Studio-1.0");
    Assert.assertEquals("Studio", version.getProductCode());
    Assert.assertEquals(1, version.getBaselineVersion());
    Assert.assertEquals(0, version.getBuild());
  }

  @Test
  public void fbIc() throws Exception {
    IdeVersion version = IdeVersion.createIdeVersion("FB-IC-143.157");
    Assert.assertEquals("FB-IC", version.getProductCode());
    Assert.assertEquals(143, version.getBaselineVersion());
    Assert.assertEquals(157, version.getBuild());
  }

  @Test
  public void isSnapshot() {
    assertTrue(IdeVersion.createIdeVersion("SNAPSHOT").isSnapshot());
    assertTrue(IdeVersion.createIdeVersion("__BUILD_NUMBER__").isSnapshot());
    assertTrue(IdeVersion.createIdeVersion("IU-90.SNAPSHOT").isSnapshot());
    assertTrue(IdeVersion.createIdeVersion("IU-145.1.2.3.4.SNAPSHOT").isSnapshot());
    assertFalse(IdeVersion.createIdeVersion("IU-145.1.2.3.4").isSnapshot());

    assertFalse(IdeVersion.createIdeVersion("IC-90.*").isSnapshot());
    assertFalse(IdeVersion.createIdeVersion("90.9999999").isSnapshot());
  }

  @Test
  public void devSnapshotVersion() throws Exception {
    IdeVersion b = IdeVersion.createIdeVersion("__BUILD_NUMBER__");
    assertTrue(b.asString(), b.getBaselineVersion() >= 145 && b.getBaselineVersion() <= 3000);
    assertTrue(b.isSnapshot());

    assertEquals(IdeVersion.createIdeVersion("__BUILD_NUMBER__"), IdeVersion.createIdeVersion("SNAPSHOT"));
  }

  @Test
  public void snapshotDomination() {
    assertTrue(IdeVersion.createIdeVersion("90.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("90.12345")) > 0);
    assertTrue(IdeVersion.createIdeVersion("IU-90.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("RM-90.12345")) > 0);
    assertTrue(IdeVersion.createIdeVersion("IU-90.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("RM-100.12345")) < 0);
    assertTrue(IdeVersion.createIdeVersion("IU-90.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("RM-100.SNAPSHOT")) < 0);
    assertTrue(IdeVersion.createIdeVersion("IU-90.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("RM-90.SNAPSHOT")) == 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.1")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT")) < 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.*")) == 0);
    assertTrue(IdeVersion.createIdeVersion("145.*").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT")) == 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.1.*")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.*").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT")) < 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT")) == 0);

    assertTrue(IdeVersion.createIdeVersion("145.1.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.1.*")) == 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.*").compareTo(IdeVersion.createIdeVersion("145.1.SNAPSHOT")) == 0);

    assertTrue(IdeVersion.createIdeVersion("145.1.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.*")) < 0);
    assertTrue(IdeVersion.createIdeVersion("145.*").compareTo(IdeVersion.createIdeVersion("145.1.SNAPSHOT")) > 0);

    assertTrue(IdeVersion.createIdeVersion("145.1.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.1.1")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1").compareTo(IdeVersion.createIdeVersion("145.1.SNAPSHOT")) < 0);

    assertTrue(IdeVersion.createIdeVersion("145.1.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.1.SNAPSHOT")) == 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT.1").compareTo(IdeVersion.createIdeVersion("145.1.1")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.1").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT.1")) < 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT.1").compareTo(IdeVersion.createIdeVersion("145.1.SNAPSHOT")) > 0);
    assertTrue(IdeVersion.createIdeVersion("145.1.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT.1")) < 0);

    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT.1").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT.SNAPSHOT")) == 0);
    assertTrue(IdeVersion.createIdeVersion("145.SNAPSHOT.SNAPSHOT").compareTo(IdeVersion.createIdeVersion("145.SNAPSHOT.1")) == 0);
  }

  @Test
  public void currentVersion() throws Exception {
    IdeVersion current = IdeVersion.createIdeVersion("IU-146.SNAPSHOT");
    assertTrue(current.isSnapshot());

    assertTrue(current.compareTo(IdeVersion.createIdeVersion("7512")) > 0);
    assertTrue(current.compareTo(IdeVersion.createIdeVersion("145")) > 0);
    assertTrue(current.compareTo(IdeVersion.createIdeVersion("145.12")) > 0);
  }

}