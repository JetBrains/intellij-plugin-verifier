package com.jetbrains.plugin.structure.base.product

interface ProductVersion<VersionType : ProductVersion<VersionType>> : Comparable<VersionType>
