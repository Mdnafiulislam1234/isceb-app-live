package com.example.util

import java.util.Locale

data class ParsedBulkMcqQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val marks: Int,
    val explanation: String,
    val answerDetected: Boolean
)

data class BulkMcqParseResult(
    val questions: List<ParsedBulkMcqQuestion>,
    val detectedAnswerCount: Int
)

object BulkMcqQuestionParser {
    private val banglaDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private fun normalizeDigits(input: String): String =
        input.map { banglaDigits[it] ?: it }.joinToString("")

    private fun mapOptionChar(ch: String): Int = when (ch.trim().lowercase(Locale.ROOT)) {
        "a", "ক" -> 0
        "b", "খ" -> 1
        "c", "গ" -> 2
        "d", "ঘ" -> 3
        else -> -1
    }

    private fun stripOptionMarkers(raw: String): Pair<String, Boolean> {
        var text = raw.trim()
        var marked = false
        val leadingRegex = Regex("^[*✓✔✅☑]+\\s*")
        if (leadingRegex.containsMatchIn(text)) {
            marked = true
            text = text.replace(leadingRegex, "").trim()
        }
        val trailingTagRegex = Regex(
            "\\s*[(\\[]\\s*(?:correct(?:\\s*answer)?|right(?:\\s*answer)?|ans(?:wer)?|সঠিক(?:\\s*উত্তর)?)\\s*[)\\]]\\s*$",
            RegexOption.IGNORE_CASE
        )
        if (trailingTagRegex.containsMatchIn(text)) {
            marked = true
            text = text.replace(trailingTagRegex, "").trim()
        }
        val trailingSymbolRegex = Regex("\\s*[*✓✔✅☑]+\\s*$")
        if (trailingSymbolRegex.containsMatchIn(text)) {
            marked = true
            text = text.replace(trailingSymbolRegex, "").trim()
        }
        return text to marked
    }

    fun parse(rawText: String, defaultMarks: Int = 5): BulkMcqParseResult {
        val safeMarks = defaultMarks.coerceIn(1, 50)
        val lines = rawText.replace("\r\n", "\n").replace("\r", "\n").split("\n")

        val answerKeyStart = lines.withIndex().indexOfFirst { (idx, line) ->
            val trimmed = line.trim()
            val isKeyHeader = Regex(
                "^(?:answer\\s*key|answers\\s*key|উত্তরমালা|সঠিক\\s*উত্তরসমূহ)\\s*[:\\-–.]?\\s*.*$",
                RegexOption.IGNORE_CASE
            ).matches(trimmed) || Regex(
                "^(?:correct\\s*answers|answers)\\s*[:\\-–.]?\\s*$",
                RegexOption.IGNORE_CASE
            ).matches(trimmed)
            if (!isKeyHeader) false
            else {
                val tail = lines.subList(idx, lines.size)
                tail.none { Regex("^\\s*[(\\[]?[B-Db-dখগঘ][)\\].:]\\s+").containsMatchIn(it) }
            }
        }
        val bodyLines = if (answerKeyStart >= 0) lines.subList(0, answerKeyStart) else lines
        val answerKeyMap = mutableMapOf<Int, Int>()
        if (answerKeyStart >= 0) {
            val joinedKey = lines.subList(answerKeyStart, lines.size).joinToString(" ")
            val pairRegex = Regex(
                "(?:Q(?:uestion)?\\.?\\s*#?\\s*|প্রশ্ন\\s*)?([0-9০-৯]{1,4})\\s*[)\\].:\\-–]?\\s*[(\\[]?([A-Da-dকখগঘ])[)\\].]?"
            )
            for (m in pairRegex.findAll(joinedKey)) {
                val num = normalizeDigits(m.groupValues[1]).toIntOrNull()
                val opt = mapOptionChar(m.groupValues[2])
                if (num != null && num in 1..500 && opt in 0..3) {
                    answerKeyMap[num] = opt
                }
            }
        }

        data class RawQuestion(
            val number: Int,
            val promptLines: MutableList<String> = mutableListOf(),
            val options: MutableList<String> = mutableListOf("", "", "", ""),
            var activeOpt: Int = -1,
            var explicitAns: Int = -1,
            var explicitAnsText: String = "",
            val expLines: MutableList<String> = mutableListOf(),
            var inExp: Boolean = false,
            var sawAnsLine: Boolean = false
        )

        val collected = mutableListOf<RawQuestion>()
        var current: RawQuestion? = null
        val pendingStem = mutableListOf<String>()

        val qHeaderRegex = Regex(
            "^(?:(Q(?:uestion)?\\.?\\s*#?\\s*|প্রশ্ন\\s*))?([0-9০-৯]{1,4})\\s*[)\\].:\\-]\\s*(.*)$",
            RegexOption.IGNORE_CASE
        )
        val ansRegex = Regex(
            "^(?:correct\\s*answer|right\\s*answer|answer|ans|সঠিক\\s*উত্তর|উত্তর)\\s*[:.\\-–]?\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )
        val expRegex = Regex(
            "^(?:explanation|rationale|note|solution|ব্যাখ্যা)\\s*[:\\-–.]\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )
        val optLineRegex = Regex(
            "^\\*?\\s*[(\\[]?([A-Da-dকখগঘ])[)\\].:]\\s*\\*?\\s*(.+)$"
        )

        for (rawLine in bodyLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            if (Regex("^(?:page\\s+\\d+(?:\\s*(?:of|/)\\s*\\d+)?|[-_=]{3,})$", RegexOption.IGNORE_CASE).matches(trimmed)) {
                continue
            }

            val expMatch = expRegex.matchEntire(trimmed)
            if (expMatch != null && current != null) {
                current.expLines.add(expMatch.groupValues[1].trim())
                current.inExp = true
                continue
            }

            val ansMatch = ansRegex.matchEntire(trimmed)
            if (ansMatch != null && current != null && current.options.any { it.isNotBlank() }) {
                val body = ansMatch.groupValues[1].trim()
                val letterMatch = Regex("^(?:option\\s*)?[(\\[]?([A-Da-dকখগঘ])(?:[)\\].:\\s\\-]|$)", RegexOption.IGNORE_CASE)
                    .find(body)
                if (letterMatch != null) {
                    val idx = mapOptionChar(letterMatch.groupValues[1])
                    if (idx in 0..3) current.explicitAns = idx
                } else {
                    current.explicitAnsText = body
                }
                current.sawAnsLine = true
                current.inExp = false
                continue
            }

            val qMatch = qHeaderRegex.matchEntire(trimmed)
            if (qMatch != null) {
                val hasWord = qMatch.groupValues[1].isNotBlank()
                val qNum = normalizeDigits(qMatch.groupValues[2]).toIntOrNull()
                val rest = qMatch.groupValues[3].trim()
                val expectedNext = collected.size + (if (current != null) 2 else 1)
                val currHasOpts = current != null && current.options.count { it.isNotBlank() } >= 2
                if (qNum != null && qNum in 1..500 &&
                    (current == null || currHasOpts || hasWord || qNum == expectedNext || current.sawAnsLine)
                ) {
                    current?.let { collected.add(it) }
                    current = RawQuestion(number = qNum).apply {
                        if (rest.isNotBlank()) promptLines.add(rest)
                    }
                    pendingStem.clear()
                    continue
                }
            }

            val optMatch = optLineRegex.matchEntire(trimmed)
            if (optMatch != null) {
                val optIdx = mapOptionChar(optMatch.groupValues[1])
                if (optIdx in 0..3) {
                    if (current == null) {
                        current = RawQuestion(number = collected.size + 1).apply {
                            if (pendingStem.isNotEmpty()) promptLines.add(pendingStem.joinToString(" "))
                        }
                        pendingStem.clear()
                    }
                    val (cleanOpt, isMarked) = stripOptionMarkers(optMatch.groupValues[2])
                    current.options[optIdx] = cleanOpt
                    current.activeOpt = optIdx
                    current.inExp = false
                    if (isMarked || trimmed.startsWith("*")) current.explicitAns = optIdx
                    continue
                }
            }

            when {
                current != null && current.inExp -> current.expLines.add(trimmed)
                current != null && current.sawAnsLine -> pendingStem.add(trimmed)
                current != null && current.activeOpt >= 0 -> {
                    val (cleanCont, isMarked) = stripOptionMarkers(trimmed)
                    current.options[current.activeOpt] = "${current.options[current.activeOpt]} $cleanCont".trim()
                    if (isMarked) current.explicitAns = current.activeOpt
                }
                current != null && pendingStem.isEmpty() -> current.promptLines.add(trimmed)
                else -> pendingStem.add(trimmed)
            }
        }
        current?.let { collected.add(it) }

        var detectedCount = 0
        val parsed = collected.mapIndexedNotNull { idx, raw ->
            val prompt = raw.promptLines.joinToString(" ").replace(Regex("\\s+"), " ").trim()
            val opts = raw.options.map { it.replace(Regex("\\s+"), " ").trim() }
            if (prompt.isBlank() || opts.count { it.isNotBlank() } < 2) return@mapIndexedNotNull null

            var resolved = raw.explicitAns
            if (resolved !in 0..3 && raw.explicitAnsText.isNotBlank()) {
                val needle = raw.explicitAnsText.lowercase(Locale.ROOT)
                resolved = opts.indexOfFirst { it.isNotBlank() && it.lowercase(Locale.ROOT) == needle }
                if (resolved < 0) {
                    resolved = opts.indexOfFirst {
                        it.isNotBlank() && (it.lowercase(Locale.ROOT).contains(needle) || needle.contains(it.lowercase(Locale.ROOT)))
                    }
                }
            }
            if (resolved !in 0..3) resolved = answerKeyMap[raw.number] ?: -1
            if (resolved !in 0..3) resolved = answerKeyMap[idx + 1] ?: -1

            val detected = resolved in 0..3
            if (detected) detectedCount += 1

            ParsedBulkMcqQuestion(
                prompt = prompt,
                options = opts,
                correctIndex = if (detected) resolved else 0,
                marks = safeMarks,
                explanation = raw.expLines.joinToString(" ").replace(Regex("\\s+"), " ").trim(),
                answerDetected = detected
            )
        }

        return BulkMcqParseResult(questions = parsed, detectedAnswerCount = detectedCount)
    }
}
