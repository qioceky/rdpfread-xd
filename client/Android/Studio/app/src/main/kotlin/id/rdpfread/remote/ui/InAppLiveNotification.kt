package id.rdpfread.remote.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Sisa bus notifikasi in-app dari upstream (Apache-2.0): hanya status
 * sembunyi/tampil overlay melayang yang masih dipakai panel sesi.
 * Overlay chat melayang + polling backend chat sudah dicabut dari fork ini.
 */
internal object InAppLiveNotificationBus {
    var floatingOverlayHidden by mutableStateOf(false)

    fun syncFloatingHiddenFromPrefs(context: Context) {
        val sp = context.getSharedPreferences("xydesk_floating_chat_prefs", Context.MODE_PRIVATE)
        floatingOverlayHidden = sp.getBoolean("hidden_in_session", false)
    }

    fun setFloatingHidden(context: Context, hidden: Boolean) {
        floatingOverlayHidden = hidden
        context.getSharedPreferences("xydesk_floating_chat_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("hidden_in_session", hidden)
            .apply()
    }
}
