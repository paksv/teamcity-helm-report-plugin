package jetbrains.buildServer.helmReport.diffParser

import jetbrains.buildServer.helmReport.jsonOutput.model.HelmChange
import java.util.regex.Pattern


// ===== Parser =====
object HelmDiffParser {

    private val headerRe = Pattern.compile(
        // example: default, mijsql-mysql, Secret (v1) has changed:
        """^\s*([^,]+),\s+([^,]+),\s+(.+?)\s+\(([^)]+)\)\s+has\s+(?:been\s+)?(added|removed|changed):\s*$"""
    )

    fun parse(diff: String): List<HelmChange> {
        val lines = diff.lineSequence().toList()
        val sections = splitIntoSections(lines)
        return sections.mapNotNull { parseSection(it) }
    }

    // A contiguous block beginning with a header line until just before the next header
    private fun splitIntoSections(lines: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var cur = mutableListOf<String>()
        for (line in lines) {
            if (isHeader(line)) {
                if (cur.isNotEmpty()) result += cur.toList()
                cur = mutableListOf()
            }
            cur.add(line)
        }
        if (cur.isNotEmpty()) result += cur.toList()
        return result.filter { it.isNotEmpty() }
    }

    private fun isHeader(line: String) = headerRe.matcher(line).find()

    private data class Header(
        val namespace: String,
        val name: String,
        val kind: String,
        val groupLabel: String, // e.g., v1, apps, batch
        val change: String
    )

    private fun parseHeader(line: String): Header? {
        val m = headerRe.matcher(line)
        if (!m.find()) return null
        return Header(
            namespace = m.group(1).trim(),
            name = m.group(2).trim(),
            kind = m.group(3).trim(),
            groupLabel = m.group(4).trim(),
            change = m.group(5).trim() // added | removed | changed
        )
    }

    private fun parseSection(sectionLines: List<String>): HelmChange? {
        val header = parseHeader(sectionLines.first()) ?: return null
        val body = sectionLines.drop(1)

        val apiVersion = extractApiVersion(body)

        // Determine change type purely from diff content, not from header text
        var hasPlus = false
        var hasMinus = false
        var topLevelKindMinus = false
        var topLevelKindPlus = false
        for (raw in body) {
            val (pref, content) = splitPrefix(raw)
            val hasContent = content.isNotBlank()
            if (pref == '+' && hasContent) hasPlus = true
            if (pref == '-' && hasContent) hasMinus = true

            // Track top-level kind changes (only when there is actual content)
            if (hasContent) {
                val indent = content.takeWhile { it == ' ' }.length
                if (indent == 0 && content.startsWith("kind:")) {
                    if (pref == '-') topLevelKindMinus = true
                    if (pref == '+') topLevelKindPlus = true
                }
            }
            if (hasPlus && hasMinus && topLevelKindMinus) break
        }

        // Special rule: consider the resource removed if top-level field "kind" was removed and never added within the current scope
        if (topLevelKindMinus && !topLevelKindPlus) {
            val snippet = buildFullFromPrefixed(body, '-')
            return HelmChange.Removed(header.namespace, header.name, header.kind, apiVersion, snippet)
        }

        return when {
            hasPlus && !hasMinus -> {
                val snippet = buildFullFromPrefixed(body, '+')
                HelmChange.Added(header.namespace, header.name, header.kind, apiVersion, snippet)
            }
            !hasPlus && hasMinus -> {
                val snippet = buildFullFromPrefixed(body, '-')
                HelmChange.Removed(header.namespace, header.name, header.kind, apiVersion, snippet)
            }
            hasPlus && hasMinus -> {
                val unified = buildUnifiedDiff(body)
                HelmChange.Modified(header.namespace, header.name, header.kind, apiVersion, unified)
            }
            else -> null
        }
    }

    private fun extractApiVersion(body: List<String>): String? {
        // Prefer explicit apiVersion from any line (strip +/- prefixes). Falls back to null.
        for (raw in body) {
            val line = stripDiffPrefix(raw) ?: continue
            val idx = line.indexOf(':')
            if (idx > 0 && line.substring(0, idx).trim() == "apiVersion") {
                return line.substring(idx + 1).trim().trim('"', '\'')
            }
        }
        return null
    }

    // Build snippet for added/removed keeping raw diff lines with the desired prefix (+/-).
    // Empty lines (including +/- lines with no content) are skipped.
    private fun buildFullFromPrefixed(body: List<String>, prefixChar: Char): String {
        val out = StringBuilder()
        for (raw in body) {
            val (pref, content) = splitPrefix(raw)
            if (pref == prefixChar && content.isNotBlank()) {
                out.append(raw).append('\n')
            }
        }
        return out.toString().trimEnd()
    }

    private fun stripDiffPrefix(raw: String): String? {
        val (pref, content) = splitPrefix(raw)
        return when (pref) {
            '+', '-', ' ' -> content
            else -> if (pref == null) raw else content
        }
    }

    private fun splitPrefix(raw: String): Pair<Char?, String> {
        if (raw.isEmpty()) return null to raw
        val first = raw.first()
        return if (first == '+' || first == '-' ) {
            // Remove leading "+ " or "- " if present
            if (raw.length >= 2 && raw[1] == ' ') first to raw.substring(2)
            else first to raw.substring(1)
        } else {
            null to raw
        }
    }

    // Build a minimal but valid YAML subtree for one side of a change (only + or only - lines),
    // including the nearest ancestor keys to preserve structure.
    private fun buildMinimalChangedSubtree(body: List<String>, takePrefix: Char): String {
        // Collect change hunks (consecutive lines that start with + or -)
        data class Line(val text: String, val indent: Int)
        data class Hunk(val lines: List<Line>, val minIndent: Int, val anchorIndex: Int)

        fun indentOf(content: String): Int = content.takeWhile { it == ' ' }.length

        val contentLines = body.map { it }
        val hunks = mutableListOf<Hunk>()
        var i = 0
        while (i < contentLines.size) {
            val (pref, content) = splitPrefix(contentLines[i])
            if (pref == takePrefix && content.isNotBlank()) {
                val buf = mutableListOf<Line>()
                var j = i
                while (j < contentLines.size) {
                    val (p, c) = splitPrefix(contentLines[j])
                    if (p == takePrefix && c.isNotBlank()) {
                        buf += Line(c, indentOf(c))
                        j++
                    } else break
                }
                val minIndent = buf.minOf { it.indent }
                hunks += Hunk(buf.toList(), minIndent, i)
                i = j
            } else {
                i++
            }
        }
        if (hunks.isEmpty()) return ""

        // For each hunk, find ancestor key chain (from previous context lines) up to root
        val result = StringBuilder()
        hunks.forEachIndexed { idx, hunk ->
            if (idx > 0) result.append('\n')
            val ancestors = findAncestors(contentLines, hunk.anchorIndex, hunk.minIndent)
            ancestors.forEach { result.append(it).append('\n') }
            hunk.lines.forEach { result.append(it.text).append('\n') }
        }
        return result.toString().trimEnd()
    }

    // Return the original YAML diff piece as-is (excluding the header), preserving all symbols (+/-) and indentation.
    // We only trim leading/trailing completely empty lines; interior lines are kept verbatim.
    private fun buildUnifiedDiff(body: List<String>): String {
        if (body.isEmpty()) return ""
        var start = 0
        var end = body.size - 1
        // Trim leading empty lines
        while (start <= end && body[start].isBlank()) start++
        // Trim trailing empty lines
        while (end >= start && body[end].isBlank()) end--
        if (start > end) return ""
        val slice = body.subList(start, end + 1)
        // Join exactly as present
        return slice.joinToString("\n")
    }

    // Walk backwards from anchorIndex to find the nearest parent keys with indent < minIndent.
    // Include them in ascending indent order to form a proper YAML path.
    private fun findAncestors(all: List<String>, anchorIndex: Int, minIndent: Int): List<String> {
        // Collect candidate context lines (non +/-) above the hunk
        val context = mutableListOf<Pair<Int, String>>() // indent to line
        var i = anchorIndex - 1
        while (i >= 0) {
            val (pref, content) = splitPrefix(all[i])
            val isContext = pref == null && content.trim().isNotEmpty()
            if (isContext) {
                val trimmed = content.trimEnd()
                val ind = trimmed.takeWhile { it == ' ' }.length
                // We care about keys ending with ":" and with indent < minIndent
                val isKey = trimmed.trim().endsWith(":")
                if (isKey && ind < minIndent) {
                    context += ind to trimmed
                    // keep searching for higher ancestors
                }
            }
            // Stop if we reached another header-ish separator (heuristic: a blank line after a comment)
            i--
        }
        // Reduce to one key per indent (nearest above)
        val byIndent = context
            .groupBy { it.first }
            .mapValues { (_, v) -> v.first().second }
            .toList()
            .sortedBy { it.first }
            .map { it.second }
        return byIndent
    }
}



