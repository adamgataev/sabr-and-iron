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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.sabr_and_iron.ui.navigation.AppDestination

private val IslandShape = RoundedCornerShape(32.dp)

/** Matches standard list horizontal inset (16.dp). Also used for island-width dialogs. */
val TabChromeHorizontalInset: Dp = 16.dp

private val IslandHorizontalInset = TabChromeHorizontalInset

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
 * When the IME is open the island collapses so search/FAB sit flush above the keyboard.
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
    // enableEdgeToEdge() prevents classic adjustResize from lifting the window;
    // use imePadding() below for layout. ViewCompat still drives island collapse
    // (Compose IME bottom can be 0 even when the keyboard is visible).
    val view = LocalView.current
    var imeVisible by remember { mutableStateOf(false) }
    DisposableEffect(view) {
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            imeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        listener.onGlobalLayout()
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            if (actionRow != null) {
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    color = frost,
                    shadowElevation = 6.dp,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TabChromeActionRowHeight)
                            .padding(horizontal = IslandHorizontalInset),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        content = actionRow,
                    )
                }
                if (!imeVisible) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ActionRowIslandGap)
                            .background(frost),
                    )
                }
            } else if (!imeVisible) {
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
            }
            if (!imeVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(frost)
                        .padding(
                            top = if (actionRow != null) 0.dp else 8.dp,
                            bottom = IslandBottomInset,
                        ),
                ) {
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
