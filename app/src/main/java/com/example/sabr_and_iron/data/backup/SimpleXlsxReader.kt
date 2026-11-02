package com.example.sabr_and_iron.data.backup

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Minimal XLSX reader (shared strings + sheet rows) for Android.
 * Avoids Apache POI, which often fails under R8 when opening workbooks.
 */
internal class SimpleXlsxReader(inputStream: InputStream) : AutoCloseable {
    private val entries: Map<String, ByteArray>
    private val sharedStrings: List<String>
    private val sheetsByName: Map<String, String>

    init {
        entries = ZipInputStream(inputStream).use { zip ->
            buildMap {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        put(entry.name, zip.readBytes())
                    }
                    zip.closeEntry()
                }
            }
        }
        sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        sheetsByName = parseWorkbookSheets(
            workbookXml = entries["xl/workbook.xml"]
                ?: error("Not a valid .xlsx file (missing workbook.xml)"),
            relsXml = entries["xl/_rels/workbook.xml.rels"]
                ?: error("Not a valid .xlsx file (missing workbook relationships)"),
        )
    }

    fun sheetNames(): Set<String> = sheetsByName.keys

    fun readSheetRows(sheetName: String): List<Map<Int, String>> {
        val path = sheetsByName[sheetName]
            ?: error("Excel backup is missing sheet \"$sheetName\"")
        val xml = entries[path]
            ?: error("Excel backup is missing sheet data for \"$sheetName\"")
        return parseSheetRows(xml, sharedStrings)
    }

    override fun close() = Unit

    private fun parseWorkbookSheets(
        workbookXml: ByteArray,
        relsXml: ByteArray,
    ): Map<String, String> {
        val relIdToTarget = mutableMapOf<String, String>()
        val relsDoc = parseDocument(relsXml)
        for (node in relsDoc.getElementsByTagName("Relationship").elements()) {
            val id = node.getAttribute("Id").takeIf { it.isNotBlank() } ?: continue
            val target = node.getAttribute("Target").takeIf { it.isNotBlank() } ?: continue
            relIdToTarget[id] = target
        }

        val sheets = linkedMapOf<String, String>()
        val workbookDoc = parseDocument(workbookXml)
        for (node in workbookDoc.getElementsByTagName("sheet").elements()) {
            val name = node.getAttribute("name").takeIf { it.isNotBlank() } ?: continue
            val relId = relationshipId(node) ?: continue
            val target = relIdToTarget[relId] ?: continue
            val path = when {
                target.startsWith("/") -> target.removePrefix("/")
                target.startsWith("xl/") -> target
                else -> "xl/$target"
            }
            sheets[name] = path
        }
        return sheets
    }

    private fun relationshipId(sheet: Element): String? {
        sheet.getAttribute("r:id").takeIf { it.isNotBlank() }?.let { return it }
        return sheet.getAttributeNS(
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
            "id",
        ).takeIf { it.isNotBlank() }
    }

    private fun parseSharedStrings(xml: ByteArray?): List<String> {
        if (xml == null) return emptyList()
        val doc = parseDocument(xml)
        return doc.getElementsByTagName("si").elements().map { si ->
            buildString {
                for (t in si.getElementsByTagName("t").elements()) {
                    append(t.textContent.orEmpty())
                }
            }
        }
    }

    private fun parseSheetRows(
        xml: ByteArray,
        shared: List<String>,
    ): List<Map<Int, String>> {
        val doc = parseDocument(xml)
        val sheetData = doc.getElementsByTagName("sheetData").item(0) as? Element
            ?: return emptyList()
        val rows = mutableListOf<Map<Int, String>>()
        for (rowNode in sheetData.childElements("row")) {
            val cells = linkedMapOf<Int, String>()
            for (cell in rowNode.childElements("c")) {
                val ref = cell.getAttribute("r")
                if (ref.isBlank()) continue
                val col = columnIndex(ref)
                val type = cell.getAttribute("t")
                val raw = when (type) {
                    "inlineStr" -> cell.getElementsByTagName("t").item(0)?.textContent
                    else -> cell.getElementsByTagName("v").item(0)?.textContent
                }?.trim().orEmpty()
                if (raw.isEmpty()) continue
                val text = when (type) {
                    "s" -> {
                        val index = raw.toIntOrNull() ?: continue
                        shared.getOrNull(index) ?: continue
                    }
                    "b" -> if (raw == "1") "TRUE" else "FALSE"
                    else -> raw
                }.trim()
                if (text.isNotEmpty()) cells[col] = text
            }
            if (cells.isNotEmpty()) rows.add(cells)
        }
        return rows
    }

    private fun columnIndex(cellRef: String): Int {
        var i = 0
        var col = 0
        while (i < cellRef.length && cellRef[i].isLetter()) {
            col = col * 26 + (cellRef[i].uppercaseChar() - 'A' + 1)
            i++
        }
        return col - 1
    }

    private fun parseDocument(bytes: ByteArray) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
        }.newDocumentBuilder().parse(bytes.inputStream())

    private fun NodeList.elements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.childElements(tag: String): List<Element> =
        (0 until childNodes.length).mapNotNull { index ->
            val node = childNodes.item(index) as? Element ?: return@mapNotNull null
            if (node.nodeName == tag || node.localName == tag) node else null
        }
}
