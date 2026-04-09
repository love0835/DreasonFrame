package com.dreason.frame.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreason.frame.core.model.ConnectionLog
import com.dreason.frame.core.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val logRepository: LogRepository,
) : ViewModel() {

    val logs: StateFlow<List<ConnectionLog>> = logRepository.getRecent()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.deleteAll()
        }
    }
}
