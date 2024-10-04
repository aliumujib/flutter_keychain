package be.appmire.flutterkeychain

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class FlutterKeychainPlugin : FlutterPlugin, MethodCallHandler {

    private var channel: MethodChannel? = null
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val CHANNEL_NAME = "com.circlemedical/cm_storage_plugin"

        @JvmStatic
        fun registerWith(registrar: io.flutter.plugin.common.PluginRegistry.Registrar) {
            try {
                val preferences = provideSharedPreferences(registrar.context())
                val instance = FlutterKeychainPlugin()
                instance.sharedPreferences = preferences
                instance.channel = MethodChannel(registrar.messenger(), CHANNEL_NAME)
                instance.channel?.setMethodCallHandler(instance)
            } catch (e: Exception) {
                Log.e("FlutterKeychainPlugin", "Could not register plugin", e)
            }
        }

        private fun provideSharedPreferences(context: Context): SharedPreferences {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    "cm_shared_prefs_store",
                    masterKeyAlias,
                    context.applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                context.getSharedPreferences("cm_shared_prefs", Context.MODE_PRIVATE)
            }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        sharedPreferences = provideSharedPreferences(binding.applicationContext)

        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel!!.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        try {
            when (call.method) {
                "get" -> {
                    val value: String? = sharedPreferences.getString(call.argument("key"), null)
                    result.success(value)
                }
                "put" -> {
                    val value = call.argument<String>("value")
                    sharedPreferences.edit().putString(call.argument("key"), value).apply()
                    result.success(null)
                }
                "remove" -> {
                    sharedPreferences.edit().remove(call.argument("key")).apply()
                    result.success(null)
                }
                "clear" -> {
                    sharedPreferences.edit().clear().apply()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        } catch (e: Exception) {
            Log.e("FlutterKeychainPlugin", e.message ?: e.toString())
            result.error("FlutterKeychainPlugin", e.message, e)
        }
    }
}
