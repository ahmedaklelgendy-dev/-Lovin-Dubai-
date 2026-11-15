package com.augustus.lovindubai.presentation.channel

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow

private val LovinOrange = Color(0xFFFF604A)
private val LovinDark = Color(0xFF151515)
private val LovinGray = Color(0xFF777777)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    // Filter videos according to the selected tab.
    val filteredVideos = remember(state.videos, selectedTab) {
        when (selectedTab) {
            0 -> state.videos.filter {
                it.type == VideoType.SHORT
            }

            1 -> state.videos.filter {
                it.type == VideoType.REGULAR
            }

            else -> state.videos.filter {
                it.type == VideoType.LIVE_ACTIVE ||
                        it.type == VideoType.LIVE_UPCOMING ||
                        it.type == VideoType.LIVE_RECORDED
            }
        }
    }

    // Return to the top when changing tabs.
    LaunchedEffect(selectedTab) {
        listState.scrollToItem(0)
    }

    // Infinite scrolling.
    LaunchedEffect(listState, selectedTab) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo

            val lastVisibleItem = layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1

            lastVisibleItem to layoutInfo.totalItemsCount
        }
            .distinctUntilChanged()
            .collect { (lastVisibleItem, totalItems) ->

                if (
                    totalItems > 0 &&
                    lastVisibleItem >= totalItems - 3
                ) {
                    viewModel.loadNextPage()
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOVIN",
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = " DUBAI",
                            color = LovinOrange,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                actions = {
                    Text(
                        text = "YOUTUBE",
                        color = LovinOrange,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = LovinOrange
                    )
                }
            }

            state.error != null -> {
                ErrorContent(
                    error = state.error ?: "Unknown error",
                    onRetry = {
                        viewModel.loadChannel()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }

            state.channel != null -> {

                val channel = state.channel!!

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {

                    // Channel Header
                    item {
                        ChannelHeader(
                            title = channel.title,
                            description = channel.description,
                            thumbnailUrl = channel.thumbnailUrl
                        )
                    }

                    // Channel Statistics
                    item {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        ) {
                            Text(
                                text = "CHANNEL OVERVIEW",
                                style = MaterialTheme.typography.labelLarge,
                                color = LovinGray,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard(
                                    title = "Subscribers",
                                    value = formatCount(
                                        channel.subscriberCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                StatCard(
                                    title = "Total Views",
                                    value = formatCount(
                                        channel.viewCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                StatCard(
                                    title = "Videos",
                                    value = formatCount(
                                        channel.videoCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Explore Content
                    item {
                        Column(
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 24.dp,
                                bottom = 12.dp
                            )
                        ) {
                            Text(
                                text = "Explore Content",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "Discover the latest from Lovin Dubai",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LovinGray
                            )
                        }
                    }

                    // Tabs
                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = LovinOrange,
                            divider = {}
                        ) {
                            listOf(
                                "Shorts",
                                "Videos",
                                "Live"
                            ).forEachIndexed { index, title ->

                                Tab(
                                    selected = selectedTab == index,
                                    onClick = {
                                        selectedTab = index
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (
                                                selectedTab == index
                                            ) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Content count
                    item {
                        Text(
                            text = "${filteredVideos.size} videos",
                            style = MaterialTheme.typography.labelMedium,
                            color = LovinGray,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        )
                    }

                    // Initial loading
                    if (
                        state.isVideosLoading &&
                        state.videos.isEmpty()
                    ) {
                        item {
                            LoadingContent()
                        }
                    }

                    // Initial error
                    else if (
                        state.videosError != null &&
                        state.videos.isEmpty()
                    ) {
                        item {
                            Text(
                                text = state.videosError
                                    ?: "Failed to load videos",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Empty content
                    else if (filteredVideos.isEmpty()) {
                        item {
                            EmptyContent(
                                selectedTab = selectedTab
                            )
                        }
                    }

                    // Every video is now a separate LazyColumn item.
                    else {
                        items(
                            items = filteredVideos,
                            key = { video -> video.id }
                        ) { video ->

                            VideoCard(
                                video = video,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp
                                )
                            )
                        }
                    }

                    // Loading more indicator
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = LovinOrange
                                )
                            }
                        }
                    }

                    // Load more error and retry
                    if (state.loadMoreError != null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.loadMoreError
                                        ?: "Couldn't load more videos",
                                    color = MaterialTheme.colorScheme.error
                                )

                                TextButton(
                                    onClick = {
                                        viewModel.loadNextPage()
                                    }
                                ) {
                                    Text("Tap to retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    title: String,
    description: String,
    thumbnailUrl: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 20.dp
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Channel profile image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LovinOrange)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text(
                        text = "Official YouTube Channel",
                        style = MaterialTheme.typography.bodySmall,
                        color = LovinGray
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LovinOrange,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = LovinGray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VideoCard(
    video: Video,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.youtube.com/watch?v=${video.id}"
                    )
                )

                context.startActivity(intent)
            },
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                if (
                    video.type == VideoType.LIVE_ACTIVE ||
                    video.type == VideoType.LIVE_UPCOMING ||
                    video.type == VideoType.LIVE_RECORDED
                ) {
                    Text(
                        text = when (video.type) {
                            VideoType.LIVE_ACTIVE -> "LIVE"
                            VideoType.LIVE_UPCOMING -> "UPCOMING"
                            else -> "LIVE STREAM"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(
                                LovinOrange,
                                RoundedCornerShape(5.dp)
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            )
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "${formatCount(video.viewCount)} views",
                    style = MaterialTheme.typography.bodySmall,
                    color = LovinGray
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = LovinOrange
        )
    }
}

@Composable
private fun EmptyContent(
    selectedTab: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (selectedTab) {
                0 -> "No Shorts available"
                1 -> "No videos available"
                else -> "No live streams available"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Content will appear here when available.",
            color = LovinGray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = error,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = LovinOrange
            )
        ) {
            Text("Try Again")
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000_000 ->
            String.format(
                "%.1fB",
                count / 1_000_000_000.0
            )

        count >= 1_000_000 ->
            String.format(
                "%.1fM",
                count / 1_000_000.0
            )

        count >= 1_000 ->
            String.format(
                "%.1fK",
                count / 1_000.0
            )

        else -> count.toString()
    }
}