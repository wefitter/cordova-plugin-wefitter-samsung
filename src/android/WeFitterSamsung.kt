package com.wefitter.cordova.plugin

import com.google.gson.Gson
import com.wefitter.shealth.WeFitterSHealth
import com.wefitter.shealth.WeFitterSHealthError
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class WeFitterSamsung : CordovaPlugin() {

    private val weFitter by lazy { WeFitterSHealth(cordova.activity) }
    private val errors = mutableMapOf<WeFitterSHealthError.Reason, WeFitterSHealthError>()

    private lateinit var callbackContextForStatusListener: CallbackContext

    private class Config(
        val token: String,
        val apiUrl: String,
        val notificationTitle: String?,
        val notificationText: String?,
        val notificationIcon: String?,
        val notificationChannelId: String?,
        val notificationChannelName: String?
    )

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        when (action) {
            "configure" -> {
                callbackContextForStatusListener = callbackContext
                configure(args)
                return true
            }
            "connect" -> {
                weFitter.connect()
                return true
            }
            "disconnect" -> {
                weFitter.disconnect()
                return true
            }
            "isConnected" -> {
                isConnected(callbackContext)
                return true
            }
            "tryToResolveError" -> {
                tryToResolveError(args)
                return true
            }
            else -> return false
        }
    }

    private fun configure(args: JSONArray) {
        val config = parseConfig(args)
        if (config === null) {
            sendPluginResult(PluginResult.Status.ERROR, "No (valid) config passed")
        } else {
            val statusListener = object : WeFitterSHealth.StatusListener {
                override fun onConfigured(configured: Boolean) {
                    val message = if (configured) "CONFIGURED" else "NOT_CONFIGURED"
                    sendPluginResult(PluginResult.Status.OK, message)
                }

                override fun onConnected(connected: Boolean) {
                    val message = if (connected) "CONNECTED" else "DISCONNECTED"
                    sendPluginResult(PluginResult.Status.OK, message)
                }

                override fun onError(error: WeFitterSHealthError) {
                    // save error so we can find it again for `tryToResolve` function
                    errors[error.reason] = error
                    val json =
                        """{"reason":"${error.reason.message}","forUser":${error.reason.forUser}}"""
                    sendPluginResult(PluginResult.Status.ERROR, JSONObject(json))
                }
            }
            val notificationConfig = parseNotificationConfig(config)
            weFitter.configure(config.token, config.apiUrl, statusListener, notificationConfig)
        }
    }

    private fun isConnected(callbackContext: CallbackContext) {
        val connected = weFitter.isConnected()
        val result = PluginResult(PluginResult.Status.OK, connected)
        callbackContext.sendPluginResult(result)
    }

    private fun tryToResolveError(args: JSONArray) {
        try {
            val reason = args.getString(0)
            // reverse lookup enum by value
            val map = WeFitterSHealthError.Reason.values()
                .associateBy(WeFitterSHealthError.Reason::message)
            val enum = map[reason]

            // try to resolve source error
            errors[enum]?.let {
                weFitter.tryToResolveError(it)
            }
        } catch (e: JSONException) {
            // ignore
        }
    }

    private fun sendPluginResult(status: PluginResult.Status, message: String) {
        sendPluginResult(PluginResult(status, message))
    }

    private fun sendPluginResult(status: PluginResult.Status, jsonObject: JSONObject) {
        sendPluginResult(PluginResult(status, jsonObject))
    }

    private fun sendPluginResult(result: PluginResult) {
        result.keepCallback = true
        callbackContextForStatusListener.sendPluginResult(result)
    }

    private fun parseConfig(args: JSONArray): Config? {
        return try {
            Gson().fromJson(args.getString(0), Config::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNotificationConfig(config: Config): WeFitterSHealth.NotificationConfig {
        return WeFitterSHealth.NotificationConfig().apply {
            config.notificationTitle?.let { this.title = it }
            config.notificationText?.let { this.text = it }
            config.notificationIcon?.let {
                val resourceId = getResourceId(it)
                if (resourceId != 0) this.iconResourceId = resourceId
            }
            config.notificationChannelId?.let { this.channelId = it }
            config.notificationChannelName?.let { this.channelName = it }
        }
    }

    private fun getResourceId(resourceName: String): Int {
        val resources = cordova.activity.resources
        val packageName = cordova.activity.packageName
        var resourceId = resources.getIdentifier(resourceName, "mipmap", packageName)
        if (resourceId == 0) {
            resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
        }
        if (resourceId == 0) {
            resourceId = resources.getIdentifier(resourceName, "raw", packageName)
        }
        return resourceId
    }
}