package com.finals_prog.campusfix.data

import android.content.Context
import java.io.File

/**
 * Stores submitted reports in <app files dir>/reports.csv (RFC 4180 style:
 * fields containing commas, quotes or line breaks are quoted, quotes doubled).
 *
 * All methods do blocking file IO: call them off the main thread (Dispatchers.IO).
 */
class ReportCsvStorage(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()

    /**
     * Creates the next report ID and appends the report in a single locked step,
     * so two submissions can never receive the same ID.
     */
    fun append(build: (reportId: String) -> Report): Report = synchronized(lock) {
        val report = build(nextReportId())
        if (!file.exists() || file.length() == 0L) {
            file.appendText(HEADER.joinToString(",") + NEWLINE)
        }
        file.appendText(report.toCsvRow() + NEWLINE)
        report
    }

    fun readAll(): List<Report> = synchronized(lock) {
        if (!file.exists()) return emptyList()
        val rows = parseCsv(file.readText())
        if (rows.isEmpty()) return emptyList()

        val col = rows.first().withIndex().associate { (i, name) -> name.trim() to i }
        rows.drop(1).filter { it.size > 1 }.map { r ->
            fun cell(name: String) = col[name]?.let { r.getOrNull(it) }.orEmpty()
            Report(
                reportId = cell("reportId"),
                title = cell("title"),
                description = cell("description"),
                assignedTo = cell("assignedTo"),
                photoUrl = cell("photoUrl"),
                building = cell("building"),
                room = cell("room"),
                category = cell("category")
            )
        }
    }

    // IDs look like MT-1001, MT-1002, ...
    private fun nextReportId(): String {
        val highest = readAll()
            .mapNotNull { it.reportId.removePrefix(ID_PREFIX).toIntOrNull() }
            .maxOrNull() ?: (FIRST_ID - 1)
        return "$ID_PREFIX${highest + 1}"
    }

    private fun Report.toCsvRow(): String = listOf(
        reportId, title, description, assignedTo, photoUrl, building, room, category
    ).joinToString(",") { escape(it) }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /** Minimal CSV parser that handles quoted fields, escaped quotes and embedded newlines. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') {
                            field.append('"'); i++
                        } else inQuotes = false
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.clear() }
                c == '\r' -> Unit
                c == '\n' -> {
                    row.add(field.toString()); field.clear()
                    rows.add(row); row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString()); rows.add(row)
        }
        return rows
    }

    companion object {
        const val FILE_NAME = "reports.csv"
        private const val NEWLINE = "\r\n"
        private const val ID_PREFIX = "MT-"
        private const val FIRST_ID = 1001

        val HEADER = listOf(
            "reportId", "title", "description", "assignedTo",
            "photoUrl", "building", "room", "category","status"
        )
    }
}