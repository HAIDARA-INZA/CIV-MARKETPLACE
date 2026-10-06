package ci.devsphere.civmarketplace.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.util.PusherManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConversationsFragment : Fragment() {

    @Inject lateinit var pusherManager: PusherManager
    @Inject lateinit var tokenManager: TokenManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
        adapter = ConversationAdapter()
        recyclerView.adapter = adapter
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val userId = tokenManager.getUserId().first() ?: return@launch
            pusherManager.init()
            pusherManager.subscribeToPrivateConversation(userId) { payload ->
                val conversationId = payload.optInt("conversation_id")
                val lastMessage = payload.optString("last_message")
                val lastMessageTime = payload.optString("last_message_time")
                val unreadCount = payload.optInt("unread_count")

                val item = ConversationItem(
                    conversationId = conversationId,
                    title = "Conversation $conversationId",
                    lastMessage = lastMessage,
                    lastMessageTime = lastMessageTime,
                    unreadCount = unreadCount
                )

                val current = adapter.currentList.toMutableList()
                val index = current.indexOfFirst { it.conversationId == conversationId }
                if (index >= 0) {
                    current[index] = item
                } else {
                    current.add(0, item)
                }

                adapter.submitList(current.sortedByDescending { it.lastMessageTime })
            }
        }
    }
}

data class ConversationItem(
    val conversationId: Int,
    val title: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int
)

class ConversationAdapter : ListAdapter<ConversationItem, ConversationViewHolder>(DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = getItem(position)
        holder.text1.text = item.title
        holder.text2.text = "${item.lastMessage} • ${item.unreadCount} non lu(s)"
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ConversationItem>() {
            override fun areItemsTheSame(oldItem: ConversationItem, newItem: ConversationItem): Boolean =
                oldItem.conversationId == newItem.conversationId

            override fun areContentsTheSame(oldItem: ConversationItem, newItem: ConversationItem): Boolean =
                oldItem == newItem
        }
    }
}

class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val text1: TextView = view.findViewById(android.R.id.text1)
    val text2: TextView = view.findViewById(android.R.id.text2)
}

