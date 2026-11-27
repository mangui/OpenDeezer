package fr.cyclooo.opendeezer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cyclooo.opendeezer.R
import fr.cyclooo.opendeezer.Routes
import fr.cyclooo.opendeezer.engine.Engine
import fr.cyclooo.opendeezer.engine.FamilyProfile
import fr.cyclooo.opendeezer.engine.HomeData
import fr.cyclooo.opendeezer.engine.Playlist
import fr.cyclooo.opendeezer.engine.Track
import fr.cyclooo.opendeezer.player.PlayerController
import fr.cyclooo.opendeezer.ui.components.Artwork
import fr.cyclooo.opendeezer.ui.components.MediaCard
import fr.cyclooo.opendeezer.ui.components.SectionHeader
import fr.cyclooo.opendeezer.ui.theme.DeezerPurple
import java.util.Calendar

private data class QuickPick(val label: String, val icon: ImageVector, val route: String, val tint: Color)

@Composable
private fun QuickPickCard(pick: QuickPick, modifier: Modifier, onNavigate: (String) -> Unit) {
    Surface(
        onClick = { onNavigate(pick.route) },
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(pick.icon, contentDescription = null, tint = pick.tint, modifier = Modifier.size(27.dp))
            Text(
                pick.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopTrackCard(
    track: Track,
    player: PlayerController,
    modifier: Modifier = Modifier.width(290.dp).height(88.dp),
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Row(
            Modifier.padding(if (compact) 7.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Artwork(track.artworkUrl, Modifier.size(if (compact) 48.dp else 64.dp), corner = 10)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    track.name.ifBlank { stringResource(R.string.unknown_title) },
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artistLine.ifBlank { track.albumName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_play_next)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                        onClick = { menuOpen = false; player.playNext(track) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_to_queue)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = { menuOpen = false; player.addToQueue(track) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accountId: String,
    accountName: String,
    familyProfiles: List<FamilyProfile>,
    familyBusy: Boolean,
    familyError: Boolean,
    connected: Boolean,
    player: PlayerController,
    onNavigate: (String) -> Unit,
    onPlaylist: (Playlist) -> Unit,
    onCast: () -> Unit,
    onSettings: () -> Unit,
    onLoadFamilyProfiles: () -> Unit,
    onSelectFamilyProfile: (String) -> Unit,
) {
    var showProfiles by remember { mutableStateOf(false) }
    val compactHeader = LocalConfiguration.current.screenWidthDp < 700
    val portalDashboard = LocalConfiguration.current.screenWidthDp >= 1000
    LaunchedEffect(Unit) { onLoadFamilyProfiles() }
    LaunchedEffect(accountId) { showProfiles = false }
    val homeData by produceState<HomeData?>(initialValue = null, key1 = accountId) {
        value = null
        value = Engine.home()
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeGreeting = when {
        hour < 12 -> stringResource(R.string.greeting_morning)
        hour < 17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    val greeting = if (accountName.isNotBlank()) {
        stringResource(R.string.greeting_named, timeGreeting, accountName.trim().substringBefore(' '))
    } else {
        timeGreeting
    }

    val quickPicks = listOf(
        QuickPick("Flow", Icons.Filled.Stream, Routes.FLOW, Color(0xFFCBA6FF)),
        QuickPick(stringResource(R.string.home_liked), Icons.Filled.Favorite, Routes.LIKED, Color(0xFFFF8CB8)),
        QuickPick(stringResource(R.string.charts_title), Icons.Filled.BarChart, Routes.CHARTS, Color(0xFF8CCFFF)),
        QuickPick(stringResource(R.string.podcasts_title), Icons.Filled.Podcasts, Routes.PODCASTS, Color(0xFFFFD08C)),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (compactHeader) "OpenDeezer" else stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = DeezerPurple,
                    )
                },
                actions = {
                    if (familyProfiles.size > 1) {
                        Box {
                            Surface(
                                onClick = { showProfiles = true },
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            ) {
                                Row(
                                    Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = DeezerPurple)
                                    if (!compactHeader) {
                                        Text(
                                            accountName,
                                            modifier = Modifier.widthIn(max = 138.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(expanded = showProfiles, onDismissRequest = { showProfiles = false }) {
                                familyProfiles.forEach { member ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(member.name + if (member.userId == accountId) stringResource(R.string.family_current) else "")
                                        },
                                        onClick = { onSelectFamilyProfile(if (member.isAdmin) "" else member.userId) },
                                        enabled = !familyBusy && member.userId != accountId,
                                    )
                                }
                                if (familyBusy) CircularProgressIndicator(Modifier.size(24.dp))
                                if (familyError) {
                                    Text(stringResource(R.string.family_switch_error), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    IconButton(onClick = { onNavigate(Routes.HISTORY) }) {
                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history_title))
                    }
                    IconButton(onClick = onCast) {
                        Icon(
                            if (connected) Icons.Filled.CastConnected else Icons.Filled.Cast,
                            contentDescription = stringResource(R.string.cd_connect),
                            tint = if (connected) DeezerPurple else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onSettings, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        // Snapshot read in composable scope so LazyColumn recomposes on data arrival.
        val data = homeData

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            // Personal context comes first; keep the full profile name in the
            // picker while greeting the listener by their first name.
            item {
                Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 14.dp)) {
                    Text(greeting, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // The whole search surface is a generous touch target and opens
            // the dedicated search screen with the keyboard ready.
            item {
                Surface(
                    onClick = { onNavigate(Routes.SEARCH) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Four shortcuts, laid out as 4×1 on Portal and 2×2 on phones.
            item {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    if (maxWidth < 600.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            quickPicks.chunked(2).forEach { pair ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    pair.forEach { pick -> QuickPickCard(pick, Modifier.weight(1f), onNavigate) }
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            quickPicks.forEach { pick -> QuickPickCard(pick, Modifier.weight(1f), onNavigate) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            // 3 & 4. Dynamic sections — show spinner until data arrives
            if (data == null) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Portal Go: both sections remain discoverable without scrolling.
                // Keep the familiar stacked rails for portrait and narrow screens.
                if (data.playlists.isNotEmpty() && data.topTracks.isNotEmpty() && portalDashboard) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(0.61f)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.section_your_playlists),
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { onNavigate(Routes.PLAYLISTS) }) {
                                        Text(stringResource(R.string.home_see_all))
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(data.playlists, key = { "hp-${it.id}" }) { pl ->
                                        MediaCard(
                                            title = pl.name,
                                            subtitle = if (pl.trackCount > 0) {
                                                pluralStringResource(R.plurals.n_tracks, pl.trackCount, pl.trackCount)
                                            } else pl.owner,
                                            artworkUrl = pl.artworkUrl,
                                            onClick = { onPlaylist(pl) },
                                        )
                                    }
                                }
                            }
                            Column(
                                Modifier.weight(0.39f),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    stringResource(R.string.section_top_tracks),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                data.topTracks.take(4).forEachIndexed { index, track ->
                                    TopTrackCard(
                                        track = track,
                                        player = player,
                                        modifier = Modifier.fillMaxWidth().height(64.dp),
                                        compact = true,
                                    ) { player.playQueue(data.topTracks, index) }
                                }
                            }
                        }
                    }
                } else {
                // Lead with the listener's collection: artwork is the most
                // useful visual navigation landmark on a large touch screen.
                if (data.playlists.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.section_your_playlists),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onNavigate(Routes.PLAYLISTS) }) {
                                Text(stringResource(R.string.home_see_all))
                            }
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 19.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(data.playlists, key = { "hp-${it.id}" }) { pl ->
                                MediaCard(
                                    title = pl.name,
                                    subtitle = if (pl.trackCount > 0) {
                                        pluralStringResource(R.plurals.n_tracks, pl.trackCount, pl.trackCount)
                                    } else {
                                        pl.owner
                                    },
                                    artworkUrl = pl.artworkUrl,
                                    onClick = { onPlaylist(pl) },
                                )
                            }
                        }
                    }
                }

                if (data.topTracks.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.section_top_tracks)) }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                data.topTracks,
                                key = { i, t -> "ht-$i-${t.id}" },
                            ) { index, track ->
                                TopTrackCard(track, player) { player.playQueue(data.topTracks, index) }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
