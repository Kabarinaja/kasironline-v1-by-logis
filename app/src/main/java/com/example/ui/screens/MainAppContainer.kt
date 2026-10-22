package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.TransactionDetail
import com.example.localization.LocalizedStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

enum class MainDestination(val id: String, val icon: ImageVector, val labelKey: String) {
    DASHBOARD("dashboard", Icons.Default.Dashboard, "dashboard"),
    POS("pos", Icons.Default.PointOfSale, "pos"),
    PRODUCTS("products", Icons.Default.Inventory, "products"),
    HISTORY("history", Icons.Default.History, "history"),
    SETTINGS("settings", Icons.Default.Settings, "settings")
}

@Composable
fun MainAppContainer(
    viewModel: PosViewModel,
    uiState: PosUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    var currentScreen by remember { mutableStateOf("dashboard") }
    val screenStack = remember { mutableStateListOf("dashboard") }

    var receiptDetailToShow by remember { mutableStateOf<TransactionDetail?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe last completed transaction
    LaunchedEffect(uiState.lastCompletedTransaction) {
        if (uiState.lastCompletedTransaction != null) {
            receiptDetailToShow = uiState.lastCompletedTransaction
        }
    }

    // Observe toast notifications
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    fun navigateTo(dest: String) {
        if (currentScreen != dest) {
            screenStack.add(dest)
            currentScreen = dest
        }
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            currentScreen = screenStack.last()
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        navigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isTablet && currentScreen in MainDestination.entries.map { it.id }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    MainDestination.entries.forEach { dest ->
                        val selected = currentScreen == dest.id
                        val label = when (dest) {
                            MainDestination.DASHBOARD -> strings.dashboard
                            MainDestination.POS -> strings.pos
                            MainDestination.PRODUCTS -> strings.products
                            MainDestination.HISTORY -> strings.history
                            MainDestination.SETTINGS -> strings.settings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTo(dest.id) },
                            icon = { Icon(dest.icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandToska,
                                selectedTextColor = BrandToska,
                                indicatorColor = BrandToskaLight
                            ),
                            modifier = Modifier.testTag("nav_item_${dest.id}")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tablet Navigation Rail
            if (isTablet) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Image(
                            painter = painterResource(id = R.drawable.logo_kasirdunia),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .height(40.dp)
                                .padding(vertical = 4.dp)
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MainDestination.entries.forEach { dest ->
                        val selected = currentScreen == dest.id
                        val label = when (dest) {
                            MainDestination.DASHBOARD -> strings.dashboard
                            MainDestination.POS -> strings.pos
                            MainDestination.PRODUCTS -> strings.products
                            MainDestination.HISTORY -> strings.history
                            MainDestination.SETTINGS -> strings.settings
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(dest.id) },
                            icon = { Icon(dest.icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BrandToska,
                                selectedTextColor = BrandToska,
                                indicatorColor = BrandToskaLight
                            ),
                            modifier = Modifier.testTag("rail_item_${dest.id}")
                        )
                    }
                }
                VerticalDivider(color = BrandGrayBorder)
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToPos = { navigateTo("pos") },
                        onNavigateToSession = { navigateTo("session") },
                        onNavigateToProducts = { navigateTo("products") },
                        onNavigateToHistory = { navigateTo("history") }
                    )
                    "pos" -> PosScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onShowReceipt = {
                            receiptDetailToShow = uiState.lastCompletedTransaction
                        }
                    )
                    "products" -> ProductManagementScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    "history" -> HistoryScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onReprintReceipt = { detail ->
                            receiptDetailToShow = detail
                        }
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onNavigateToAbout = { navigateTo("about") },
                        onNavigateToCrew = { navigateTo("crew") }
                    )
                    "session" -> CashierSessionScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    "crew" -> CrewScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    "about" -> AboutScreen(
                        onBack = { navigateBack() }
                    )
                }
            }
        }
    }

    // Receipt Modal Dialog
    if (receiptDetailToShow != null) {
        ReceiptDialog(
            detail = receiptDetailToShow!!,
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = { receiptDetailToShow = null }
        )
    }
}
