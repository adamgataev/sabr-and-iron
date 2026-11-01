package com.example.sabr_and_iron.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun ReorderableCollectionItemScope.ReorderableExerciseRow(
    name: String,
    isDragging: Boolean,
    dimmed: Boolean,
    onRemove: (() -> Unit)?,
    removeEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        label = "reorderElevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "reorderScale",
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            isDragging -> 1f
            dimmed -> 0.45f
            else -> 1f
        },
        label = "reorderAlpha",
    )

    AppCard(
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
            }
            .alpha(alpha),
        selected = isDragging,
        contentPadding = AppCardCompactContentPadding,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(40.dp)
                    .draggableHandle(),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    enabled = removeEnabled,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = if (removeEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                    )
                }
            }
        }
    }
}
