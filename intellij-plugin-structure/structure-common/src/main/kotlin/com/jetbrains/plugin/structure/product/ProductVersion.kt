package com.jetbrains.plugin.structure.product

interface ProductVersion<VersionType : ProductVersion<VersionType>> : Comparable<VersionType>
