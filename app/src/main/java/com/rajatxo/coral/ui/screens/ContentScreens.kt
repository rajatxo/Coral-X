package com.rajatxo.coral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.foundation.Dimensions
import com.rajatxo.coral.ui.foundation.LocalCoralXWindowInsets

@Composable
fun QuickPicksScreen() {
    val items = listOf(
        "Blinding Lights" to "The Weeknd",
        "Starboy" to "The Weeknd",
        "Save Your Tears" to "The Weeknd",
        "After Hours" to "The Weeknd",
        "Die For You" to "The Weeknd",
        "Call Out My Name" to "The Weeknd",
        "The Hills" to "The Weeknd",
        "Can't Feel My Face" to "The Weeknd",
        "I Feel It Coming" to "The Weeknd",
        "Pray For Me" to "The Weeknd"
    )
    LazyColumn(
        contentPadding = LocalCoralXWindowInsets.current.asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (title, artist) -> SongCard(title, artist) }
    }
}

@Composable
fun SongsScreen() {
    val items = listOf(
        "Song One" to "Artist A",
        "Song Two" to "Artist B",
        "Song Three" to "Artist C",
        "Song Four" to "Artist D",
        "Song Five" to "Artist E",
        "Song Six" to "Artist F",
        "Song Seven" to "Artist G",
        "Song Eight" to "Artist H"
    )
    LazyColumn(
        contentPadding = LocalCoralXWindowInsets.current.asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (title, artist) -> SongCard(title, artist) }
    }
}

@Composable
fun PlaylistsScreen() {
    val playlists = listOf(
        "Chill Mix" to 12,
        "Workout" to 25,
        "Road Trip" to 30,
        "Late Night" to 8,
        "Favorites" to 45
    )
    LazyColumn(
        contentPadding = LocalCoralXWindowInsets.current.asPaddingValues(),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists) { (name, count) -> PlaylistCard(name, count) }
    }
}

@Composable
private fun SongCard(title: String, artist: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.ContentPaddingHorizontal)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "♪", color = Color.White.copy(alpha = 0.5f), fontSize = 20.sp)
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(text = artist, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlaylistCard(name: String, songCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.ContentPaddingHorizontal)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "♫", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp)
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(text = "$songCount songs", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }
    }
}
