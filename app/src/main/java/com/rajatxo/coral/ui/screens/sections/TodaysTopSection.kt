package com.rajatxo.coral.ui.screens.sections

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.rajatxo.coral.data.prefs.PlaybackHistory
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TodaysTopSection(
    modifier: Modifier = Modifier,
    songs: List<Song> = emptyList(),
    onCapsuleClick: (Song) -> Unit = {}
) {
    val dailyPlays by PlaybackHistory.dailyPlays.collectAsState()
    val topSongs: List<Song> = remember(dailyPlays, songs) {
        dailyPlays.take(6).mapNotNull { entry -> songs.find { it.id == entry.songId } }
    }
    val placeholderColors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF6B9EFF),
        Color(0xFFFF8FAB), Color(0xFF51CF66), Color(0xFFB197FC)
    )
    val context = LocalContext.current
    var slotColors by remember { mutableStateOf<List<Color>>(List(6) { Color(0xFF333333) }) }
    LaunchedEffect(topSongs) {
        val colors = withContext(Dispatchers.IO) {
            val result = mutableListOf<Color>()
            for (i in 0 until 6) {
                val song = topSongs.getOrNull(i)
                val artUri = song?.albumArtUri
                if (artUri != null) { result.add(extractDominantColor(context, artUri)) }
                else { result.add(getVibrantFallbackColor(i)) }
            }
            result
        }
        slotColors = colors
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Today's Top", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = CalSansFamily)
            Icon(imageVector = CoralXIcons.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 3) {
                val song1 = topSongs.getOrNull(i * 2)
                val song2 = topSongs.getOrNull(i * 2 + 1)
                val color1 = slotColors.getOrNull(i * 2) ?: placeholderColors[i * 2]
                val color2 = slotColors.getOrNull(i * 2 + 1) ?: placeholderColors[i * 2 + 1]
                val capsuleShape = RoundedCornerShape(28.dp)
                Box(
                    modifier = Modifier.weight(1f).height(56.dp).clip(capsuleShape)
                        .background(Brush.horizontalGradient(listOf(color1.copy(alpha = 0.25f), color2.copy(alpha = 0.25f))))
                        .border(2.5.dp, Brush.horizontalGradient(listOf(color1, color2)), capsuleShape)
                        .padding(6.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { (song1 ?: song2)?.let { onCapsuleClick(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArtCircle(artUri = song1?.albumArtUri, placeholderColor = color1)
                        AlbumArtCircle(artUri = song2?.albumArtUri, placeholderColor = color2)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArtCircle(artUri: Uri?, placeholderColor: Color) {
    val context = LocalContext.current
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(placeholderColor.copy(alpha = 0.7f)).border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
        if (artUri != null) { AsyncImage(model = ImageRequest.Builder(context).data(artUri).build(), contentDescription = null, modifier = Modifier.fillMaxWidth()) }
        else { Text(text = "+", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

private fun extractDominantColor(context: android.content.Context, uri: Uri): Color {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return getVibrantFallbackColor(0)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return getVibrantFallbackColor(0)
        inputStream.close()
        val pixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(android.graphics.Color.red(pixel), android.graphics.Color.green(pixel), android.graphics.Color.blue(pixel), hsv)
        hsv[1] = (hsv[1] * 1.3f).coerceAtMost(1f)
        val boosted = android.graphics.Color.HSVToColor(hsv)
        Color(android.graphics.Color.red(boosted) / 255f, android.graphics.Color.green(boosted) / 255f, android.graphics.Color.blue(boosted) / 255f)
    } catch (_: Exception) { getVibrantFallbackColor(0) }
}

private fun getVibrantFallbackColor(index: Int): Color {
    val c = listOf(Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF6B9EFF), Color(0xFFFF8FAB), Color(0xFF51CF66), Color(0xFFB197FC))
    return c[index % c.size]
}
