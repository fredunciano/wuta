package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.WaterTrackerApplication
import com.example.data.gemini.ChatMessage
import com.example.data.gemini.GeminiAssistantAction
import com.example.data.gemini.MessageSender
import com.example.data.local.entity.UserSettingsEntity
import com.example.data.model.BeverageType
import com.example.data.model.HydrationSummary
import com.example.data.model.WaterLog
import com.example.data.sync.SyncState
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    object TriggerGoalCelebration : UiEvent()
    data class AssistantLoggedWater(val amountMl: Int, val beverage: BeverageType, val reply: String) : UiEvent()
}

data class AssistantUiState(
    val isProcessing: Boolean = false,
    val isListening: Boolean = false,
    val partialSpeechText: String = "",
    val lastAction: GeminiAssistantAction? = null,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Hello! I am your Gemini Hydration Assistant. Say or type what you drank (e.g., \"I drank a glass of water\"), and I'll log it automatically for you!"
        )
    )
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WaterTrackerApplication
    private val repository = app.repository
    private val syncManager = app.syncManager
    private val geminiService = app.geminiAssistantService

    val summary: StateFlow<HydrationSummary> = repository.getHydrationSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrationSummary()
        )

    val todayLogs: StateFlow<List<WaterLog>> = repository.getTodayLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<WaterLog>> = repository.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userSettings: StateFlow<UserSettingsEntity> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettingsEntity()
        )

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val isOnline: StateFlow<Boolean> = syncManager.isOnline

    private val _selectedBeverage = MutableStateFlow(BeverageType.WATER)
    val selectedBeverage: StateFlow<BeverageType> = _selectedBeverage.asStateFlow()

    private val _showGoalCelebration = MutableStateFlow(false)
    val showGoalCelebration: StateFlow<Boolean> = _showGoalCelebration.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantUiState())
    val assistantState: StateFlow<AssistantUiState> = _assistantState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    private var previousTotalMl = 0

    init {
        viewModelScope.launch {
            summary.collect { sum ->
                if (previousTotalMl < sum.dailyTargetMl && sum.todayTotalMl >= sum.dailyTargetMl && sum.dailyTargetMl > 0) {
                    _showGoalCelebration.value = true
                    _eventFlow.emit(UiEvent.TriggerGoalCelebration)
                }
                previousTotalMl = sum.todayTotalMl
            }
        }
    }

    fun selectBeverage(beverage: BeverageType) {
        _selectedBeverage.value = beverage
    }

    fun logWater(
        amountMl: Int,
        beverage: BeverageType = _selectedBeverage.value,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.logWater(amountMl, beverage, note)
            _eventFlow.emit(UiEvent.ShowToast("Logged +${amountMl}ml ${beverage.displayName} 💧"))
        }
    }

    fun processAssistantSpeechOrText(input: String) {
        if (input.isBlank()) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = input.trim()
        )

        _assistantState.value = _assistantState.value.copy(
            isProcessing = true,
            chatMessages = _assistantState.value.chatMessages + userMessage
        )

        viewModelScope.launch {
            val currentSum = summary.value
            val result = geminiService.processUserSpeechOrText(
                input = input,
                currentTotalMl = currentSum.todayTotalMl,
                targetMl = currentSum.dailyTargetMl,
                streakDays = currentSum.currentStreakDays
            )

            // Auto-log to the database if action is LOG_WATER
            if (result.action == "LOG_WATER" && result.amountMl != null && result.amountMl > 0) {
                val bev = result.toBeverageType()
                repository.logWater(
                    amountMl = result.amountMl,
                    beverageType = bev,
                    note = if (result.note.isNotBlank()) result.note else "Gemini Assistant Log"
                )
                _eventFlow.emit(UiEvent.AssistantLoggedWater(result.amountMl, bev, result.assistantReply))
            }

            val assistantMsg = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = result.assistantReply,
                loggedAmountMl = if (result.action == "LOG_WATER") result.amountMl else null,
                beverageType = if (result.action == "LOG_WATER") result.toBeverageType() else null
            )

            _assistantState.value = _assistantState.value.copy(
                isProcessing = false,
                lastAction = result,
                chatMessages = _assistantState.value.chatMessages + assistantMsg
            )
        }
    }

    fun setSpeechListening(isListening: Boolean, partialText: String = "") {
        _assistantState.value = _assistantState.value.copy(
            isListening = isListening,
            partialSpeechText = partialText
        )
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteLog(id)
            _eventFlow.emit(UiEvent.ShowToast("Log deleted"))
        }
    }

    fun updateSettings(settings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            // Reschedule reminders
            ReminderScheduler.scheduleReminder(
                context = app,
                intervalMinutes = settings.reminderIntervalMinutes,
                startHour = settings.reminderStartHour,
                endHour = settings.reminderEndHour,
                isEnabled = settings.isRemindersEnabled
            )
        }
    }

    fun sendTestReminder() {
        ReminderScheduler.showHydrationNotification(
            context = app,
            customMessage = "💧 Testing your Hydration Push Reminder! Log your intake now."
        )
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowToast("Test reminder notification sent!"))
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val success = syncManager.syncData()
            if (success) {
                _eventFlow.emit(UiEvent.ShowToast("Synced offline changes to cloud ☁️"))
            } else {
                if (!isOnline.value) {
                    _eventFlow.emit(UiEvent.ShowToast("Offline mode. Changes queued safely."))
                } else {
                    _eventFlow.emit(UiEvent.ShowToast("Syncing in progress..."))
                }
            }
        }
    }

    fun dismissCelebration() {
        _showGoalCelebration.value = false
    }
}

