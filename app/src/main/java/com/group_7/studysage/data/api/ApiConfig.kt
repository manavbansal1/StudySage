package com.group_7.studysage.data.api

/**
 * Centralized API configuration for easy switching between local and remote URLs
 *
 * To use ngrok:
 * 1. Start your backend: cd studysage-backend && ./gradlew run
 * 2. In another terminal, run: ngrok http 8080
 * 3. Copy the https URL from ngrok (e.g., https://abc123.ngrok.io)
 * 4. Paste it in NGROK_URL below
 * 5. Set USE_NGROK = true
 */
object ApiConfig {

    // Toggle between local and ngrok
    private const val USE_NGROK = true

    // Your ngrok URL (update this when you start ngrok)
    private const val NGROK_URL = "https://unrefreshing-dusti-unflouted.ngrok-free.dev"
    //private const val NGROK_URL = "https://perorational-maegan-hexaplar.ngrok-free.dev"

    // Local development URLs
    private const val LOCAL_HTTP_URL = "http://10.0.2.2:8080"
    private const val LOCAL_WS_URL = "ws://10.0.2.2:8080"

    // Active URLs based on configuration
    val BASE_HTTP_URL: String
        get() = if (USE_NGROK) NGROK_URL else LOCAL_HTTP_URL

    val BASE_WS_URL: String
        get() = if (USE_NGROK) {
            // Convert https to wss for WebSocket
            NGROK_URL.replace("https://", "wss://")
        } else {
            LOCAL_WS_URL
        }

    /**
     * Get current configuration info for debugging
     */
    fun getConfigInfo(): String {
        return """
            API Configuration:
            - Mode: ${if (USE_NGROK) "NGROK" else "LOCAL"}
            - HTTP URL: $BASE_HTTP_URL
            - WebSocket URL: $BASE_WS_URL
        """.trimIndent()
    }
}
