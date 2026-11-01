package com.example.sabr_and_iron.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressMetric
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.EmptyState
import com.example.sabr_and_iron.ui.components.FloatingIslandOnlyListBottomPadding
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.theme.AccentBlue
import com.example.sabr_and_iron.ui.theme.ChartSecondary
import com.example.sabr_and_iron.ui.util.formatChartAxisDate
import com.example.sabr_and_iron.ui.util.formatDate
import com.example.sabr_and_iron.ui.util.formatDistanceKm
import com.example.sabr_and_iron.ui.util.formatDuration
import com.example.sabr_and_iron.ui.util.formatKg
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.roundToInt

@Composable
fun RecordsSortMenu(
    viewModel: RecordsViewModel = viewModel(),
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sort by viewModel.sort.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort records",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(12.dp),
        ) {
            DropdownMenuItem(
                text = { Text("Sort A–Z") },
                onClick = {
                    viewModel.setSort(RecordsSort.Alphabetical)
                    menuExpanded = false
                },
                trailingIcon = {
                    if (sort == RecordsSort.Alphabetical) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Sort by last updated") },
                onClick = {
                    viewModel.setSort(RecordsSort.LastUpdated)
                    menuExpanded = false
                },
                trailingIcon = {
                    if (sort == RecordsSort.LastUpdated) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
            )
        }
    }
}

@Composable
fun RecordsScreen(
    onOpenRecord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    val density = LocalDensity.current
    var maxTileHeightPx by remember { mutableIntStateOf(0) }
    val rows = remember(records) { records.chunked(2) }

    LaunchedEffect(records) {
        maxTileHeightPx = 0
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (records.isEmpty()) {
            EmptyState(
                title = "No personal records yet.",
                subtitle = "PRs and progress charts will appear here.",
            )
        } else {
            val tileHeightModifier = if (maxTileHeightPx > 0) {
                Modifier.height(with(density) { maxTileHeightPx.toDp() })
            } else {
                Modifier.fillMaxHeight()
            }
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = FloatingIslandOnlyListBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = rows,
                    key = { row -> row.joinToString("-") { it.exerciseId.toString() } },
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { record ->
                            RecordTile(
                                record = record,
                                onClick = { onOpenRecord(record.exerciseId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .then(tileHeightModifier)
                                    .onSizeChanged { size ->
                                        if (size.height > maxTileHeightPx) {
                                            maxTileHeightPx = size.height
                                        }
                                    },
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordTile(
    record: PersonalRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Text(
            text = record.exerciseName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = record.exerciseType.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = formatRecordSummary(record),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDate(record.performedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun RecordDetailScreen(
    exerciseId: Long,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(),
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val record = records.find { it.exerciseId == exerciseId }
    val progress by viewModel.observeProgress(exerciseId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val dualProgress = remember(progress) {
        progress.filter { it.secondaryMetricValue != null }
    }
    val showDualDistanceTime = dualProgress.isNotEmpty() &&
        dualProgress.first().metric == ProgressMetric.DistanceMeters

    val sharedScrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = Scroll.Absolute.End,
    )
    val visibleXCount = remember(dualProgress.size) {
        dualProgress.size.coerceIn(1, 6).toDouble()
    }
    val sharedZoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = remember(visibleXCount) {
            Zoom.max(Zoom.fixed(), Zoom.x(visibleXCount))
        },
        minZoom = Zoom.Content,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (record != null) {
            Text(
                text = record.exerciseName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = record.exerciseType.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatRecordSummary(record),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatDate(record.performedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (progress.isEmpty()) {
            Text(
                "Not enough history to chart yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (showDualDistanceTime) {
            ProgressChart(
                progress = dualProgress,
                yValues = dualProgress.map { it.metricValue },
                metric = ProgressMetric.DistanceMeters,
                title = "Distance",
                subtitle = "Distance over time",
                primaryLabel = "Distance",
                showBodyWeight = false,
                scrollState = sharedScrollState,
                zoomState = sharedZoomState,
            )
            ProgressChart(
                progress = dualProgress,
                yValues = dualProgress.mapNotNull { it.secondaryMetricValue },
                metric = ProgressMetric.DurationSeconds,
                title = "Duration",
                subtitle = "Duration over time",
                primaryLabel = "Duration",
                showBodyWeight = false,
                scrollState = sharedScrollState,
                zoomState = sharedZoomState,
            )
        } else {
            ProgressChart(progress = progress)
        }
    }
}

fun formatRecordSummary(record: PersonalRecord): String =
    when {
        record.effectiveWeightKg != null && record.reps != null ->
            "${formatKg(record.effectiveWeightKg)} kg × ${record.reps}"
        record.distanceMeters != null -> {
            val distance = formatDistanceKm(record.distanceMeters)
            val duration = record.durationSeconds?.let { " · ${formatDuration(it)}" }.orEmpty()
            "$distance$duration"
        }
        record.durationSeconds != null -> formatDuration(record.durationSeconds)
        else -> "—"
    }

@Composable
fun ProgressChart(
    progress: List<ProgressPoint>,
    modifier: Modifier = Modifier,
    yValues: List<Double> = progress.map { it.metricValue },
    metric: ProgressMetric = progress.firstOrNull()?.metric ?: ProgressMetric.LoadKg,
    title: String = "Progress",
    subtitle: String? = null,
    primaryLabel: String? = null,
    showBodyWeight: Boolean =
        metric == ProgressMetric.LoadKg && progress.any { it.bodyWeightKg != null },
    scrollState: VicoScrollState? = null,
    zoomState: VicoZoomState? = null,
) {
    if (progress.isEmpty() || yValues.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    var chartReady by remember { mutableStateOf(false) }
    val dates = remember(progress) { progress.map { it.performedAt } }
    val resolvedSubtitle = subtitle ?: when (metric) {
        ProgressMetric.LoadKg ->
            "Effective load over time" + if (showBodyWeight) " vs body weight" else ""
        ProgressMetric.DurationSeconds -> "Duration over time"
        ProgressMetric.DistanceMeters -> "Distance over time"
    }
    val resolvedPrimaryLabel = primaryLabel ?: when (metric) {
        ProgressMetric.LoadKg -> "Load"
        ProgressMetric.DurationSeconds -> "Duration"
        ProgressMetric.DistanceMeters -> "Distance"
    }

    val bottomAxisFormatter = remember(dates) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.roundToInt().coerceIn(dates.indices)
            formatChartAxisDate(dates[index])
        }
    }
    val startAxisFormatter = remember(metric) {
        CartesianValueFormatter { _, value, _ ->
            when (metric) {
                ProgressMetric.LoadKg -> "${formatKg(value)} kg"
                ProgressMetric.DurationSeconds -> formatDuration(value.roundToInt())
                ProgressMetric.DistanceMeters -> formatDistanceKm(value)
            }
        }
    }

    val ownedScrollState = rememberVicoScrollState(
        scrollEnabled = true,
        initialScroll = Scroll.Absolute.End,
    )
    val visibleXCount = remember(progress.size) {
        progress.size.coerceIn(1, 6).toDouble()
    }
    val ownedZoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = remember(visibleXCount) {
            Zoom.max(Zoom.fixed(), Zoom.x(visibleXCount))
        },
        minZoom = Zoom.Content,
    )
    val effectiveScroll = scrollState ?: ownedScrollState
    val effectiveZoom = zoomState ?: ownedZoomState

    LaunchedEffect(progress, yValues, showBodyWeight) {
        chartReady = false
        val bodyXs = mutableListOf<Double>()
        val bodyYs = mutableListOf<Double>()
        if (showBodyWeight) {
            progress.forEachIndexed { index, point ->
                val body = point.bodyWeightKg ?: return@forEachIndexed
                bodyXs += index.toDouble()
                bodyYs += body
            }
        }
        modelProducer.runTransaction {
            lineSeries {
                series(y = yValues)
                if (bodyXs.isNotEmpty()) {
                    series(x = bodyXs, y = bodyYs)
                }
            }
        }
        chartReady = true
    }

    ProvideVicoTheme(
        theme = rememberM3VicoTheme(
            lineCartesianLayerColors = listOf(AccentBlue, ChartSecondary),
            lineColor = MaterialTheme.colorScheme.outline,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        AppCard(modifier = modifier) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = resolvedSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            ) {
                Text(
                    resolvedPrimaryLabel,
                    color = AccentBlue,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (showBodyWeight) {
                    Text(
                        "Body weight",
                        color = ChartSecondary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (chartReady) {
                val lineColors = listOf(AccentBlue, ChartSecondary)
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                lineColors.map { color ->
                                    LineCartesianLayer.rememberLine(
                                        fill = LineCartesianLayer.LineFill.single(fill(color)),
                                        pointProvider = LineCartesianLayer.PointProvider.single(
                                            LineCartesianLayer.point(
                                                rememberShapeComponent(
                                                    fill = fill(color),
                                                    shape = CorneredShape.Pill,
                                                ),
                                            ),
                                        ),
                                    )
                                },
                            ),
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = startAxisFormatter,
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomAxisFormatter,
                            labelRotationDegrees = -35f,
                        ),
                    ),
                    modelProducer = modelProducer,
                    scrollState = effectiveScroll,
                    zoomState = effectiveZoom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
            } else {
                Text(
                    text = "Loading chart…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }
    }
}
