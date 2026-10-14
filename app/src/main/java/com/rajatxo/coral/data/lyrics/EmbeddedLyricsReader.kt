package com.rajatxo.coral.data.lyrics

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import aman.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * ★ EmbeddedLyricsReader — reads lyrics embedded in audio files via TagLib.
 *
 * TagLib's JNI API takes a FILE PATH (String), not a content:// URI or file
 * descriptor. On Android 10+ (scoped storage), we get content:// URIs from
 * MediaStore — TagLib can't open those directly.
 *
 * SOLUTION: copy the audio file to a temp cache file, pass that path to
 * TagLib, return the lyrics, then delete the temp file.
 *
 * This does NOT use MediaMetadataRetriever — on many devices MMR grabs a
 * native file handle that conflicts with ExoPlayer's audio track and
 * silences playback. TagLib reads the file independently (separate native
 * handle, separate file descriptor), so it's safe to call while a song is
 * playing.
 *
 * Supported tag formats (read by TagLib):
 *  - MP3: ID3v2 UNSYNCEDLYRICS / SYNCEDLYRICS / USLT frames
 *  - FLAC: LYRICS Vorbis comment
 *  - M4A/AAC: ©lyr MOV atom
 *  - OGG/OPUS: LYRICS Vorbis comment
 *  - WAV: ID3 chunk
 *  - WMA/APE: as supported by TagLib
 *
 * @param context App context (for cacheDir + contentResolver)
 */
class EmbeddedLyricsReader(private val context: Context) {

    /**
     * Read raw embedded lyrics text from the audio file at [uri].
     *
     * @return Raw lyrics text (may be plain text, LRC, Enhanced LRC, or TTML),
     *         or null if no lyrics are embedded or reading failed.
     */
    suspend fun readEmbeddedLyrics(uri: Uri): String? = withContext(Dispatchers.IO) {
        if (uri == Uri.EMPTY) return@withContext null

        // 1. Try to get a real file path first (Android 9 and below, or files
        //    the app has direct access to). This avoids the temp-file copy.
        val realPath = uri.path?.takeIf { File(it).exists() }
        if (realPath != null) {
            return@withContext extractLyricsFromPath(realPath)
        }

        // 2. content:// URI — copy to temp file, then extract
        val tempFile = try {
            copyToTempFile(uri) ?: return@withContext null
        } catch (_: Exception) {
            return@withContext null
        }

        try {
            extractLyricsFromPath(tempFile.absolutePath)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Copy the audio file at [uri] to a temp file in cacheDir.
     * Returns the temp File, or null on failure.
     */
    private fun copyToTempFile(uri: Uri): File? {
        return try {
            val tempFile = File.createTempFile("embedded_lyrics_", ".audio", context.cacheDir)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd: ParcelFileDescriptor ->
                FileInputStream(pfd.fileDescriptor).use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } ?: return null
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract lyrics from a real file path via TagLib.
     * Tries all known lyrics tag keys (different formats store under different keys).
     */
    private fun extractLyricsFromPath(path: String): String? {
        return try {
            val rawMap = TagLib.getMetadata(path) ?: return null
            if (rawMap.isEmpty()) return null

            // TagLibHelper priority order (from Catalog's TagLibHelper.extractLyrics):
            // LYRICS → USLT → UNSYNCEDLYRICS → "UNSYNCED LYRICS" → ©LYR
            val lyrics = rawMap["LYRICS"]
                ?: rawMap["USLT"]
                ?: rawMap["UNSYNCEDLYRICS"]
                ?: rawMap["UNSYNCED LYRICS"]
                ?: rawMap["©LYR"]
                ?: rawMap["\u00A9LYR"]  // ©LYR as Java-escaped

            if (lyrics.isNullOrBlank()) null else lyrics
        } catch (_: Exception) {
            null
        }
    }
}
