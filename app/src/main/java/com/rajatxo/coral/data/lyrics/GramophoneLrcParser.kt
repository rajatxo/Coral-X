package com.rajatxo.coral.data.lyrics

import android.util.Log
import org.akanework.gramophone.logic.utils.LrcUtils
import org.akanework.gramophone.logic.utils.SemanticLyrics

/**
 * ★ GramophoneLrcParser — uses Gramophone's robust LrcUtils parser.
 *
 * Copied from SpatialFlow (github.com/MythicalSHUB/SpatialFlow, Apache 2.0).
 * SpatialFlow delegates parsing to Gramophone's high-fidelity LrcUtils which
 * correctly handles:
 *   - Standard LRC
 *   - Enhanced LRC (word-by-word sync with <mm:ss.xx> timestamps)
 *   - TTML (Apple Music format with <span begin="X"> tags)
 *   - Multi-line timestamps
 *   - Voice/agent tags (v1:, v2:)
 *   - Background vocals (bg:)
 *
 * This parser is more robust than our old regex-based LrcParser.
 */
object GramophoneLrcParser {

    private const val TAG = "GramophoneLrcParser"
    private const val INTERLUDE_THRESHOLD_MS = 5000L

    /**
     * Parse lyrics content into a list of [LyricLine]s.
     * Handles LRC, Enhanced LRC, and TTML formats.
     *
     * @param lrcContent The raw lyrics text (LRC, Enhanced LRC, or TTML)
     * @return List of parsed LyricLines with word timestamps, or empty list on failure
     */
    fun parse(lrcContent: String?): List<LyricLine> {
        if (lrcContent.isNullOrEmpty()) {
            return emptyList()
        }

        val parserOptions = LrcUtils.LrcParserOptions(trim = true, multiLine = true, errorText = null)
        val semanticLyrics = try {
            LrcUtils.parseLyrics(lrcContent, null, parserOptions, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing lyrics via Gramophone parser", e)
            null
        } ?: return emptyList()

        val parsedLines = mutableListOf<LyricLine>()

        if (semanticLyrics is SemanticLyrics.SyncedLyrics) {
            for (line in semanticLyrics.text) {
                val words = line.words?.map { word ->
                    val wordText = if (word.charRange.first in 0 until line.text.length &&
                        word.charRange.last in 0 until line.text.length &&
                        word.charRange.first <= word.charRange.last) {
                        line.text.substring(word.charRange)
                    } else {
                        ""
                    }
                    val duration = (word.endInclusive?.toLong()?.minus(word.begin.toLong()))
                        ?.coerceAtLeast(0L) ?: 0L
                    WordTimestamp(
                        text = wordText,
                        startTime = word.begin.toLong(),
                        endTime = word.begin.toLong() + duration.coerceAtLeast(120L)
                    )
                } ?: emptyList()

                parsedLines.add(
                    LyricLine(
                        timeMs = line.start.toLong(),
                        text = line.text,
                        words = if (words.isNotEmpty()) words else null
                    )
                )
            }
        } else if (semanticLyrics is SemanticLyrics.UnsyncedLyrics) {
            for (line in semanticLyrics.unsyncedText) {
                parsedLines.add(
                    LyricLine(
                        timeMs = -1L,
                        text = line.first,
                        words = null
                    )
                )
            }
        }

        // Sort by start time
        parsedLines.sortBy { it.timeMs }

        // Insert interlude markers for instrumental gaps
        return insertInterludes(parsedLines)
    }

    /**
     * Insert "♪" interlude markers between lines that have a large time gap.
     */
    private fun insertInterludes(sorted: List<LyricLine>): List<LyricLine> {
        if (sorted.size < 2) return sorted

        val result = mutableListOf<LyricLine>()

        for (i in sorted.indices) {
            val current = sorted[i]

            if (current.text.isEmpty() && i > 0) {
                val nextTime = if (i + 1 < sorted.size) sorted[i + 1].timeMs else current.timeMs
                val prevTime = sorted[i - 1].timeMs
                if (nextTime - prevTime > INTERLUDE_THRESHOLD_MS) {
                    result.add(LyricLine(current.timeMs, "♪", null))
                    continue
                }
                continue
            }

            result.add(current)

            if (i < sorted.size - 1) {
                val next = sorted[i + 1]
                val gap = next.timeMs - current.timeMs
                if (gap > INTERLUDE_THRESHOLD_MS) {
                    val interludeTime = current.timeMs + (gap / 2)
                    result.add(LyricLine(interludeTime, "♪", null))
                }
            }
        }

        return result
    }
}
