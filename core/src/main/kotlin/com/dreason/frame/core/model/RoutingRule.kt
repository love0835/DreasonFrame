package com.dreason.frame.core.model

data class RoutingRule(
    val id: Long = 0,
    val type: RuleType,
    val pattern: String,
    val route: Route,
    val priority: Int = 0,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val groupTag: String? = null,
)
