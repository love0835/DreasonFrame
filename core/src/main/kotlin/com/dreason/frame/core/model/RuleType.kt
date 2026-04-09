package com.dreason.frame.core.model

enum class RuleType {
    APP,
    DOMAIN_EXACT,
    DOMAIN_SUFFIX,
    DOMAIN_KEYWORD,
    IP_CIDR;

    val displayName: String
        get() = when (this) {
            APP -> "應用程式"
            DOMAIN_EXACT -> "精確域名"
            DOMAIN_SUFFIX -> "域名後綴"
            DOMAIN_KEYWORD -> "域名關鍵字"
            IP_CIDR -> "IP 段"
        }
}
