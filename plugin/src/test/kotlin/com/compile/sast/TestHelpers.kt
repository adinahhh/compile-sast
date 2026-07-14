package com.compile.sast

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
fun compile(source: String): JvmCompilationResult =
    KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("Sample.kt", source))
        compilerPluginRegistrars = listOf(SastCompilerPluginRegistrar())
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
