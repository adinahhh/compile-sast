package com.compile.sast

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SastFirExtensionRegistrar(private val config: SastPluginConfig) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: org.jetbrains.kotlin.fir.FirSession -> SastAdditionalCheckersExtension(session, config) }
    }
}
