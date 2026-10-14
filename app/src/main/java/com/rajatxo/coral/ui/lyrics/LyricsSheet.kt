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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
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

    // ★ Lyrics font size — persisted in SharedPreferences. Range: 18-36sp.
    //   Default 26sp (matches old Coral). User can change via 3-dot menu.
    val prefs = remember { context.getSharedPreferences("coral_prefs", android.content.Context.MODE_PRIVATE) }
    var lyricsFontSize by remember {
        mutableStateOf(prefs.getFloat("lyrics_font_size_sp", 26f))
    }
    fun setLyricsFontSize(size: Float) {
        lyricsFontSize = size
        prefs.edit().putFloat("lyrics_font_size_sp", size).apply()
    }

    // ★ Font size dialog state
    var showFontSizeDialog by remember { mutableStateOf(false) }

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
                        // ★ Font Size — opens a slider to adjust lyrics text size
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Text Size",
                                    color = Color.White,
                                    fontFamily = CalSansFamily
                                )
                            },
                            onClick = {
                                showMenu = false
                                showFontSizeDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    CoralXIcons.Gear,
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
                    // ★ Use accompanist KaraokeLyricsView — same approach as Lyricify.
                    //   AutoParser parses raw lyrics text → SyncedLyrics → KaraokeLyricsView renders.
                    //   No custom rendering, no sanitizers, no preprocessors.
                    //   Falls back to CinematicLyricsContent for unsynced plain text.
                    if (lyric!!.synced && lyric!!.rawText != null) {
                        AccompanistLyricsContent(
                            rawText = lyric!!.rawText!!,
                            currentPositionMs = currentPositionMs,
                            isPlaying = isPlaying,
                            fontSizeSp = lyricsFontSize,
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

        // ★ Font size dialog — slider to adjust lyrics text size (18-40sp)
        if (showFontSizeDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showFontSizeDialog = false },
                containerColor = Color(0xFF1F1F1F),
                titleContentColor = Color.White,
                title = {
                    Text(
                        "Text Size",
                        color = Color.White,
                        fontFamily = CalSansFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Adjust the lyrics text size",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = CalSansFamily
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("A", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = CalSansFamily)
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.material3.Slider(
                                value = lyricsFontSize,
                                onValueChange = { setLyricsFontSize(it) },
                                valueRange = 18f..40f,
                                steps = 21,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("A", color = Color.White, fontSize = 20.sp, fontFamily = CalSansFamily)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${lyricsFontSize.toInt()}sp",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontFamily = CalSansFamily,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showFontSizeDialog = false }
                    ) {
                        Text("Done", color = Color.White, fontFamily = CalSansFamily)
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

// ═══ Accompanist lyrics content (Lyricify approach) ═══

/**
 * ★ AccompanistLyricsContent — renders lyrics using accompanist's KaraokeLyricsView.
 *
 * This is the EXACT same approach Lyricify uses:
 *   1. AutoParser.Builder().build().parse(rawText) → SyncedLyrics
 *   2. withFrameNanos loop for smooth position interpolation
 *   3. KaraokeLyricsView renders everything (word sync, multi-person, etc.)
 *
 * NO custom rendering. NO sanitizers. NO preprocessors.
 * Trust the library — it works in Lyricify, it works in BitChord, it works
 * in ArchiveTune. The crashes we had before were from OUR data conversion
 * being wrong, not the library being broken.
 */
@Composable
private fun AccompanistLyricsContent(
    rawText: String,
    currentPositionMs: Long,
    isPlaying: Boolean,
    fontSizeSp: Float,
    onSeek: (Long) -> Unit
) {
    // ★ Step 1: Parse raw lyrics using AutoParser
    //   Same as Lyricify: AutoParser().parse(rawText)
    var parseError by remember { mutableStateOf<String?>(null) }
    val syncedLyrics: com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics =
        remember(rawText) {
            try {
                val result = com.mocharealm.accompanist.lyrics.core.parser.AutoParser()
                    .parse(rawText)
                // ★ Check if we got KaraokeLine entries (word-level) or just SyncedLine (line-level)
                val hasKaraoke = result.lines.any { it is com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine }
                if (!hasKaraoke && result.lines.isNotEmpty()) {
                    // AutoParser returned only line-level lines — try our LrcParser
                    // which correctly parses word timestamps from Enhanced LRC + TTML
                    val ourLines = com.rajatxo.coral.data.lyrics.LrcParser.parse(rawText)
                    if (ourLines.any { it.hasWordSync }) {
                        // Our parser found word sync — convert manually
                        val lyric = com.rajatxo.coral.data.lyrics.Lyric(
                            synced = true,
                            lines = ourLines,
                            source = com.rajatxo.coral.data.lyrics.LyricSource.MANUAL,
                            rawText = rawText
                        )
                        return@remember convertToSyncedLyrics(lyric)
                    }
                }
                result
            } catch (e: Exception) {
                // AutoParser crashed — try our LrcParser as fallback
                parseError = e.message ?: "Unknown error"
                try {
                    val ourLines = com.rajatxo.coral.data.lyrics.LrcParser.parse(rawText)
                    if (ourLines.isNotEmpty()) {
                        val lyric = com.rajatxo.coral.data.lyrics.Lyric(
                            synced = ourLines.any { it.timeMs >= 0 },
                            lines = ourLines,
                            source = com.rajatxo.coral.data.lyrics.LyricSource.MANUAL,
                            rawText = rawText
                        )
                        return@remember convertToSyncedLyrics(lyric)
                    }
                } catch (_: Exception) { }
                com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics(emptyList())
            }
        }

    if (syncedLyrics.lines.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = CoralXIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Could not parse lyrics",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = CalSansFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    // ★ Step 2: Smooth position interpolation — same as Lyricify.
    //   withFrameNanos loop advances position when playing.
    //   When paused, position stays at the last reported value.
    var animatedPosition by remember { mutableLongStateOf(currentPositionMs) }
    val latestPosition by rememberUpdatedState(currentPositionMs)
    val latestPlaying by rememberUpdatedState(isPlaying)
    var lastUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(currentPositionMs) {
        lastUpdateTime = System.currentTimeMillis()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val elapsed = System.currentTimeMillis() - lastUpdateTime
                animatedPosition = latestPosition + elapsed
                withFrameNanos {}
            }
        } else {
            animatedPosition = latestPosition
        }
    }

    // ★ Step 3: Render with KaraokeLyricsView — same as Lyricify.
    val listState = rememberLazyListState()

    com.mocharealm.accompanist.lyrics.ui.composable.lyrics.KaraokeLyricsView(
        listState = listState,
        lyrics = syncedLyrics,
        currentPosition = { animatedPosition.toInt() },
        onLineClicked = { line -> onSeek(line.start.toLong()) },
        onLinePressed = { },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        textColor = Color.White,
        normalLineTextStyle = TextStyle(
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CalSansFamily
        ),
        accompanimentLineTextStyle = TextStyle(
            fontSize = (fontSizeSp * 0.7f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = CalSansFamily
        ),
        useBlurEffect = false,
        offset = 100.dp
    )
}

// ═══ Helper: convert our Lyric model to accompanist's SyncedLyrics ═══

/**
 * Convert our Lyric model → accompanist's SyncedLyrics.
 * Used as fallback when AutoParser returns only line-level (no KaraokeLine).
 * Our LrcParser correctly parses word timestamps from Enhanced LRC + TTML,
 * so if our parser found word sync, we convert manually.
 */
private fun convertToSyncedLyrics(
    lyric: Lyric
): com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics {
    val lines = mutableListOf<com.mocharealm.accompanist.lyrics.core.model.ISyncedLine>()

    lyric.lines.forEachIndexed { index, line ->
        val startMs = line.timeMs.toInt().coerceAtLeast(0)
        val nextStart = lyric.lines.getOrNull(index + 1)?.timeMs?.toInt() ?: (startMs + 3000)
        val endMs = nextStart.coerceAtLeast(startMs + 1)

        if (line.hasWordSync && line.words != null && line.words.isNotEmpty()) {
            // Word-synced → KaraokeLine with syllables
            val syllables = line.words.map { word ->
                com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable(
                    content = word.text + " ",
                    start = word.startTime.toInt().coerceAtLeast(0),
                    end = word.endTime.toInt().coerceAtLeast(word.startTime.toInt() + 1)
                )
            }
            try {
                lines.add(
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
            } catch (_: Exception) {
                // If KaraokeLine creation fails (end < start), fall back to SyncedLine
                lines.add(
                    com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine(
                        content = line.text.ifBlank { "♪" },
                        translation = null,
                        start = startMs,
                        end = endMs
                    )
                )
            }
        } else {
            // Line-level only → SyncedLine
            lines.add(
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
        lines = lines,
        title = lyric.trackName ?: "",
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
