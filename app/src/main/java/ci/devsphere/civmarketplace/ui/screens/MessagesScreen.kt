package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ci.devsphere.civmarketplace.data.model.ConversationDto
import ci.devsphere.civmarketplace.ui.components.ErrorStateView
import ci.devsphere.civmarketplace.util.TimeUtils
import ci.devsphere.civmarketplace.ui.theme.AccentContent
import ci.devsphere.civmarketplace.ui.theme.OnlineStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onNavigateToChat: (Int, String, Int?) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.conversationsState
    val lifecycleOwner = LocalLifecycleOwner.current
    // Bug messagerie #1 : IDs en ligne mis à jour en direct par le presence channel,
    // on les combine avec le is_online "snapshot" renvoyé par l'API au chargement.
    val liveOnlineIds by viewModel.onlineUserIds.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Discussions", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val conversationsState = state) {
                    is ChatState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ChatState.Success -> {
                        val data = conversationsState.data
                        if (data.isEmpty()) {
                            ci.devsphere.civmarketplace.ui.components.EmptyStateView(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                title = "Aucune conversation",
                                subtitle = "Contacte un vendeur depuis une fiche produit pour démarrer une discussion.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(data) { conversation ->
                                    ConversationItem(
                                        conversation = conversation,
                                        isOnline = conversation.isOnline || liveOnlineIds.contains(conversation.otherUserId),
                                        isTyping = typingUsers[conversation.otherUserId] == true,
                                        onClick = { onNavigateToChat(conversation.otherUserId, conversation.otherUserName, null) }
                                    )
                                }
                            }
                        }
                    }
                    is ChatState.Error -> {
                        ErrorStateView(
                            error = conversationsState.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {}
                }
            }
        }
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.loadConversations()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationDto,
    isOnline: Boolean = false,
    isTyping: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            ,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.otherUserName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp
                    )
                }
                // Bug messagerie #1 : petit point vert = en ligne maintenant.
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(OnlineStatus, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.otherUserName, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isTyping) "en train d'écrire..." else conversation.lastMessage,
                    fontSize = 14.sp,
                    color = if (isTyping) OnlineStatus else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = TimeUtils.humanReadableDateTime(conversation.lastMessageTime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("${conversation.unreadCount}", color = AccentContent)
                    }
                }
            }
        }
    }
}

