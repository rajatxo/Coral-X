package com.rajatxo.coral.ui.lyrics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.coral.data.lyrics.LrcParser
import com.rajatxo.coral.data.lyrics.Lyric
import com.rajatxo.coral.data.lyrics.LyricLine
import com.rajatxo.coral.data.lyrics.LyricSource
import com.rajatxo.coral.data.lyrics.LyricsRepository
import com.rajatxo.coral.data.lyrics.WordTimestamp
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.util.CoralPalette
import com.rajatxo.coral.util.extractPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import com.rajatxo.coral.ui.util.consistentNavBarPadding

/**
 * Cinematic lyrics sheet — ArchiveTune-inspired.
 *
 * Features:
 *  1. Solid palette-based gradient background (tertiary → darker tertiary)
 *     — no visible bands or horizontal lines.
 *  2. Word-by-word karaoke animation (sweep fill + bounce + glow)
 *  3. Progressive opacity by distance from active line
 *  4. Smooth auto-scroll
 *  5. Tap-to-seek on any line
 *  6. 3-dot menu with: Close, Fetch, Search, Import LRC
 *
 * Lyrics priority:
 *  1. Embedded lyrics (from audio metadata, passed in as parameter)
 *  2. Manually imported .lrc file (saved via LyricsRepository.saveImportedLrc)
 *  3. Fetched from LrcLib (only on manual user action)
 */
@Composable
fun LyricsSheet(
    trackName: String,
    artistName: String,
    albumName: String?,
    durationMs: Long?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    albumArtUri: Uri? = null,
    embeddedLyrics: String? = null,
    onLyricsFetched: ((Lyric?) -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { LyricsRepository(context) }

    // Back button: close lyrics page (not the whole player)
    BackHandler { onDismiss() }
    val coroutineScope = rememberCoroutineScope()

    var lyric by remember { mutableStateOf<Lyric?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var palette by remember { mutableStateOf(CoralPalette.Default) }

    // Menu + dialog state
    var showMenu by remember { mutableStateOf(false) }

    // ★ Lyrics picker overlay state — when true, opens the LyricsPicker
    //   full-screen overlay showing all LrcLib candidates for the song.
    //   User can pick the candidate whose duration matches their local file.
    var showLyricsPicker by remember { mutableStateOf(false) }

    // Paste-lyrics dialog state — when the user picks "Paste Lyrics"
    // from the menu, this dialog opens with a multi-line text field
    // where they can paste LRC-formatted lyrics (with [mm:ss.xxx]
    // timestamps). On confirm, the text is saved via saveImportedLrc
    // (same path as importing an .lrc file).
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    // File picker for LRC import — accepts any text file, tries to parse as LRC
    val lrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val lrcText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
                if (lrcText.isNullOrBlank()) {
                    errorMessage = "Could not read the file"
                } else {
                    val saved = repository.saveImportedLrc(trackName, artistName, lrcText)
                    if (saved != null) {
                        lyric = saved
                        onLyricsFetched?.invoke(saved)
                        errorMessage = null
                    } else {
                        errorMessage = "Failed to parse lyrics. Make sure it's a valid LRC file."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to import: ${e.message ?: "unknown error"}"
            }
            isLoading = false
        }
    }

    // ─── Initial load: embedded > imported > cached fetched ───
    // ★ Priority order (per user request):
    //   1. EMBEDDED — lyrics read straight from the audio file via TagLib
    //      (the user's own file's lyrics — TTML, ID3 SYLT/USLT, Vorbis LYRICS).
    //      This is the PRIMARY source. Skipped if albumArtUri is null (can't
    //      open the file).
    //   2. IMPORTED (.imported.json) — user pasted or .lrc-imported lyrics.
    //      Secondary authority — user picked these on purpose.
    //   3. CACHED fetched — fallback from a previous network fetch.
    //
    // The auto-fetch chain in Spiral2Player (embedded → imported → cached →
    // LrcLib → KuGou → NetEase) populates `embeddedLyrics` with whatever it
    // found, so we just trust that here. The sheet doesn't do its own network
    // fetch — user must trigger Fetch / Pick / Import / Paste via the menu.
    LaunchedEffect(trackName, artistName, embeddedLyrics) {
        isLoading = false
        errorMessage = null

        // 1. Embedded lyrics (passed in from the player's auto-fetch chain)
        when {
            !embeddedLyrics.isNullOrBlank() -> {
                val lines = withContext(Dispatchers.IO) { LrcParser.parse(embeddedLyrics) }
                if (lines.isNotEmpty()) {
                    val hasWordSync = lines.any { it.hasWordSync }
                    val hasTimestamps = lines.any { it.timeMs >= 0 }
                    lyric = Lyric(
                        synced = hasTimestamps,
                        lines = lines,
                        source = LyricSource.EMBEDDED,
                        trackName = trackName,
                        artistName = artistName,
                        hasWordSync = hasWordSync,
                        rawText = embeddedLyrics
                    )
                } else {
                    lyric = null
                }
            }
            // 2. Imported (pasted / .lrc file) — secondary priority
            else -> {
                val imported = withContext(Dispatchers.IO) {
                    repository.getImportedLrc(trackName, artistName)
                }
                if (imported != null) {
                    lyric = imported
                    return@LaunchedEffect
                }
                // 3. Cached fetched lyrics (from a previous Fetch/Search action)
                val cached = withContext(Dispatchers.IO) {
                    repository.getCachedLyrics(trackName, artistName)
                }
                lyric = cached
            }
        }
    }

    // Palette extraction from album art
    LaunchedEffect(albumArtUri) {
        if (albumArtUri != null) {
            extractPalette(context, albumArtUri)?.let { palette = it }
        }
    }

    // ─── Actions ───


    // ═══ Background: solid palette gradient (tertiary → darker tertiary) ═══
    // Single smooth vertical gradient — no bands, no visible lines.
    val topColor = palette.tertiary
    val bottomColor = lerp(palette.tertiary, Color.Black, 0.4f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(topColor, bottomColor)
                    )
                )
        )

        // ═══ Content ═════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .consistentNavBarPadding()
        ) {
            // ─── Header: thumbnail · title · 3-dot menu ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (albumArtUri != null) {
                    AsyncImage(
                        model = albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = CalSansFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = artistName,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontFamily = CalSansFamily,
                        maxLines = 1
                    )
                }
                // 3-dot menu button
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CoralXIcons.Ellipsis,
                            contentDescription = "More options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1F1F1F))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Close",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDismiss()
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.ChevronDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Fetch Lyrics",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val fetched = repository.fetchFromNetwork(
                                            trackName, artistName, albumName, durationMs
                                        )
                                        if (fetched != null) {
                                            lyric = fetched
                                            onLyricsFetched?.invoke(fetched)
                                        } else {
                                            errorMessage = "No lyrics found online"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to fetch: ${e.message}"
                                    }
                                    isLoading = false
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.Unplug,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        // ★ Pick Lyrics — opens the LyricsPicker overlay
                        //   showing all LrcLib candidates. User picks one
                        //   based on duration match. Solves the wrong-sync
                        //   problem when a song has multiple releases.
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Pick Lyrics",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                showLyricsPicker = true
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.MousePointerClick,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Import LRC File",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                lrcPicker.launch(arrayOf("*/*"))
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.FolderInput,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Paste Lyrics",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                pasteText = ""
                                showPasteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.ClipboardType,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ─── Lyrics content ──────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Searching...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontFamily = CalSansFamily
                            )
                        }
                    }
                }
                lyric != null -> {
                    // ★ Use our own SweptLyricsContent for word-synced lyrics.
                    //   BitChord-inspired approach: two stacked Text layers (dim base
                    //   + bright clipped to sung portion) with a horizontal sweep
                    //   clip. No accompanist library — too fragile (crashes on
                    //   overlapping timelines, empty syllables, multi-agent edge
                    //   cases). Our renderer is simple + robust.
                    //   Falls back to CinematicLyricsContent for unsynced plain text.
                    if (lyric!!.synced) {
                        SweptLyricsContent(
                            lyric = lyric!!,
                            currentPositionMs = currentPositionMs,
                            onSeek = onSeek
                        )
                    } else {
                        CinematicLyricsContent(
                            lyric = lyric!!,
                            currentPositionMs = currentPositionMs,
                            onSeek = onSeek
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = CoralXIcons.Music,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No lyrics found",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = CalSansFamily,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Use the menu to import an LRC file",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontFamily = CalSansFamily,
                                textAlign = TextAlign.Center
                            )
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    errorMessage!!,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontFamily = CalSansFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Paste Lyrics dialog ─────────────────────────────────────
        // Opens when the user picks "Paste Lyrics" from the 3-dot menu.
        // Multi-line text field where they can paste LRC-formatted lyrics
        // (with [mm:ss.xxx] timestamps). On Save, the text is parsed and
        // stored via saveImportedLrc — same path as importing an .lrc
        // file, so the lyrics show up immediately and persist for
        // offline use on future plays of this song.
        if (showPasteDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showPasteDialog = false
                    pasteText = ""
                },
                containerColor = Color(0xFF1F1F1F),
                titleContentColor = Color.White,
                title = {
                    Text(
                        "Paste Lyrics",
                        color = Color.White,
                        fontFamily = CalSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Paste LRC-formatted lyrics below. Lines with [mm:ss.xxx] timestamps will be synced.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = CalSansFamily
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            placeholder = {
                                Text(
                                    "[00:22.170]Mon eke eke dui\n[00:26.370]Ekakar ami-tui\n...",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 13.sp,
                                    fontFamily = CalSansFamily
                                )
                            },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = CalSansFamily
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2A2A2A),
                                unfocusedContainerColor = Color(0xFF2A2A2A),
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (pasteText.isNotBlank()) {
                                val textToSave = pasteText
                                showPasteDialog = false
                                pasteText = ""
                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val saved = repository.saveImportedLrc(trackName, artistName, textToSave)
                                        if (saved != null) {
                                            lyric = saved
                                            onLyricsFetched?.invoke(saved)
                                        } else {
                                            errorMessage = "Failed to parse lyrics. Make sure lines have [mm:ss.xxx] timestamps."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to save: ${e.message}"
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Save", color = Color.White, fontFamily = CalSansFamily)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showPasteDialog = false
                            pasteText = ""
                        }
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontFamily = CalSansFamily)
                    }
                }
            )
        }

        // ★ Lyrics Picker overlay — shown when user taps "Pick Lyrics"
        //   from the 3-dot menu. User picks a candidate, we convert it
        //   to a Lyric (applying the auto-offset so a +2s candidate syncs
        //   perfectly to the song's actual timeline), cache it, and swap
        //   into the active lyric state.
        if (showLyricsPicker) {
            LyricsPicker(
                trackName = trackName,
                artistName = artistName,
                albumName = albumName,
                durationMs = durationMs,
                onDismiss = { showLyricsPicker = false },
                onCandidateSelected = { candidate, offsetMs ->
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        // ★ Pass offsetMs to candidateToLyric — it shifts
                        //   all line + word timestamps by this amount so
                        //   the candidate's timeline fits the user's song.
                        val lyricFromCandidate = repository.candidateToLyric(candidate, offsetMs)
                        if (lyricFromCandidate != null) {
                            // Cache the offset-applied lyric under the song's
                            // actual name so it auto-loads next time.
                            repository.cacheLyricsPublic(trackName, artistName, lyricFromCandidate)
                            lyric = lyricFromCandidate
                            onLyricsFetched?.invoke(lyricFromCandidate)
                            showLyricsPicker = false
                            // ★ Show a brief confirmation message with the offset applied
                            errorMessage = if (offsetMs == 0L) {
                                "✓ Perfect sync — no offset needed"
                            } else {
                                val offsetSec = offsetMs / 1000.0
                                val sign = if (offsetMs > 0) "+" else ""
                                "✓ Synced with ${sign}${"%.1f".format(offsetSec)}s offset"
                            }
                        } else {
                            errorMessage = "Could not parse that candidate's lyrics"
                        }
                        isLoading = false
                    }
                }
            )
        }
    }
}

// ═══ Swept lyrics content (BitChord-inspired, NO accompanist library) ═══

/**
 * ★ SweptLyricsContent — our own word-by-word karaoke renderer.
 *
 * Inspired by BitChord's SweptLyricLine approach:
 *   - Frame clock via withFrameNanos — interpolates between player readings
 *     (every ~500ms) so the sweep updates at 60fps. Without this, the sweep
 *     "jumps" every 500ms → not smooth.
 *   - Two stacked Text layers per line:
 *     1. Dim base (white at low alpha) — full line
 *     2. Bright overlay (full white) — clipped to sung portion via horizontalGradient
 *   - animatesFromMs / animatesUntilMs — each line has a time window when it's
 *     actively animating. Outside that window, it's drawn statically (fully dim
 *     or fully bright). Only the active line shows the moving sweep.
 *   - NO accompanist library — pure Compose, can't crash.
 *   - Uses our existing Lyric model (LyricLine.words: List<WordTimestamp>)
 *
 * Line-synced lyrics (no word sync): NO sweep animation — just highlight the
 * active line + dim others. The sweep is for word-synced lyrics only.
 */
@Composable
private fun SweptLyricsContent(
    lyric: Lyric,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit
) {
    if (!lyric.synced || lyric.lines.isEmpty()) {
        CinematicLyricsContent(lyric, currentPositionMs, onSeek)
        return
    }

    // ★ Active line index — binary search for the current line
    val activeIndex = remember(lyric.lines, currentPositionMs) {
        findActiveLineIndex(lyric.lines, currentPositionMs)
    }

    // ★ Frame clock — interpolates between player readings so the sweep
    //   updates at 60fps. The player reports position every ~500ms; without
    //   interpolation, the sweep "jumps" every 500ms → not smooth.
    //   Keyed on lyric so the clock resets when the song changes.
    val clockMs = rememberFrameClock(lyric, currentPositionMs)

    val listState = rememberLazyListState()

    // Auto-scroll: active line at ~1/3 from top
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyric.lines.size) {
            val target = (activeIndex - 3).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(lyric.lines.size) { index ->
            val line = lyric.lines[index]
            val distance = kotlin.math.abs(index - activeIndex)
            val isActive = index == activeIndex
            // ★ isBefore: this line has already been sung (index < active).
            //   Used to show fully bright (no sweep) for past lines.
            val isBefore = index < activeIndex
            SweptLyricLine(
                line = line,
                clockMs = clockMs,
                isActive = isActive,
                distanceFromActive = distance,
                isBefore = isBefore,
                onSeek = onSeek
            )
        }
    }
}

/**
 * ★ Frame clock — interpolates between player readings so the sweep updates
 *   at 60fps (buttery smooth). The player reports position every ~500ms;
 *   without interpolation, the sweep "jumps" every 500ms.
 *
 * Returns a State<Long> that updates every frame (~16ms) with the interpolated
 * playback position. Read inside draw lambdas so only the draw phase re-runs
 * each frame (not composition).
 *
 * BitChord's LyricClock is more sophisticated (steers rather than sets, learns
 * playback rate, handles seeks). This is a simpler version: linear
 * interpolation between the last two readings. Good enough for smoothness.
 */
@Composable
private fun rememberFrameClock(lyric: Lyric, currentPositionMs: Long): androidx.compose.runtime.State<Long> {
    // ★ Anchor: the last reported position + when it was reported.
    //   Stored in mutableStateOf so the produceState block can read the
    //   latest value via snapshotFlow.
    val anchor = remember(lyric) {
        mutableStateOf(currentPositionMs to System.nanoTime())
    }
    // Update the anchor when a new reading arrives
    if (currentPositionMs != anchor.value.first) {
        anchor.value = currentPositionMs to System.nanoTime()
    }

    // ★ Frame-by-frame interpolation. withFrameNanos fires every vsync (~16ms).
    //   We compute: displayed = lastReported + (now - lastReportedAt)
    //   This advances the position by exactly the elapsed wall-clock time.
    //   Reading anchor.value inside the lambda keeps it subscribed to changes.
    return androidx.compose.runtime.produceState(
        initialValue = currentPositionMs,
        lyric
    ) {
        while (true) {
            withFrameNanos { _ ->
                val (reportedMs, reportedAtNanos) = anchor.value
                val nowNanos = System.nanoTime()
                val elapsedMs = (nowNanos - reportedAtNanos) / 1_000_000.0
                value = reportedMs + elapsedMs.toLong()
            }
        }
    }
}

/**
 * A single lyric line with the sung part lit, the rest dimmed, and the boundary
 * travelling across the words in time with the vocal.
 *
 * Two copies of the same text stacked:
 *   1. Dim base (white at alpha based on distance from active line)
 *   2. Bright overlay (full white) clipped to sung portion via horizontalGradient
 *
 * ★ Phase-based drawing (BitChord approach):
 *   - BEFORE (clock < line start): all dim, no sweep → static, no redraw
 *   - MOVING (line start <= clock <= line end): sweep active → redraws per frame
 *   - AFTER (clock > line end): all bright, no sweep → static, no redraw
 *   Only MOVING phase reads the clock → only the active line redraws per frame.
 *
 * ★ Line-synced lyrics (no word sync): NO sweep animation. Just highlight the
 *   active line + dim others. The sweep is for word-synced lyrics only —
 *   applying a fake sweep to line-synced lyrics looks wrong because the sweep
 *   doesn't match the actual word boundaries.
 */
@Composable
private fun SweptLyricLine(
    line: LyricLine,
    clockMs: androidx.compose.runtime.State<Long>,
    isActive: Boolean,
    distanceFromActive: Int,
    isBefore: Boolean,
    onSeek: (Long) -> Unit
) {
    // Progressive opacity by distance from active line
    val targetAlpha = when {
        isActive -> 1f
        distanceFromActive == 1 -> 0.5f
        distanceFromActive == 2 -> 0.3f
        distanceFromActive == 3 -> 0.18f
        else -> 0.1f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (isActive) 330 else 500),
        label = "lineAlpha"
    )

    // Scale: active = 1.0, inactive = 0.96
    val targetScale = if (isActive) 1f else 0.96f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(166),
        label = "lineScale"
    )

    val fontSize = 24.sp
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold

    // ★ Determine if this line should show the sweep animation.
    //   - ONLY for word-synced lyrics (Enhanced LRC, TTML with word spans)
    //   - ONLY when the line is actively being sung (isActive = true)
    //   - Line-synced lyrics: NO sweep (just highlight + dim)
    val showSweep = line.hasWordSync && line.words != null && line.words.isNotEmpty()

    // ★ Read the clock ONLY when this line is actively animating (active + word-synced).
    //   Outside the animation window, the line is static (fully dim or fully
    //   bright) and doesn't need to read the clock → doesn't redraw per frame.
    val isAnimating = isActive && showSweep
    val currentPos = if (isAnimating) clockMs.value else 0L

    // ★ Compute the sung fraction (0..1) for the sweep clip.
    //   - Word-synced + active: compute from clock (sweep)
    //   - Word-synced + not active: 1.0 if before (already sung), 0.0 if after
    //   - Line-synced: 0 (no sweep — highlight via dim/bright only)
    val sungFraction = when {
        !showSweep -> 0f  // Line-synced: no sweep
        isAnimating -> computeSungFraction(line, currentPos)
        isBefore -> 1f    // Past word-synced line: fully sung
        else -> 0f        // Future word-synced line: not sung yet
    }

    // The text to display (♪ for instrumental gaps)
    val text = line.text.ifBlank { "♪" }

    // ★ Layout result captured from the dim Text — used to compute the sweep
    //   clip rectangle based on character positions
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // ★ Determine if the bright overlay should be shown at all.
    //   - Word-synced: show if sungFraction > 0 (with sweep clip if < 1)
    //   - Line-synced + active: show full bright (highlight)
    //   - Line-synced + isBefore: show full bright (already sung)
    //   - Line-synced + future: don't show bright (dim only)
    val showBright = when {
        showSweep -> sungFraction > 0f
        isActive -> true
        isBefore -> true
        else -> false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (line.timeMs >= 0) onSeek(line.timeMs) }
    ) {
        // Dim base layer (full line, low alpha)
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = fontSize,
            fontFamily = CalSansFamily,
            fontWeight = fontWeight,
            lineHeight = (fontSize.value * 1.3f).sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Visible,
            onTextLayout = { layoutResult = it },
            modifier = Modifier.fillMaxWidth()
        )
        // Bright overlay layer — only drawn if there's something sung/highlighted
        if (showBright) {
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSize,
                fontFamily = CalSansFamily,
                fontWeight = fontWeight,
                lineHeight = (fontSize.value * 1.3f).sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (showSweep && sungFraction < 1f) {
                            // Word-synced + partially sung: apply sweep clip
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    val measured = layoutResult
                                    if (measured != null && sungFraction < 1f) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colorStops = arrayOf(
                                                    0f to Color.Black,
                                                    sungFraction to Color.Black,
                                                    (sungFraction + 0.05f).coerceAtMost(1f) to Color.Transparent,
                                                    1f to Color.Transparent
                                                )
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                        } else {
                            // Line-synced or fully sung: no clip (full bright)
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * Compute the fraction (0..1) of characters sung at [positionMs] for a line
 * with word sync. Each word contributes its character count, and we sum how
 * many characters are sung across all words.
 *
 * For a word: 0 before start, 1 after end, linear in between.
 */
private fun computeSungFraction(line: LyricLine, positionMs: Long): Float {
    val words = line.words ?: return 0f
    if (words.isEmpty()) return 0f

    var totalChars = 0
    var sungChars = 0f

    words.forEach { word ->
        val wordLen = word.text.length.coerceAtLeast(1)
        totalChars += wordLen
        when {
            positionMs >= word.endTime -> sungChars += wordLen
            positionMs <= word.startTime -> sungChars += 0f
            else -> {
                val span = (word.endTime - word.startTime).coerceAtLeast(1L)
                val frac = (positionMs - word.startTime).toFloat() / span
                sungChars += wordLen * frac
            }
        }
    }

    return if (totalChars == 0) 0f else (sungChars / totalChars).coerceIn(0f, 1f)
}

// ═══ Accompanist lyrics content (word-synced karaoke via accompanist-lyrics-ui) ═══

/**
 * ★ AccompanistLyricsContent — renders word-synced lyrics using accompanist's
 *   KaraokeLyricsView. Uses accompanist's AutoParser to parse the raw lyrics
 *   text (TTML, Enhanced LRC, YRC, KRC, Lyricify Syllable) directly into a
 *   SyncedLyrics model — bypassing our LrcParser entirely. This preserves
 *   multi-person / duet structure, word-level timing, and accompaniment lines
 *   that our LrcParser flattens.
 *
 * AutoParser chain (from accompanist-lyrics-core):
 *   TTMLParser → LyricifySyllableParser → EnhancedLrcParser → KugouKrcParser → NeteaseYrcParser
 *
 * ★ CRASH FIX: After parsing, we sanitize the SyncedLyrics to ensure all
 *   syllable and line end times are >= their start times. Real-world lyrics
 *   (especially Enhanced LRC with multi-person `v1:` prefixes) often have
 *   overlapping timelines — a word's end timestamp may exceed the next line's
 *   start. accompanist's KaraokeSyllable has `init { require(end >= start) }`
 *   which throws on these cases. The sanitizer clamps all ends to be >= starts.
 *
 * Falls back to our CinematicLyricsContent if:
 *   - rawText is null (Lyric was created without preserving source text)
 *   - AutoParser returns empty SyncedLyrics
 *   - AutoParser or sanitizer throws
 *
 * ★ Auto-scroll fix: accompanist's derivedStateOf reads currentPosition via
 *   the lambda we pass. The lambda must read from a Compose State (not a plain
 *   parameter) for derivedStateOf to track it. We wrap currentPositionMs in
 *   rememberUpdatedState so the lambda reads from the State.
 */
@Composable
private fun AccompanistLyricsContent(
    lyric: Lyric,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit
) {
    val rawText = lyric.rawText

    if (rawText.isNullOrBlank()) {
        // No raw text to parse with AutoParser — fall back to cinematic renderer
        CinematicLyricsContent(lyric, currentPositionMs, onSeek)
        return
    }

    // ★ Pre-process the raw text to fix common format issues that crash AutoParser:
    //   1. Enhanced LRC with trailing end timestamps: `<00:05.27>you<00:06.00>`
    //      The trailing `<00:06.00>` has no text after it → AutoParser creates
    //      an empty syllable → KaraokeSyllable init throws or rendering crashes.
    //      Fix: strip trailing `<timestamp>` at end of each line if no text follows.
    //   2. Enhanced LRC with `v1:` / `v2:` voice prefix before word timestamps.
    //      AutoParser's EnhancedLrcParser handles this, but sometimes the prefix
    //      causes empty syllable creation. The preprocessor ensures clean format.
    val cleanedText = preprocessLyricsText(rawText)

    // ★ Use accompanist's AutoParser to parse the cleaned text directly.
    //   Then sanitize to fix overlapping timelines that crash KaraokeLyricsView.
    val syncedLyrics: com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics =
        remember(cleanedText) {
            try {
                val parsed = com.mocharealm.accompanist.lyrics.core.parser.AutoParser().parse(cleanedText)
                sanitizeSyncedLyrics(parsed)
            } catch (_: Exception) {
                // AutoParser or sanitizer failed — return empty, will fall back below
                com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics(emptyList())
            }
        }

    if (syncedLyrics.lines.isEmpty()) {
        // AutoParser couldn't parse the format — fall back to cinematic
        CinematicLyricsContent(lyric, currentPositionMs, onSeek)
        return
    }

    // ★ Wrap currentPositionMs in a Compose State so accompanist's derivedStateOf
    //   tracks changes. Without this, the lambda { currentPositionMs.toInt() }
    //   captures the value at first composition and never updates.
    val positionState = androidx.compose.runtime.rememberUpdatedState(currentPositionMs.toInt())

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView(
        listState = listState,
        lyrics = syncedLyrics,
        currentPosition = { positionState.value },
        onLineClicked = { line -> onSeek(line.start.toLong()) },
        onLinePressed = { /* no-op for now */ },
        modifier = Modifier.fillMaxSize(),
        textColor = Color.White,
        normalLineTextStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CalSansFamily
        ),
        accompanimentLineTextStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = CalSansFamily
        ),
        offset = 80.dp,
        useBlurEffect = false  // blur is expensive on low-end devices
    )
}

/**
 * ★ Pre-process raw lyrics text to fix format issues that crash AutoParser.
 *
 * FIX 1: Enhanced LRC trailing end timestamps
 *   Format: `[00:03.35]<00:03.35>Last <00:03.80>night, ... <00:05.27>you<00:06.00>`
 *   The trailing `<00:06.00>` is an END timestamp with no text after it.
 *   AutoParser creates an empty syllable from this → crash.
 *   Fix: strip trailing `<timestamp>` at end of each line if no text follows.
 *
 * FIX 2: Ensure spaces between Enhanced LRC word timestamps
 *   Format: `<00:03.35>Last<00:03.80>night` (no space between word and next timestamp)
 *   → accompanist concatenates words without spaces → large visual gap or no gap.
 *   Fix: add a space between a word's last char and the next `<timestamp>`.
 */
private fun preprocessLyricsText(rawText: String): String {
    return rawText.lines().joinToString("\n") { line ->
        var cleaned = line.trimEnd()

        // Skip non-LRC lines (plain text, TTML, etc.) — TTML doesn't have [
        if (!cleaned.contains("[") || !cleaned.contains("<")) {
            return@joinToString cleaned
        }

        // FIX 1: Strip trailing `<timestamp>` at end of line with no text after it.
        while (true) {
            val lastAngleClose = cleaned.lastIndexOf('>')
            if (lastAngleClose < 0) break
            val afterClose = cleaned.substring(lastAngleClose + 1).trim()
            if (afterClose.isNotEmpty()) break  // There IS text after the last >
            val lastAngleOpen = cleaned.lastIndexOf('<', lastAngleClose)
            if (lastAngleOpen < 0) break
            val inner = cleaned.substring(lastAngleOpen + 1, lastAngleClose)
            if (!inner.matches(Regex("""\d{1,2}[:.]\d{2}(?:[.:]\d{1,3})?"""))) break
            cleaned = cleaned.substring(0, lastAngleOpen).trimEnd()
        }

        // FIX 2: Ensure a space between a word's last character and the next
        // `<timestamp>` tag. Pattern: `word<00:03.80>` → `word <00:03.80>`.
        // Only matches when `<` is followed by a digit (timestamp start), not XML.
        cleaned = cleaned.replace(Regex("(\\S)<(\\d)"), "$1 <$2")

        cleaned
    }
}

/**
 * ★ Sanitize a SyncedLyrics to prevent crashes in KaraokeLyricsView.
 *
 * Real-world lyrics (especially Enhanced LRC with multi-person `v1:` prefixes)
 * often have overlapping timelines:
 *   [02:00.05] v1: ... <02:06.24>      ← last word ends at 02:06.24
 *   [02:04.97] v1: <02:04.97>There's   ← next line starts at 02:04.97
 *
 * accompanist's KaraokeSyllable and KaraokeLine both have:
 *   init { require(end >= start) }
 *
 * If a word's end gets clamped to the next line's start (which is before the
 * word's own start), require() throws and the app crashes.
 *
 * This function:
 *   1. For each syllable: clamp end to be at least start + 1ms
 *   2. For each line: clamp end to be at least start + 1ms
 *   3. Rebuilds KaraokeLine.MainKaraokeLine with sanitized values
 *
 * Inspired by ArchiveTune's clampEnhancedLrcLastWordEnds.
 */
private fun sanitizeSyncedLyrics(
    lyrics: com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
): com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics {
    val sanitizedLines = lyrics.lines.map { line ->
        when (line) {
            is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.MainKaraokeLine -> {
                val safeSyllables = line.syllables.map { s ->
                    val safeStart = s.start.coerceAtLeast(0)
                    val safeEnd = s.end.coerceAtLeast(safeStart + 1)
                    s.copy(start = safeStart, end = safeEnd)
                }
                val safeStart = line.start.coerceAtLeast(0)
                val safeEnd = line.end.coerceAtLeast(safeStart + 1)
                try {
                    line.copy(syllables = safeSyllables, start = safeStart, end = safeEnd)
                } catch (_: Exception) {
                    // If copy still fails (e.g. accompanimentLines constraint),
                    // rebuild from scratch with minimal safe values
                    com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.MainKaraokeLine(
                        syllables = safeSyllables,
                        translation = line.translation,
                        alignment = line.alignment,
                        start = safeStart,
                        end = safeEnd,
                        phonetic = line.phonetic,
                        accompanimentLines = null  // drop accompaniment lines if they cause issues
                    )
                }
            }
            is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.AccompanimentKaraokeLine -> {
                val safeSyllables = line.syllables.map { s ->
                    val safeStart = s.start.coerceAtLeast(0)
                    val safeEnd = s.end.coerceAtLeast(safeStart + 1)
                    s.copy(start = safeStart, end = safeEnd)
                }
                val safeStart = line.start.coerceAtLeast(0)
                val safeEnd = line.end.coerceAtLeast(safeStart + 1)
                try {
                    line.copy(syllables = safeSyllables, start = safeStart, end = safeEnd)
                } catch (_: Exception) {
                    com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.AccompanimentKaraokeLine(
                        syllables = safeSyllables,
                        translation = line.translation,
                        alignment = line.alignment,
                        start = safeStart,
                        end = safeEnd,
                        phonetic = line.phonetic
                    )
                }
            }
            is com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine -> {
                val safeStart = line.start.coerceAtLeast(0)
                val safeEnd = line.end.coerceAtLeast(safeStart + 1)
                line.copy(start = safeStart, end = safeEnd)
            }
            else -> line  // Unknown line type — pass through as-is
        }
    }
    return com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics(
        lines = sanitizedLines,
        title = lyrics.title,
        id = lyrics.id,
        artists = lyrics.artists
    )
}

/**
 * Convert our [Lyric] model → accompanist's [SyncedLyrics] model.
 *
 * DEPRECATED: Only used as a fallback when rawText is unavailable.
 * The primary path uses AutoParser directly on rawText (see AccompanistLyricsContent).
 *
 * - LyricLine with hasWordSync + words → KaraokeLine.MainKaraokeLine
 *   (with syllables derived from each WordTimestamp).
 * - LyricLine without words → SyncedLine (line-level only).
 *
 * The end time of each line is the start time of the NEXT line (or
 * start + 2000ms for the last line).
 */
private fun Lyric.toAccompanistSyncedLyrics(): com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics {
    val accompanistLines = mutableListOf<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>()

    lines.forEachIndexed { index, line ->
        val startMs = line.timeMs.toInt().coerceAtLeast(0)
        val endMs = (lines.getOrNull(index + 1)?.timeMs?.toInt()?.takeIf { it > startMs } ?: (startMs + 2000))
            .coerceAtLeast(startMs + 1)

        if (line.hasWordSync && line.words != null && line.words.isNotEmpty()) {
            val syllables = line.words.map { word ->
                com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable(
                    content = word.text,
                    start = word.startTime.toInt().coerceAtLeast(0),
                    end = word.endTime.toInt().coerceAtLeast(word.startTime.toInt() + 1)
                )
            }
            accompanistLines.add(
                com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine.MainKaraokeLine(
                    syllables = syllables,
                    translation = null,
                    alignment = com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment.Start,
                    start = startMs,
                    end = endMs,
                    phonetic = null,
                    accompanimentLines = null
                )
            )
        } else {
            accompanistLines.add(
                com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine(
                    content = line.text.ifBlank { "♪" },
                    translation = null,
                    start = startMs,
                    end = endMs
                )
            )
        }
    }

    return com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics(
        lines = accompanistLines,
        title = trackName ?: "",
        id = "0",
        artists = null
    )
}

// ═══ Cinematic lyrics content with word-by-word karaoke ═════════════

@Composable
private fun CinematicLyricsContent(
    lyric: Lyric,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit
) {
    if (!lyric.synced) {
        // Plain lyrics
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lyric.lines.size) { index ->
                Text(
                    text = lyric.lines[index].text,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    fontFamily = CalSansFamily,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    val activeIndex = remember(lyric.lines, currentPositionMs) {
        findActiveLineIndex(lyric.lines, currentPositionMs)
    }

    val listState = rememberLazyListState()

    // Auto-scroll: active line at ~1/3 from top
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyric.lines.size) {
            val targetScroll = (activeIndex - 3).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    // No top/bottom fade gradients — they showed as visible horizontal lines.
    // The solid palette background is enough for text legibility.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(lyric.lines.size) { index ->
            val line = lyric.lines[index]
            val distance = kotlin.math.abs(index - activeIndex)
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            CinematicLine(
                line = line,
                isActive = isActive,
                isPast = isPast,
                distanceFromActive = distance,
                currentPositionMs = currentPositionMs,
                onSeek = onSeek
            )
        }
    }
}

/**
 * A single cinematic lyric line.
 * - Active line: full opacity, 26sp, bold, word-by-word karaoke animation
 * - Past lines: 52% opacity (1 away), 30% (2), 18% (3), 10% (4+)
 * - Future lines: same opacity scale
 */
@Composable
private fun CinematicLine(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    distanceFromActive: Int,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit
) {
    // Progressive opacity by distance
    val targetAlpha = when {
        isActive -> 1f
        distanceFromActive == 1 -> 0.52f
        distanceFromActive == 2 -> 0.30f
        distanceFromActive == 3 -> 0.18f
        else -> 0.10f
    }
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(if (isActive) 330 else 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "lineAlpha"
    )

    // Scale: active = 1.0, inactive = 0.95
    val targetScale = if (isActive) 1f else 0.95f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(166, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "lineScale"
    )

    val fontSize = 26.sp
    val fontWeight = if (isActive || isPast) FontWeight.ExtraBold else FontWeight.SemiBold

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (line.timeMs >= 0) onSeek(line.timeMs) }
    ) {
        if (line.hasWordSync && line.words != null) {
            // ─── Word-by-word karaoke line (wraps naturally) ────────
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                line.words.forEachIndexed { index, word ->
                    AnimatedWord(
                        word = word,
                        currentPositionMs = currentPositionMs,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        isLineActive = isActive,
                        isLinePast = isPast,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
            }
        } else {
            // ─── Regular line (no word sync or inactive) ────────────
            Text(
                text = line.text.ifBlank { "♪" },
                color = Color.White,
                fontSize = fontSize,
                fontFamily = CalSansFamily,
                fontWeight = fontWeight,
                lineHeight = (fontSize.value * 1.35f).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A single animated word with karaoke sweep fill + bounce + glow.
 *
 * Animation details (from ArchiveTune LyricsV2.AnimatedWordV2):
 * - Sweep fill 0→1 with soft 8dp edge (BlendMode.DstIn)
 * - Bounce: scale 1 + 0.015 * sin(π * progress)
 * - Float: -4px * sin(π * progress)
 * - Glow: shadow alpha 0.45, radius 12
 * - Min sweep duration: 180ms, LinearEasing
 */
@Composable
private fun AnimatedWord(
    word: WordTimestamp,
    currentPositionMs: Long,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    isLineActive: Boolean = false,
    isLinePast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val wordStartMs = word.startTime
    val wordEndMs = word.endTime
    val isWordComplete = currentPositionMs >= wordEndMs
    val isWordActive = currentPositionMs >= wordStartMs && currentPositionMs < wordEndMs

    // Sweep fill 0→1
    val sweepAnimatable = remember(word) { Animatable(0f) }
    LaunchedEffect(isWordActive, isWordComplete, wordStartMs, wordEndMs) {
        when {
            isWordComplete && sweepAnimatable.value < 1f -> {
                sweepAnimatable.animateTo(1f, tween(80, easing = LinearEasing))
            }
            isWordActive -> {
                val remainingMs = (wordEndMs - currentPositionMs).coerceAtLeast(1L)
                sweepAnimatable.animateTo(1f, tween(maxOf(remainingMs, 180L).toInt(), easing = LinearEasing))
            }
            !isWordActive && !isWordComplete -> {
                sweepAnimatable.snapTo(0f)
            }
        }
    }
    val progress = if (isWordComplete) 1f else sweepAnimatable.value
    val sinProgress = sin(progress * PI).toFloat()

    // BOUNCE: scale 1.0 → 1.015 peak mid-word
    val wordScale = 1f + (0.015f * sinProgress)
    // FLOAT: -4px peak mid-word
    val targetFloat = if (isWordActive) -4f * sinProgress else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(if (isWordActive) 50 else 350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "wordFloat"
    )

    // GLOW: peaks at half-sweep, then holds
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val glowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val glowRadius = if (isWordActive) glowProgress * 12f else 0f

    val glowPadding = 10.dp
    val fillTransitionWidth = 8f

    val baseTextStyle = TextStyle(
        color = Color.White.copy(alpha = 0.35f),
        fontSize = fontSize,
        fontFamily = CalSansFamily,
        fontWeight = fontWeight
    )

    val fillShadow = if (isWordActive && glowAlpha > 0f) Shadow(
        color = Color.White.copy(alpha = glowAlpha),
        offset = androidx.compose.ui.geometry.Offset.Zero,
        blurRadius = glowRadius.coerceAtLeast(1f)
    ) else null

    val fillTextStyle = TextStyle(
        color = Color.White.copy(alpha = 1f),
        fontSize = fontSize,
        fontFamily = CalSansFamily,
        fontWeight = fontWeight
    ).let { if (fillShadow != null) it.copy(shadow = fillShadow) else it }

    Box(
        modifier = modifier
            .graphicsLayer {
                clip = false
                translationY = floatOffset * density.density
                scaleX = wordScale
                scaleY = wordScale
            }
    ) {
        // Base layer (dim)
        Text(
            text = word.text,
            style = baseTextStyle,
            modifier = Modifier.padding(glowPadding)
        )

        // Overlay layer (fill) — show if word is complete/active, or line is past
        if (progress > 0f || isWordComplete || isLinePast) {
            val showFullFill = isWordComplete || isLinePast || (isLineActive && !isWordActive && !isWordComplete)
            if (isWordActive && !isWordComplete) {
                // Karaoke sweep wipe with soft edge
                Text(
                    text = word.text,
                    style = fillTextStyle,
                    modifier = Modifier
                        .padding(glowPadding)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = fillTransitionWidth.dp.toPx()
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            } else {
                // Complete, past, or line active but word not started — full fill
                Text(
                    text = word.text,
                    style = fillTextStyle,
                    modifier = Modifier.padding(glowPadding)
                )
            }
        }
    }
}

/**
 * Find the active line index using binary search.
 */
private fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var lo = 0
    var hi = lines.lastIndex
    var result = -1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (lines[mid].timeMs in 0..positionMs) {
            result = mid
            lo = mid + 1
        } else if (lines[mid].timeMs > positionMs) {
            hi = mid - 1
        } else {
            lo = mid + 1
        }
    }
    return result
}
