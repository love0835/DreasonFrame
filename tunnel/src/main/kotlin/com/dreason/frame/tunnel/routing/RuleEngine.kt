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
 */
class RuleEngine(
    private val appMatcher: AppMatcher,
    private val domainMatcher: DomainMatcher,
    private val cidrMatcher: CidrMatcher,
    private val defaultRoute: Route = Route.DIRECT,
) {

    data class RouteDecision(
        val route: Route,
        val matchType: String,
        val matchedPattern: String? = null,
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
                return RouteDecision(route, "APP", packageName)
            }
        }

        // 2. Check domain rules
        if (domain != null) {
            domainMatcher.match(domain)?.let { route ->
                return RouteDecision(route, "DOMAIN", domain)
            }
        }

        // 3. Check IP/CIDR rules
        cidrMatcher.match(destIp)?.let { route ->
            return RouteDecision(route, "IP_CIDR", destIp)
        }

        // 4. Default route
        return RouteDecision(defaultRoute, "DEFAULT")
    }
}
