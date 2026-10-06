package ci.devsphere.civmarketplace.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import ci.devsphere.civmarketplace.R
import ci.devsphere.civmarketplace.util.PusherManager
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    @Inject lateinit var pusherManager: PusherManager

    private var conversationId: Int = 0
    private var friendId: Int = 0
    private var currentUserId: Int = 0
    private var typingSent = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        conversationId = intent.getIntExtra("conversation_id", 0)
        friendId = intent.getIntExtra("friend_id", 0)
        currentUserId = intent.getIntExtra("user_id", 0)

        setupTypingListener()
        bindRealtime()
    }

    private fun setupTypingListener() {
        val input = findViewById<android.widget.EditText>(R.id.messageInput)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString()?.trim().orEmpty()
                val shouldTyping = text.isNotEmpty()

                if (shouldTyping && !typingSent) {
                    typingSent = true
                    pusherManager.triggerTyping(conversationId, currentUserId, true)
                }

                typingRunnable?.let { mainHandler.removeCallbacks(it) }
                typingRunnable = Runnable {
                    if (typingSent) {
                        typingSent = false
                        pusherManager.triggerTyping(conversationId, currentUserId, false)
                    }
                }
                mainHandler.postDelayed(typingRunnable!!, 800L)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun bindRealtime() {
        pusherManager.init()
        pusherManager.subscribeToPrivateChat(
            conversationId,
            onMessage = { payload ->
                val senderId = payload.optInt("sender_id", 0)
                if (senderId == friendId) {
                    // ici : ajouter le message dans la liste UI
                }
            },
            onTyping = { payload ->
                val userId = payload.optInt("user_id", 0)
                val isTyping = payload.optBoolean("is_typing", false)
                if (userId == friendId) {
                    if (isTyping) showTypingState() else hideTypingState()
                }
            }
        )

        pusherManager.bindPresenceUser(friendId) { payload ->
            val isOnline = payload.optBoolean("is_online", false)
            val lastSeenAt = payload.optString("last_seen_at")
            if (isOnline) {
                updatePresenceHeader("en ligne")
            } else {
                updatePresenceHeader("vu à ${formatLastSeen(lastSeenAt)}")
            }
        }
    }

    private fun showTypingState() {
        runOnUiThread {
            val title = findViewById<android.widget.TextView>(R.id.chatSubtitle)
            title.text = "en train d'écrire..."
        }
    }

    private fun hideTypingState() {
        runOnUiThread {
            val title = findViewById<android.widget.TextView>(R.id.chatSubtitle)
            title.text = "en ligne"
        }
    }

    private fun updatePresenceHeader(text: String) {
        runOnUiThread {
            val title = findViewById<android.widget.TextView>(R.id.chatSubtitle)
            title.text = text
        }
    }

    private fun formatLastSeen(lastSeenAt: String?): String {
        if (lastSeenAt.isNullOrBlank()) return "à l'instant"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            input.timeZone = TimeZone.getTimeZone("UTC")
            val date = input.parse(lastSeenAt) ?: return "à l'instant"
            val output = SimpleDateFormat("HH:mm", Locale.getDefault())
            output.format(date)
        } catch (_: Exception) {
            "à l'instant"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        typingRunnable?.let { mainHandler.removeCallbacks(it) }
    }
}

