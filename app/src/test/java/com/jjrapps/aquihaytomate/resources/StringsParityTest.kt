package com.jjrapps.aquihaytomate.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Fails the build when a string is added to one locale and not the other.
 *
 * Runs on the JVM by parsing the resource XML directly: Gradle sets the test working directory to
 * the module root, and enumerating string resources through the `R` class at runtime is not
 * possible. Cheaper and stricter than an instrumented check.
 */
class StringsParityTest {

    private val baseFile = File("src/main/res/values/strings.xml")
    private val spanishFile = File("src/main/res/values-es/strings.xml")

    @Test
    fun `both strings files exist`() {
        assertTrue("Missing ${baseFile.path}", baseFile.isFile)
        assertTrue("Missing ${spanishFile.path}", spanishFile.isFile)
    }

    @Test
    fun `every translatable string is present in both locales`() {
        val base = translatableNames(baseFile, "string")
        val spanish = names(spanishFile, "string")

        assertEquals(
            "Strings missing from values-es/strings.xml",
            emptySet<String>(),
            base - spanish,
        )
        assertEquals(
            "Strings in values-es/strings.xml that no longer exist in values/strings.xml",
            emptySet<String>(),
            spanish - base,
        )
    }

    @Test
    fun `every string array is present in both locales`() {
        val base = translatableNames(baseFile, "string-array")
        val spanish = names(spanishFile, "string-array")

        assertEquals("String arrays missing from values-es", emptySet<String>(), base - spanish)
        assertEquals("Orphan string arrays in values-es", emptySet<String>(), spanish - base)
    }

    @Test
    fun `every plurals is present in both locales`() {
        val base = translatableNames(baseFile, "plurals")
        val spanish = names(spanishFile, "plurals")

        assertEquals("Plurals missing from values-es", emptySet<String>(), base - spanish)
        assertEquals("Orphan plurals in values-es", emptySet<String>(), spanish - base)
    }

    /**
     * Quantity sets legitimately differ between languages (Russian has `few` and `many`), so only
     * the fallback is required. A `plurals` without `other` crashes at format time.
     */
    @Test
    fun `every plurals declares the other quantity`() {
        listOf(baseFile, spanishFile).forEach { file ->
            elements(file, "plurals").forEach { plural ->
                val quantities = plural.getElementsByTagName("item")
                    .let { items -> (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") } }
                assertTrue(
                    "plurals ${plural.getAttribute("name")} in ${file.path} has no `other` item",
                    "other" in quantities,
                )
            }
        }
    }

    @Test
    fun `changelog arrays have the same number of items in both locales`() {
        val base = arrayItemCounts(baseFile)
        val spanish = arrayItemCounts(spanishFile)

        base.forEach { (name, count) ->
            assertEquals("Item count differs for string-array $name", count, spanish[name])
        }
    }

    private fun elements(file: File, tag: String): List<Element> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun names(file: File, tag: String): Set<String> =
        elements(file, tag).map { it.getAttribute("name") }.toSet()

    /** `translatable="false"` entries (the app name) are intentionally base-only. */
    private fun translatableNames(file: File, tag: String): Set<String> =
        elements(file, tag)
            .filter { it.getAttribute("translatable") != "false" }
            .map { it.getAttribute("name") }
            .toSet()

    private fun arrayItemCounts(file: File): Map<String, Int> =
        elements(file, "string-array").associate { array ->
            array.getAttribute("name") to array.getElementsByTagName("item").length
        }
}
