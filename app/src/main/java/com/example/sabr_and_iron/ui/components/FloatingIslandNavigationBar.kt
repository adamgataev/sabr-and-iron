package com.example.sabr_and_iron.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.sabr_and_iron.ui.navigation.AppDestination

private val IslandShape = RoundedCornerShape(32.dp)

/** Matches standard list horizontal inset (16.dp). */
private val IslandHorizontalInset = 16.dp

private val IslandBottomInset = 24.dp

/** Taller than default NavigationBar (80.dp). */
private val IslandBarHeight = 92.dp

/** Height of the action row (search / FAB) above the island. */
val TabChromeActionRowHeight: Dp = 64.dp

/** Gap between the action row (search/FAB) and the floating island. */
private val ActionRowIslandGap: Dp = 12.dp

/** Space from screen bottom to the top of the floating island. */
val FloatingIslandTopClearance: Dp = IslandBottomInset + IslandBarHeight + 8.dp

/**
 * Space from screen bottom to the top of the action row (search/FAB).
 * Lists should pad at least this much so content clears the chrome.
 */
val FloatingIslandListBottomPadding: Dp =
    FloatingIslandTopClearance + TabChromeActionRowHeight + ActionRowIslandGap + 12.dp

/** List bottom pad when only the island is shown (no search/FAB action row). */
val FloatingIslandOnlyListBottomPadding: Dp = FloatingIslandTopClearance + 12.dp

/**
 * Single bottom overlay: frost + optional action row (search/FAB) + opaque island.
 * Keeps FAB/search on the same layer as the nav island.
 */
@Composable
fun TabBottomChrome(
    navController: NavHostController,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
    actionRow: (@Composable RowScope.() -> Unit)? = null,
) {
    val frost = MaterialTheme.colorScheme.background.copy(alpha = 0.82f)
    val frostEdge = MaterialTheme.colorScheme.background.copy(alpha = 0f)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(frostEdge, frost),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(frost)
                    .padding(bottom = IslandBottomInset),
            ) {
                Column {
                    if (actionRow != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TabChromeActionRowHeight)
                                .padding(horizontal = IslandHorizontalInset),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            content = actionRow,
                        )
                        Spacer(Modifier.height(ActionRowIslandGap))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                    FloatingIslandBar(
                        navController = navController,
                        currentDestination = currentDestination,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingIslandBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = IslandHorizontalInset)
            .clip(IslandShape)
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = 0.55f),
                shape = IslandShape,
            ),
        shape = IslandShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp,
    ) {
        NavigationBar(
            modifier = Modifier.height(IslandBarHeight),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            AppDestination.entries.forEach { destination ->
                val selected = currentDestination
                    ?.hierarchy
                    ?.any { it.route == destination.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

/** @deprecated Prefer [TabBottomChrome]. Kept name for any residual refs. */
@Composable
fun FloatingIslandNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    modifier: Modifier = Modifier,
) {
    TabBottomChrome(
        navController = navController,
        currentDestination = currentDestination,
        modifier = modifier,
        actionRow = null,
    )
}
