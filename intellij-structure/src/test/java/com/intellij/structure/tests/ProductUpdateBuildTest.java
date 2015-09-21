package com.intellij.structure.tests;


import com.intellij.structure.utils.ProductUpdateBuild;
import org.junit.Assert;
import org.junit.Test;

public class ProductUpdateBuildTest {
  @Test
  public void testTypicalBuild() {
    ProductUpdateBuild productUpdateBuild = new ProductUpdateBuild("IU-138.1042");
    Assert.assertEquals(138, productUpdateBuild.getBranch());
    Assert.assertEquals(1042, productUpdateBuild.getBuild());
    Assert.assertEquals(0, productUpdateBuild.getAttempt());
    Assert.assertEquals("IU", productUpdateBuild.getProductCode());
    Assert.assertEquals("idea", productUpdateBuild.getProductName());
    Assert.assertEquals(false, productUpdateBuild.isSnapshot());
    Assert.assertEquals(true, productUpdateBuild.isOk());
  }

  @Test
  public void testBuildWithAttempt() {
    ProductUpdateBuild productUpdateBuild = new ProductUpdateBuild("IU-138.1042.1");
    Assert.assertEquals(138, productUpdateBuild.getBranch());
    Assert.assertEquals(1042, productUpdateBuild.getBuild());
    Assert.assertEquals(1, productUpdateBuild.getAttempt());
    Assert.assertEquals("IU", productUpdateBuild.getProductCode());
    Assert.assertEquals("idea", productUpdateBuild.getProductName());
    Assert.assertEquals(false, productUpdateBuild.isSnapshot());
    Assert.assertEquals(true, productUpdateBuild.isOk());
  }

  @Test
  public void testSnapshotBuild() {
    ProductUpdateBuild productUpdateBuild = new ProductUpdateBuild("PS-136.SNAPSHOT");
    Assert.assertEquals(136, productUpdateBuild.getBranch());
    Assert.assertEquals(Integer.MAX_VALUE, productUpdateBuild.getBuild());
    Assert.assertEquals(0, productUpdateBuild.getAttempt());
    Assert.assertEquals("PS", productUpdateBuild.getProductCode());
    Assert.assertEquals("phpStorm", productUpdateBuild.getProductName());
    Assert.assertEquals(true, productUpdateBuild.isSnapshot());
    Assert.assertEquals(true, productUpdateBuild.isOk());
  }

  @Test
  public void testUnsupportedProduct() {
    ProductUpdateBuild productUpdateBuild = new ProductUpdateBuild("XX-138.SNAPSHOT");
    Assert.assertEquals(0, productUpdateBuild.getBranch());
    Assert.assertEquals(0, productUpdateBuild.getBuild());
    Assert.assertEquals("", productUpdateBuild.getProductCode());
    Assert.assertNull(productUpdateBuild.getProductName());
    Assert.assertEquals(false, productUpdateBuild.isSnapshot());
    Assert.assertEquals(false, productUpdateBuild.isOk());
  }

  @Test
  public void testCLionTypicalBuild() {
    ProductUpdateBuild productUpdateBuild = new ProductUpdateBuild("CL-140.1197");
    Assert.assertEquals(140, productUpdateBuild.getBranch());
    Assert.assertEquals(1197, productUpdateBuild.getBuild());
    Assert.assertEquals("CL", productUpdateBuild.getProductCode());
    Assert.assertEquals("clion", productUpdateBuild.getProductName());
    Assert.assertEquals(false, productUpdateBuild.isSnapshot());
    Assert.assertEquals(true, productUpdateBuild.isOk());
  }
}