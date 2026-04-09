package com.dreason.frame.tunnel.routing

import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RoutingRule
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple HashMap-based matcher for per-app routing rules.
 * Maps Android package names to Route targets.
 */
class AppMatcher {

    private val rules = ConcurrentHashMap<String, Route>()

    fun addRule(rule: RoutingRule) {
        rules[rule.pattern] = rule.route
    }

    fun addRule(packageName: String, route: Route) {
        rules[packageName] = route
    }

    /**
     * Match a package name to its route.
     * @return The route if matched, null if no match.
     */
    fun match(packageName: String): Route? = rules[packageName]

    fun removeRule(packageName: String) {
        rules.remove(packageName)
    }

    fun clear() {
        rules.clear()
    }

    fun getAllRules(): Map<String, Route> = rules.toMap()
}
