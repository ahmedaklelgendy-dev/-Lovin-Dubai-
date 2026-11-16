package com.augustus.lovindubai.presentation.channel

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    viewModel: ChannelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.videos.collectAsLazyPagingItems()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val loadedVideos = pagingItems.itemSnapshotList.items

    fun matchesSelectedTab(video: Video): Boolean {
        return when (selectedTab) {
            0 -> video.type == VideoType.SHORT
            1 -> video.type == VideoType.REGULAR
            else -> video.type in listOf(
                VideoType.LIVE_ACTIVE,
                VideoType.LIVE_UPCOMING,
                VideoType.LIVE_RECORDED
            )
        }
    }

    // تصفية الفيديوهات مسبقاً لمنع ترك مساحات فارغة في القائمة
    val filteredVideos = loadedVideos.filter { matchesSelectedTab(it) }
    val filteredCount = filteredVideos.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LOVIN DUBAI",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Text(
                        text = "YOUTUBE",
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/@LovinDubai")
                                )
                                context.startActivity(intent)
                            },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->

        when {
            state.isLoading && state.channel == null -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.error != null && state.channel == null -> {
                ErrorContent(
                    message = state.error ?: "Something went wrong",
                    onRetry = { viewModel.loadChannel() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            state.channel != null -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing ||
                            pagingItems.loadState.refresh is LoadState.Loading,
                    onRefresh = {
                        pagingItems.refresh()
                        viewModel.refresh()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        item {
                            ChannelHeader(
                                title = state.channel!!.title,
                                description = state.channel!!.description,
                                thumbnailUrl = state.channel!!.thumbnailUrl
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(
                                    title = "Subscribers",
                                    value = formatCount(
                                        state.channel!!.subscriberCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                StatCard(
                                    title = "Total Views",
                                    value = formatCount(
                                        state.channel!!.viewCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                StatCard(
                                    title = "Videos",
                                    value = formatCount(
                                        state.channel!!.videoCount
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Explore Content",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                            )
                        }

                        item {
                            TabRow(selectedTabIndex = selectedTab) {
                                listOf("Shorts", "Videos", "Live").forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(title) }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "$filteredCount loaded videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp
                                )
                            )
                        }

                        // Initial loading
                        if (
                            pagingItems.loadState.refresh is LoadState.Loading &&
                            loadedVideos.isEmpty()
                        ) {
                            item {
                                LoadingContent()
                            }
                        }

                        // Initial loading error
                        if (
                            pagingItems.loadState.refresh is LoadState.Error &&
                            loadedVideos.isEmpty()
                        ) {
                            item {
                                ErrorContent(
                                    message = (
                                            pagingItems.loadState.refresh as LoadState.Error
                                            ).error.localizedMessage
                                        ?: "Failed to load videos",
                                    onRetry = { pagingItems.retry() }
                                )
                            }
                        }

                        // Paging videos (عرض العناصر المفلترة مباشرة دون مسافات فارغة)
                        if (filteredVideos.isNotEmpty()) {
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

                        // Empty state only after all pages are loaded
                        if (
                            pagingItems.loadState.refresh is LoadState.NotLoading &&
                            pagingItems.loadState.append is LoadState.NotLoading &&
                            pagingItems.loadState.append.endOfPaginationReached &&
                            filteredCount == 0
                        ) {
                            item {
                                EmptyContent(selectedTab)
                            }
                        }

                        // Loading next page
                        if (pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        // Next page error
                        if (pagingItems.loadState.append is LoadState.Error) {
                            item {
                                ErrorContent(
                                    message = (
                                            pagingItems.loadState.append as LoadState.Error
                                            ).error.localizedMessage
                                        ?: "Failed to load more videos",
                                    onRetry = { pagingItems.retry() }
                                )
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=${video.id}")
                )
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {

            Box {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(52.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatCount(video.viewCount) + " views",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = video.publishedAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    selectedTab: Int
) {
    val message = when (selectedTab) {
        0 -> "No Shorts available"
        1 -> "No videos available"
        else -> "No live videos available"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.material3.Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000_000 ->
            "%.1fB".format(count / 1_000_000_000.0)

        count >= 1_000_000 ->
            "%.1fM".format(count / 1_000_000.0)

        count >= 1_000 ->
            "%.1fK".format(count / 1_000.0)

        else -> count.toString()
    }
}