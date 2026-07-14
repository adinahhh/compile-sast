package com.compile.sast

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.psi.KtElement

object SastDiagnostics {
    val HARDCODED_SECRET by error1<KtElement, String>()
    val SQL_INJECTION by warning1<KtElement, String>()
    val WEAK_CRYPTO by error1<KtElement, String>()
    val COMMAND_INJECTION by warning1<KtElement, String>()
    val WEAK_TLS by error1<KtElement, String>()
    val INSECURE_DESERIALIZATION by error1<KtElement, String>()
    val SSRF by warning1<KtElement, String>()
    val PATH_TRAVERSAL by warning1<KtElement, String>()
    val LOGGING_SENSITIVE_DATA by warning1<KtElement, String>()
    val XXE by warning1<KtElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(SastDiagnosticRenderers)
    }
}

object SastDiagnosticRenderers : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("Sast").also { map ->
        map.put(SastDiagnostics.HARDCODED_SECRET, "SAST001 [CWE-798] Hardcoded secret: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.WEAK_CRYPTO, "SAST002 [CWE-327] Weak cryptographic algorithm: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.SQL_INJECTION, "SAST003 [CWE-89] Potential SQL injection: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.COMMAND_INJECTION, "SAST004 [CWE-78] Potential command injection: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.WEAK_TLS, "SAST005 [CWE-326] Weak TLS/SSL protocol: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.INSECURE_DESERIALIZATION, "SAST006 [CWE-502] Insecure deserialization: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.SSRF, "SAST007 [CWE-918] Potential SSRF: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.PATH_TRAVERSAL, "SAST008 [CWE-22] Potential path traversal: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.LOGGING_SENSITIVE_DATA, "SAST009 [CWE-532] Potential logging of sensitive data: {0}", CommonRenderers.STRING)
        map.put(SastDiagnostics.XXE, "SAST010 [CWE-611] Potential XML external entity (XXE) injection: {0}", CommonRenderers.STRING)
    }
}
