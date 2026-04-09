package com.dreason.frame.tunnel.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TunnelModule {
    // VPN service components are created within DreasonVpnService lifecycle.
    // This module provides any shared tunnel-layer dependencies if needed.
}
