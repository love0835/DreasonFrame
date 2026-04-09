package com.dreason.frame.tunnel.routing

import com.dreason.frame.core.model.Route

/**
 * Core routing decision engine.
 *
 * Evaluates rules in priority order:
 * 1. App rules (highest priority — explicit per-app overrides)
 * 2. Domain rules (checked via FakeIP reverse lookup)
 * 3. IP/CIDR rules
 * 4. Default route (fallback)
 *
 * When a proxy route is selected but no proxy server is configured for that route,
 * traffic automatically falls back to DIRECT (phone's own network).
 */
class RuleEngine(
    private val appMatcher: AppMatcher,
    private val domainMatcher: DomainMatcher,
    private val cidrMatcher: CidrMatcher,
    private val defaultRoute: Route = Route.DIRECT,
    private val hasChinaProxy: Boolean = false,
    private val hasTaiwanProxy: Boolean = false,
) {

    data class RouteDecision(
        val route: Route,
        val matchType: String,
        val matchedPattern: String? = null,
        val isFallback: Boolean = false,
    )

    /**
     * Determine the route for a connection.
     *
     * @param packageName The app's package name (from UID resolution), or null
     * @param domain The destination domain (from FakeIP lookup), or null
     * @param destIp The destination IP address string
     * @return RouteDecision with the chosen route and match info
     */
    fun resolve(packageName: String?, domain: String?, destIp: String): RouteDecision {
        // 1. Check app rules first (highest priority)
        if (packageName != null) {
            appMatcher.match(packageName)?.let { route ->
                return applyFallback(RouteDecision(route, "APP", packageName))
            }
        }

        // 2. Check domain rules
        if (domain != null) {
            domainMatcher.match(domain)?.let { route ->
                return applyFallback(RouteDecision(route, "DOMAIN", domain))
            }
        }

        // 3. Check IP/CIDR rules
        cidrMatcher.match(destIp)?.let { route ->
            return applyFallback(RouteDecision(route, "IP_CIDR", destIp))
        }

        // 4. Default route
        return applyFallback(RouteDecision(defaultRoute, "DEFAULT"))
    }

    /**
     * If the selected route requires a proxy that isn't configured,
     * fall back to DIRECT (use phone's own network).
     */
    private fun applyFallback(decision: RouteDecision): RouteDecision {
        return when {
            decision.route == Route.CHINA_PROXY && !hasChinaProxy ->
                decision.copy(route = Route.DIRECT, isFallback = true)
            decision.route == Route.TAIWAN_PROXY && !hasTaiwanProxy ->
                decision.copy(route = Route.DIRECT, isFallback = true)
            else -> decision
        }
    }
}
