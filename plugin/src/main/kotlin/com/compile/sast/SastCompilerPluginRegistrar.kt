package com.compile.sast

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class SastCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val enabled = configuration.get(SastConfigurationKeys.ENABLED, true)
        if (!enabled) return

        val config = SastPluginConfig(enabled = enabled)
        FirExtensionRegistrarAdapter.registerExtension(SastFirExtensionRegistrar(config))
    }

    companion object {
        const val PLUGIN_ID = "com.compile.sast.compiler"
    }
}

object SastConfigurationKeys {
    val ENABLED = org.jetbrains.kotlin.config.CompilerConfigurationKey<Boolean>("enabled")
}
