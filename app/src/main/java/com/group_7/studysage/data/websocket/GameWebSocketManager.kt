package com.group_7.studysage.data.websocket

import com.google.gson.Gson
import com.group_7.studysage.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class GameWebSocketManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val gson = Gson()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _gameMessages = MutableStateFlow<GameMessage?>(null)
    val gameMessages: StateFlow<GameMessage?> = _gameMessages

    fun connect(baseUrl: String, groupId: String, sessionId: String, userId: String, userName: String) {
        val url = "$baseUrl/game/ws?groupId=$groupId&sessionId=$sessionId&userId=$userId&userName=$userName"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    // Parse the backend message format
                    val rawMessage = gson.fromJson(text, Map::class.java) as Map<String, Any>
                    val type = rawMessage["type"]?.toString() ?: "ERROR"

                    // Parse the data field - it might be a JSON string or an object
                    val data = when (val dataValue = rawMessage["data"]) {
                        is String -> {
                            // If data is a JSON string, parse it as a Map
                            try {
                                gson.fromJson(dataValue, Map::class.java) as? Map<String, Any>
                            } catch (e: Exception) {
                                mapOf("value" to dataValue)
                            }
                        }
                        is Map<*, *> -> dataValue as? Map<String, Any>
                        else -> null
                    }

                    val message = GameMessage(type = type, data = data)
                    _gameMessages.value = message
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Error(e.message ?: "Parse error")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.Disconnected
            }
        })
    }

    fun sendMessage(message: Any) {
        val json = gson.toJson(message)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }
}

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class GameMessage(
    val type: String = "",
    val data: Map<String, Any>? = null,
    val error: String? = null
)
