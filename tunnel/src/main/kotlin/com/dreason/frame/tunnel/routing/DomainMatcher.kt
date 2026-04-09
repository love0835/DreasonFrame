package com.dreason.frame.tunnel.routing

import com.dreason.frame.core.model.Route
import com.dreason.frame.core.model.RoutingRule
import com.dreason.frame.core.model.RuleType

/**
 * Trie-based domain matcher for efficient domain rule matching.
 *
 * Uses a reversed-label trie: "www.baidu.com" is stored as com -> baidu -> www.
 * This enables efficient suffix matching (*.baidu.com matches www.baidu.com, tieba.baidu.com, etc.)
 */
class DomainMatcher {

    private class TrieNode {
        val children = HashMap<String, TrieNode>()
        var suffixRoute: Route? = null  // Matches this domain and all subdomains
        var exactRoute: Route? = null   // Matches only this exact domain
    }

    private val root = TrieNode()
    private val keywordRules = mutableListOf<Pair<String, Route>>()

    fun addRule(rule: RoutingRule) {
        when (rule.type) {
            RuleType.DOMAIN_SUFFIX -> addSuffixRule(rule.pattern, rule.route)
            RuleType.DOMAIN_EXACT -> addExactRule(rule.pattern, rule.route)
            RuleType.DOMAIN_KEYWORD -> keywordRules.add(rule.pattern.lowercase() to rule.route)
            else -> { /* not a domain rule */ }
        }
    }

    private fun addSuffixRule(domain: String, route: Route) {
        val labels = domain.lowercase().split(".").reversed()
        var node = root
        for (label in labels) {
            node = node.children.getOrPut(label) { TrieNode() }
        }
        node.suffixRoute = route
    }

    private fun addExactRule(domain: String, route: Route) {
        val labels = domain.lowercase().split(".").reversed()
        var node = root
        for (label in labels) {
            node = node.children.getOrPut(label) { TrieNode() }
        }
        node.exactRoute = route
    }

    /**
     * Match a domain against all rules.
     * @return The route if matched, null if no match.
     */
    fun match(domain: String): Route? {
        val lowerDomain = domain.lowercase()

        // Try trie-based matching (suffix and exact)
        val trieResult = matchTrie(lowerDomain)
        if (trieResult != null) return trieResult

        // Try keyword matching (slower, checked last)
        for ((keyword, route) in keywordRules) {
            if (lowerDomain.contains(keyword)) return route
        }

        return null
    }

    private fun matchTrie(domain: String): Route? {
        val labels = domain.split(".").reversed()
        var node = root
        var lastSuffixRoute: Route? = null

        for (label in labels) {
            val child = node.children[label] ?: break
            node = child
            // Track the deepest suffix match
            if (node.suffixRoute != null) {
                lastSuffixRoute = node.suffixRoute
            }
        }

        // Check for exact match at the final node
        if (node.exactRoute != null) {
            val labels2 = domain.split(".").reversed()
            // Verify we actually traversed the entire domain
            var checkNode = root
            var matched = true
            for (label in labels2) {
                val child = checkNode.children[label]
                if (child == null) { matched = false; break }
                checkNode = child
            }
            if (matched && checkNode.exactRoute != null) {
                return checkNode.exactRoute
            }
        }

        return lastSuffixRoute
    }
}
