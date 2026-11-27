package fr.cyclooo.opendeezer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cyclooo.opendeezer.R
import fr.cyclooo.opendeezer.engine.Playlist
import fr.cyclooo.opendeezer.engine.Track
import fr.cyclooo.opendeezer.player.PlayerController
import fr.cyclooo.opendeezer.ui.components.CenteredMessage
import fr.cyclooo.opendeezer.ui.components.Artwork
import fr.cyclooo.opendeezer.ui.components.TrackRow

/**
 * A scrollable track list. When [onBulkDownload] + [bulkDownloadLabel] are set an
 * overflow action offers to download the whole collection (album/playlist),
 * premium-gated like the per-track download.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    title: String,
    player: PlayerController,
    onBack: () -> Unit,
    onBulkDownload: (() -> Unit)? = null,
    bulkDownloadLabel: String? = null,
    playlist: Playlist? = null,
    load: suspend () -> List<Track>,
) {
    val tracks by produceState<List<Track>?>(initialValue = null, key1 = playlist?.id ?: title) {
        value = load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (playlist != null) stringResource(R.string.playlists_title) else title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (onBulkDownload != null && bulkDownloadLabel != null) {
                        BulkDownloadMenu(
                            label = bulkDownloadLabel,
                            premium = player.premium,
                            onDownload = onBulkDownload,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            val list = tracks
            if (playlist == null && !list.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { player.playQueue(list, 0) },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    text = { Text(stringResource(R.string.action_play_all)) },
                )
            }
        },
    ) { padding ->
        if (playlist == null) {
            TrackList(tracks, player, Modifier.fillMaxSize().padding(padding))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                if (maxWidth >= 900.dp && maxHeight >= 420.dp) {
                    val coverSize = minOf(maxWidth * 0.25f, maxHeight * 0.48f, 280.dp)
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        PlaylistHero(
                            playlist = playlist,
                            tracks = tracks,
                            player = player,
                            artworkSize = coverSize,
                            modifier = Modifier.weight(0.42f),
                        )
                        Surface(
                            modifier = Modifier.weight(0.58f).fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            TrackList(tracks, player, Modifier.fillMaxSize(), bottomSpacing = 12.dp)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        PlaylistHero(
                            playlist = playlist,
                            tracks = tracks,
                            player = player,
                            artworkSize = 112.dp,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            compact = true,
                        )
                        TrackList(tracks, player, Modifier.weight(1f), bottomSpacing = 12.dp)
                    }
                }
            }
        }
    }
}

/** Playlist identity and permanent playback actions; never overlays the track list. */
@Composable
private fun PlaylistHero(
    playlist: Playlist,
    tracks: List<Track>?,
    player: PlayerController,
    artworkSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp, Alignment.CenterVertically),
    ) {
        Artwork(playlist.artworkUrl.ifBlank { tracks?.firstOrNull()?.artworkUrl.orEmpty() }, Modifier.size(artworkSize), corner = 18)
        Text(
            playlist.name,
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        val count = tracks?.size ?: playlist.trackCount
        val details = listOfNotNull(
            playlist.owner.takeIf { it.isNotBlank() },
            count.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.n_tracks, it, it) },
        ).joinToString(" · ")
        if (details.isNotBlank()) {
            Text(
                details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    tracks?.takeIf { it.isNotEmpty() }?.let { list ->
                        player.setShuffle(false)
                        player.playQueue(list, 0)
                    }
                },
                enabled = !tracks.isNullOrEmpty(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_play_all))
            }
            OutlinedButton(
                onClick = {
                    tracks?.takeIf { it.isNotEmpty() }?.let { list ->
                        player.setShuffle(true)
                        player.playQueue(list, list.indices.random())
                    }
                },
                enabled = !tracks.isNullOrEmpty(),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_shuffle_play))
            }
        }
    }
}

/** Track list body (spinner / empty message / rows), reusable in fold-aware layouts. */
@Composable
fun TrackList(
    tracks: List<Track>?, player: PlayerController, modifier: Modifier = Modifier,
    bottomSpacing: androidx.compose.ui.unit.Dp = 88.dp,
) {
    Box(modifier) {
        when (val list = tracks) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> if (list.isEmpty()) {
                CenteredMessage(stringResource(R.string.tracklist_empty))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(list, key = { i, t -> "$i-${t.id}" }) { index, track ->
                        TrackRow(
                            track = track,
                            player = player,
                            onClick = { player.playQueue(list, index) },
                            onStartRadio = if (track.isEpisode) null else { { player.startTrackRadio(track) } },
                        )
                    }
                    item { Spacer(Modifier.height(bottomSpacing)) }
                }
            }
        }
    }
}

/** Overflow action for a whole-collection download, premium-gated with a hint. */
@Composable
private fun BulkDownloadMenu(label: String, premium: Boolean, onDownload: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = {
                if (premium) {
                    Text(label)
                } else {
                    Column {
                        Text(label)
                        Text(
                            stringResource(R.string.download_requires_premium),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
            enabled = premium,
            onClick = {
                menuOpen = false
                onDownload()
            },
        )
    }
}
