package com.codingcat.changelogs.base.util

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResourceUtilTest {
    @Test
    fun testReadResourceAsString() {
        val content = ResourceUtil.readResourceAsString("defaults/config.yml")
        assertTrue(content.contains("changelog_storage"))
    }

    @Test
    fun testReadNonExistentResourceThrows() {
        assertFailsWith<IllegalStateException> {
            ResourceUtil.readResourceAsString("non_existent_resource_file_xyz.txt")
        }
    }

    @Test
    fun testListResources() {
        val list = ResourceUtil.listResources("lang")
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.endsWith(".yml") })
    }

    @Test
    fun testReadResourcesAsString() {
        val map = ResourceUtil.readResourcesAsString("lang")
        assertTrue(map.isNotEmpty())
        assertTrue(map.containsKey("en-US.yml") || map.keys.any { it.endsWith(".yml") })
    }
}
