package com.dreason.frame.core.model

enum class Route {
    CHINA_PROXY,
    TAIWAN_PROXY,
    DIRECT,
    REJECT;

    val displayName: String
        get() = when (this) {
            CHINA_PROXY -> "中國代理"
            TAIWAN_PROXY -> "台灣代理"
            DIRECT -> "直連"
            REJECT -> "阻擋"
        }
}
