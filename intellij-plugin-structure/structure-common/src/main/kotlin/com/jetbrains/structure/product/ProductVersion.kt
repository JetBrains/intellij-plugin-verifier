package com.jetbrains.structure.product

interface ProductVersion<VersionType : ProductVersion<VersionType>> : Comparable<VersionType>
