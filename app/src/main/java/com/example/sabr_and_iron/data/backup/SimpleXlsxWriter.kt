package com.example.sabr_and_iron.data.backup

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal XLSX writer (shared strings + sheet rows) for Android — no Apache POI.
 */
internal object SimpleXlsxWriter {
    data class Sheet(
        val name: String,
        val rows: List<List<Any?>>,
    )

    fun write(file: File, sheets: List<Sheet>) {
        require(sheets.isNotEmpty()) { "Workbook needs at least one sheet" }
        val shared = linkedMapOf<String, Int>()
        fun share(text: String): Int =
            shared.getOrPut(text) { shared.size }

        val sheetXmls = sheets.map { sheet ->
            buildSheetXml(sheet.rows) { value ->
                when (value) {
                    null -> null
                    is Number -> CellXml.Number(value.toDouble())
                    is Boolean -> CellXml.Shared(share(if (value) "TRUE" else "FALSE"))
                    else -> CellXml.Shared(share(value.toString()))
                }
            }
        }

        FileOutputStream(file).use { fos ->
            ZipOutputStream(fos).use { zip ->
                zip.putUtf8(
                    "[Content_Types].xml",
                    contentTypesXml(sheets.size),
                )
                zip.putUtf8("_rels/.rels", rootRelsXml())
                zip.putUtf8("xl/workbook.xml", workbookXml(sheets.map { it.name }))
                zip.putUtf8("xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
                zip.putUtf8("xl/sharedStrings.xml", sharedStringsXml(shared.keys.toList()))
                zip.putUtf8("xl/styles.xml", stylesXml())
                sheetXmls.forEachIndexed { index, xml ->
                    zip.putUtf8("xl/worksheets/sheet${index + 1}.xml", xml)
                }
            }
        }
    }

    private sealed class CellXml {
        data class Number(val value: Double) : CellXml()
        data class Shared(val index: Int) : CellXml()
    }

    private fun buildSheetXml(
        rows: List<List<Any?>>,
        mapCell: (Any?) -> CellXml?,
    ): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""",
        )
        append("<sheetData>")
        rows.forEachIndexed { rowIndex, row ->
            val r = rowIndex + 1
            append("""<row r="$r">""")
            row.forEachIndexed { colIndex, raw ->
                val cell = mapCell(raw) ?: return@forEachIndexed
                val ref = cellRef(colIndex, r)
                when (cell) {
                    is CellXml.Number ->
                        append("""<c r="$ref"><v>${cell.value}</v></c>""")
                    is CellXml.Shared ->
                        append("""<c r="$ref" t="s"><v>${cell.index}</v></c>""")
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun cellRef(colIndex: Int, row: Int): String {
        var n = colIndex + 1
        val letters = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            letters.insert(0, ('A' + rem))
            n = (n - 1) / 26
        }
        return "$letters$row"
    }

    private fun contentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
        for (i in 1..sheetCount) {
            append(
                """<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""",
            )
        }
        append("</Types>")
    }

    private fun rootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private fun workbookXml(names: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append(
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
                """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""",
        )
        append("<sheets>")
        names.forEachIndexed { index, name ->
            val safe = name
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("\"", "&quot;")
            append(
                """<sheet name="$safe" sheetId="${index + 1}" r:id="rId${index + 1}"/>""",
            )
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append(
                """<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""",
            )
        }
        append(
            """<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>""",
        )
        append(
            """<Relationship Id="rId${sheetCount + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""",
        )
        append("</Relationships>")
    }

    private fun sharedStringsXml(strings: List<String>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append(
            """<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
                """count="${strings.size}" uniqueCount="${strings.size}">""",
        )
        strings.forEach { text ->
            val escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            append("<si><t>")
            append(escaped)
            append("</t></si>")
        }
        append("</sst>")
    }

    private fun stylesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>""" +
            """<fills count="1"><fill><patternFill patternType="none"/></fill></fills>""" +
            """<borders count="1"><border/></borders>""" +
            """<cellStyleXfs count="1"><xf/></cellStyleXfs>""" +
            """<cellXfs count="1"><xf/></cellXfs>""" +
            """</styleSheet>"""

    private fun ZipOutputStream.putUtf8(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
