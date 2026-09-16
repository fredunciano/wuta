package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BeverageType
import com.example.data.model.HydrationSummary
import com.example.data.model.WaterLog
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AddCustomLogDialog
import com.example.ui.components.BeverageBreakdownSection
import com.example.ui.components.ConfettiCelebration
import com.example.ui.components.GeminiAssistantBottomSheet
import com.example.ui.components.GeminiVoiceAssistantBar
import com.example.ui.components.HourlyIntakeChart
import com.example.ui.components.HydrationStatsOverview
import com.example.ui.components.InteractiveWaterWave
import com.example.ui.components.QuickLogSection
import com.example.ui.components.ReminderSettingsSheet
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.WeeklyTrendChart
import com.example.ui.theme.WaterTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleVoiceIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

            WaterTrackerTheme(darkModeSetting = userSettings.darkModeSetting) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceIntent(intent)
    }

    private fun handleVoiceIntent(intent: Intent?) {
        if (intent == null) return

        val data = intent.data
        val query = data?.getQueryParameter("query")
            ?: intent.getStringExtra("query")
            ?: intent.getStringExtra("foodOrWater.name")
            ?: intent.getStringExtra("healthMetric.name")

        if (!query.isNullOrBlank()) {
            viewModel.processAssistantSpeechOrText(query)
        } else if (data?.host == "assistant" || intent.getBooleanExtra("open_assistant", false)) {
            // Can trigger assistant view
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedBeverage by viewModel.selectedBeverage.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val showCelebration by viewModel.showGoalCelebration.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) } // 0: Hydrate, 1: Analytics, 2: History
    var showCustomLogDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Hydration reminders enabled! 💧", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle Toast, Goal, and Gemini Assistant auto-log events
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.TriggerGoalCelebration -> {
                    // Celebration is managed by state
                }
                is UiEvent.AssistantLoggedWater -> {
                    Toast.makeText(
                        context,
                        "🤖 Gemini: Logged +${event.amountMl}ml ${event.beverage.displayName}! 💧",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "WATER TRACKER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Daily Hydration",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    actions = {
                        // Gemini AI Assistant Quick Action
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF004977),
                            border = BorderStroke(1.dp, Color(0xFF75D1FF).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { showAssistantSheet = true }
                                .testTag("top_bar_gemini_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI Voice Assistant",
                                    tint = Color(0xFF75D1FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Live Offline-First Cloud Sync Status
                        SyncStatusBadge(
                            syncState = syncState,
                            isOnline = isOnline,
                            onManualSync = { viewModel.triggerManualSync() }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Social Share Action
                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.testTag("top_bar_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share hydration progress with friends",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Settings / Reminders Button
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.testTag("top_bar_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open reminder and hydration settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Opacity,
                                contentDescription = "Hydrate Dashboard"
                            )
                        },
                        label = { Text("Hydrate") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_hydrate")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Hydration Analytics and Trends"
                            )
                        },
                        label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_analytics")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Hydration Log History"
                            )
                        },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                }
            },
            floatingActionButton = {
                if (currentTab == 0 || currentTab == 2) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Floating Gemini Voice Trigger
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF004977),
                            border = BorderStroke(1.5.dp, Color(0xFF75D1FF)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { showAssistantSheet = true }
                                .testTag("fab_gemini_assistant")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini Assistant",
                                    tint = Color(0xFF75D1FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = { showCustomLogDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .testTag("fab_add_custom_water")
                                .semantics { contentDescription = "Log custom amount of water" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    0 -> HydrateDashboardScreen(
                        summary = summary,
                        todayLogs = todayLogs,
                        selectedBeverage = selectedBeverage,
                        assistantState = assistantState,
                        onOpenAssistantSheet = { showAssistantSheet = true },
                        onDirectVoiceSpoken = { spokenText ->
                            viewModel.processAssistantSpeechOrText(spokenText)
                        },
                        onBeverageSelected = { viewModel.selectBeverage(it) },
                        onQuickLog = { amount -> viewModel.logWater(amount, selectedBeverage) },
                        onCustomLogClick = { showCustomLogDialog = true },
                        onDeleteLog = { viewModel.deleteLog(it) }
                    )

                    1 -> AnalyticsScreen(
                        summary = summary
                    )

                    2 -> HistoryLogsScreen(
                        allLogs = allLogs,
                        onDeleteLog = { viewModel.deleteLog(it) }
                    )
                }
            }
        }

        // Confetti celebration overlay
        ConfettiCelebration(
            trigger = showCelebration,
            onAnimationEnd = { viewModel.dismissCelebration() }
        )

        // Gemini AI Assistant Bottom Sheet
        if (showAssistantSheet) {
            GeminiAssistantBottomSheet(
                assistantState = assistantState,
                onDismiss = { showAssistantSheet = false },
                onSendMessage = { message ->
                    viewModel.processAssistantSpeechOrText(message)
                }
            )
        }

        // Custom Log Dialog
        if (showCustomLogDialog) {
            AddCustomLogDialog(
                initialBeverage = selectedBeverage,
                onDismiss = { showCustomLogDialog = false },
                onConfirm = { amount, beverage, note ->
                    viewModel.logWater(amount, beverage, note)
                    showCustomLogDialog = false
                }
            )
        }

        // Social Share Modal Dialog
        if (showShareDialog) {
            SocialShareDialog(
                summary = summary,
                onDismiss = { showShareDialog = false }
            )
        }

        // Settings and Reminder Modal Sheet
        if (showSettingsSheet) {
            ReminderSettingsSheet(
                settings = userSettings,
                onDismiss = { showSettingsSheet = false },
                onUpdateSettings = { viewModel.updateSettings(it) },
                onSendTestNotification = { viewModel.sendTestReminder() }
            )
        }
    }
}

@Composable
fun HydrateDashboardScreen(
    summary: HydrationSummary,
    todayLogs: List<WaterLog>,
    selectedBeverage: BeverageType,
    assistantState: com.example.ui.AssistantUiState,
    onOpenAssistantSheet: () -> Unit,
    onDirectVoiceSpoken: (String) -> Unit,
    onBeverageSelected: (BeverageType) -> Unit,
    onQuickLog: (Int) -> Unit,
    onCustomLogClick: () -> Unit,
    onDeleteLog: (Long) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Gemini Voice & AI Assistant Bar
        item {
            GeminiVoiceAssistantBar(
                assistantState = assistantState,
                onOpenAssistantSheet = onOpenAssistantSheet,
                onDirectVoiceSpoken = onDirectVoiceSpoken
            )
        }

        // 2. Interactive Animated Water Flask Container
        item {
            InteractiveWaterWave(
                currentMl = summary.todayTotalMl,
                targetMl = summary.dailyTargetMl,
                beverageType = selectedBeverage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
        }

        // 3. Quick Log & Beverage selection controls
        item {
            QuickLogSection(
                selectedBeverage = selectedBeverage,
                onBeverageSelected = onBeverageSelected,
                onQuickLog = onQuickLog,
                onCustomLogClick = onCustomLogClick
            )
        }

        // 4. Streak & 7-Day Stats Overview
        item {
            HydrationStatsOverview(summary = summary)
        }

        // 5. Today's Recent Logs List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Activity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${todayLogs.size} logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (todayLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No water logged yet today",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Say \"I drank a glass of water\" or tap quick log above!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(todayLogs, key = { it.id }) { log ->
                WaterLogItemCard(
                    log = log,
                    timeFormatted = timeFormat.format(Date(log.timestamp)),
                    onDelete = { onDeleteLog(log.id) }
                )
            }
        }
    }
}

@Composable
fun AnalyticsScreen(summary: HydrationSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HydrationStatsOverview(summary = summary)
        }

        item {
            WeeklyTrendChart(
                weeklyHistory = summary.weeklyHistory,
                targetMl = summary.dailyTargetMl
            )
        }

        item {
            HourlyIntakeChart(
                hourlyIntakes = summary.hourlyIntakes
            )
        }

        item {
            BeverageBreakdownSection(summary = summary)
        }
    }
}

@Composable
fun HistoryLogsScreen(
    allLogs: List<WaterLog>,
    onDeleteLog: (Long) -> Unit
) {
    val fullDateFormat = remember { SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Complete Hydration History",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Locally encrypted and offline accessible logs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (allLogs.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recorded history yet",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            items(allLogs, key = { it.id }) { log ->
                WaterLogItemCard(
                    log = log,
                    timeFormatted = fullDateFormat.format(Date(log.timestamp)),
                    onDelete = { onDeleteLog(log.id) }
                )
            }
        }
    }
}

@Composable
fun WaterLogItemCard(
    log: WaterLog,
    timeFormatted: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_log_card_${log.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Beverage Icon with circular color badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(log.beverageType.defaultColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = log.beverageType.icon,
                        contentDescription = log.beverageType.displayName,
                        tint = log.beverageType.defaultColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${log.amountMl} ml",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (log.beverageType != BeverageType.WATER) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${log.effectiveHydrationMl}ml eff.)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${log.beverageType.displayName} · $timeFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (log.note.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted note",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (log.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.note,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_log_btn_${log.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete log entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


