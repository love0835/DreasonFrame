package com.dreason.frame.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RoutingRule
import com.dreason.frame.core.model.RuleType
import com.dreason.frame.core.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleViewModel @Inject constructor(
    private val ruleRepository: RuleRepository,
) : ViewModel() {

    val rules: StateFlow<List<RoutingRule>> = ruleRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun addRule(
        type: RuleType,
        pattern: String,
        route: Route,
        priority: Int = 100,
    ) {
        viewModelScope.launch {
            ruleRepository.insert(
                RoutingRule(
                    type = type,
                    pattern = pattern,
                    route = route,
                    priority = priority,
                )
            )
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            ruleRepository.deleteById(id)
        }
    }

    fun toggleRule(rule: RoutingRule) {
        viewModelScope.launch {
            ruleRepository.update(rule.copy(enabled = !rule.enabled))
        }
    }

    fun reimportBuiltInRules() {
        viewModelScope.launch {
            ruleRepository.importBuiltInRules()
        }
    }
}
