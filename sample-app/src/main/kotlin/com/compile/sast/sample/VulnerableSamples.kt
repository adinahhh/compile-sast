package com.compile.sast.sample

import java.security.MessageDigest
import java.sql.Connection

/**
 * Demonstrates the compile-sast K2 plugin catching real issues at compile
 * time. This file is intentionally vulnerable - building :sample-app should
 * fail because of the SAST001/SAST002 errors below.
 */

// SAST001 (ERROR): hardcoded secret-shaped literal in a secret-named property
class HardcodedSecretExample {
    val apiKey = "sk-prod-1234567890abcdef"
}

// Safe: not a constant secret-shaped literal
class SafeSecretExample {
    val apiKey: String = System.getenv("API_KEY") ?: "unset"
}

// SAST002 (ERROR): weak hash algorithm
class WeakCryptoExample {
    fun insecureMd5Hash(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input)
    }
}

// Safe: strong hash algorithm
class SafeCryptoExample {
    fun secureHash(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }
}

// SAST003 (WARNING): non-constant query string
class SqlInjectionExample {
    fun vulnerableQuery(connection: Connection, userId: String): String {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT * FROM users WHERE id = '" + userId + "'")
        return resultSet.toString()
    }
}

// Safe: constant query string
class SafeSqlExample {
    fun safeQuery(connection: Connection): String {
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT * FROM users")
        return resultSet.toString()
    }
}
