package com.example.sabr_and_iron.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val AppCardShape = RoundedCornerShape(16.dp)

val AppCardContentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

/** Denser padding for compact list rows (e.g. reorderable exercises). */
val AppCardCompactContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = AppCardContentPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(
        width = 1.dp,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline
        },
    )
    val colors = CardDefaults.cardColors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    )
    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = AppCardShape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(contentPadding),
                content = content,
            )
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = AppCardShape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}
