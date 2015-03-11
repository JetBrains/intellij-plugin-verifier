package com.jetbrains.pluginUtils;

import org.junit.Assert;
import org.junit.Test;

public class UpdateBuildTest {
    @Test
    public void testTypicalBuild() {
        UpdateBuild updateBuild = new UpdateBuild("IU-138.1042");
        Assert.assertEquals(138, updateBuild.getBranch());
        Assert.assertEquals(1042, updateBuild.getBuild());
        Assert.assertEquals(0, updateBuild.getAttempt());
        Assert.assertEquals("IU", updateBuild.getProductCode());
        Assert.assertEquals("idea", updateBuild.getProductName());
        Assert.assertEquals(false, updateBuild.isSnapshot());
        Assert.assertEquals(true, updateBuild.isOk());
    }

    @Test
    public void testBuildWithAttempt() {
        UpdateBuild updateBuild = new UpdateBuild("IU-138.1042.1");
        Assert.assertEquals(138, updateBuild.getBranch());
        Assert.assertEquals(1042, updateBuild.getBuild());
        Assert.assertEquals(1, updateBuild.getAttempt());
        Assert.assertEquals("IU", updateBuild.getProductCode());
        Assert.assertEquals("idea", updateBuild.getProductName());
        Assert.assertEquals(false, updateBuild.isSnapshot());
        Assert.assertEquals(true, updateBuild.isOk());
    }

    @Test
    public void testSnapshotBuild() {
        UpdateBuild updateBuild = new UpdateBuild("PS-136.SNAPSHOT");
        Assert.assertEquals(136, updateBuild.getBranch());
        Assert.assertEquals(Integer.MAX_VALUE, updateBuild.getBuild());
        Assert.assertEquals(0, updateBuild.getAttempt());
        Assert.assertEquals("PS", updateBuild.getProductCode());
        Assert.assertEquals("phpStorm", updateBuild.getProductName());
        Assert.assertEquals(true, updateBuild.isSnapshot());
        Assert.assertEquals(true, updateBuild.isOk());
    }

    @Test
    public void testUnsupportedProduct() {
        UpdateBuild updateBuild = new UpdateBuild("XX-138.SNAPSHOT");
        Assert.assertEquals(0, updateBuild.getBranch());
        Assert.assertEquals(0, updateBuild.getBuild());
        Assert.assertEquals("", updateBuild.getProductCode());
        Assert.assertNull(updateBuild.getProductName());
        Assert.assertEquals(false, updateBuild.isSnapshot());
        Assert.assertEquals(false, updateBuild.isOk());
    }

    @Test
    public void testCLionTypicalBuild() {
        UpdateBuild updateBuild = new UpdateBuild("CL-140.1197");
        Assert.assertEquals(140, updateBuild.getBranch());
        Assert.assertEquals(1197, updateBuild.getBuild());
        Assert.assertEquals("CL", updateBuild.getProductCode());
        Assert.assertEquals("clion", updateBuild.getProductName());
        Assert.assertEquals(false, updateBuild.isSnapshot());
        Assert.assertEquals(true, updateBuild.isOk());
    }
}