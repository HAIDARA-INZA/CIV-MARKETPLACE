package ci.devsphere.civmarketplace.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ci.devsphere.civmarketplace.data.model.ChatMessageDto
import ci.devsphere.civmarketplace.util.TimeUtils
import ci.devsphere.civmarketplace.ui.theme.AccentContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    userId: Int,
    userName: String,
    productId: Int? = null,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var messageText by rememberSaveable { mutableStateOf("") }
    var editingMessageId by rememberSaveable { mutableStateOf<Int?>(null) }
    val messagesState by viewModel.messagesState
    val typingState by viewModel.typingState
    val presenceState by viewModel.presenceState
    val currentUserId by viewModel.currentUserIdState
    val selectedProductId = viewModel.selectedProductId
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val filteredMessages = remember(messagesState, selectedProductId.value) {
        val current = (messagesState as? ChatState.Success)?.data.orEmpty()
        if (selectedProductId.value == null) current else current.filter { it.matchesProductFilter(selectedProductId.value) }
    }

    LaunchedEffect(userId, productId) {
        viewModel.setSelectedProductId(productId)
        viewModel.loadMessages(userId)
        viewModel.loadPresence(userId)
    }

    DisposableEffect(userId) {
        viewModel.setConversationVisible(userId, true)
        onDispose {
            viewModel.setConversationVisible(userId, false)
            viewModel.stopTyping(userId)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar Circulaire
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    color = AccentContent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (typingState) {
                                    Text(
                                        "en train d'écrire...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                } else if (presenceState?.isOnline == true) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("En ligne", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                } else {
                                    Text(
                                        text = presenceState?.lastSeenAt?.let {
                                            "Vu ${TimeUtils.humanReadableDateTime(it)}"
                                        } ?: "Hors ligne",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Retour", 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Column {
                    val productContexts = remember(messagesState) {
                        val current = (messagesState as? ChatState.Success)?.data.orEmpty()
                        current.filter { it.productId != null }
                            .distinctBy { it.productId }
                            .sortedByDescending { it.timestamp }
                    }

                    if (productContexts.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ProductContextChip(
                                    productName = "Tout afficher",
                                    productImageUrl = null,
                                    isSelected = selectedProductId.value == null,
                                    unreadCount = 0,
                                    onClick = { viewModel.setSelectedProductId(null) }
                                )
                            }
                            items(productContexts) { message ->
                                ProductContextChip(
                                    productName = message.productName ?: "Produit",
                                    productImageUrl = message.productImageUrl,
                                    isSelected = selectedProductId.value == message.productId,
                                    unreadCount = 0,
                                    onClick = { viewModel.setSelectedProductId(message.productId) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = {
                                messageText = it
                                viewModel.onComposerChanged(userId, it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            placeholder = { Text("Votre message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    if (editingMessageId != null) {
                                        viewModel.updateMessage(editingMessageId!!, messageText)
                                        editingMessageId = null
                                    } else {
                                        viewModel.sendMessage(userId, messageText, productId = selectedProductId.value)
                                    }
                                    messageText = ""
                                    viewModel.stopTyping(userId)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = AccentContent,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)) {
            when (val state = messagesState) {
                is ChatState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ChatState.Success -> {
                    val visibleMessages = if (selectedProductId.value == null) {
                        state.data
                    } else {
                        state.data.filter { it.matchesProductFilter(selectedProductId.value) }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState,
                        reverseLayout = true
                    ) {
                        if (viewModel.hasMoreState.value) {
                            item {
                                TextButton(
                                    onClick = viewModel::loadMoreMessages,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Charger les anciens messages")
                                }
                            }
                        }
                        if (visibleMessages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aucun message pour cet article.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(visibleMessages.sortedByDescending { it.timestamp }) { message ->
                                val isMe = message.senderId == currentUserId
                                MessageBubble(
                                    message = message,
                                    isMe = isMe,
                                    onRetry = if (isMe && message.status == "failed" && message.clientMessageId != null) {
                                        { viewModel.retryMessage(message.clientMessageId) }
                                    } else null
                                )
                            }
                        }
                    }
                }
                is ChatState.Error -> ci.devsphere.civmarketplace.ui.components.ErrorStateView(
                    error = state.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun ProductContextChip(
    productName: String,
    productImageUrl: String?,
    isSelected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val itemColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = itemColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!productImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = productImageUrl,
                    contentDescription = productName,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("•", color = MaterialTheme.colorScheme.primary) }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = productName.take(18),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (unreadCount > 0) {
                    Text(
                        text = "$unreadCount non lu${if (unreadCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessageDto, isMe: Boolean, onRetry: (() -> Unit)? = null) {
    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    val backgroundBrush = if (isMe) {
        val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        Brush.horizontalGradient(colors) // Couleurs du thème iOS 26
    } else {
        // Fond adaptatif pour le message reçu
        val receivedColor = MaterialTheme.colorScheme.surfaceVariant
        Brush.linearGradient(listOf(receivedColor, receivedColor))
    }

    val textColor = if (isMe) AccentContent else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(backgroundBrush, bubbleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.message ?: "",
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        val deliveryText = when (message.status) {
            "sending" -> "Envoi..."
            "pending" -> "En attente"
            "failed" -> "Echec"
            else -> TimeUtils.formatChatMessageTime(message.timestamp)
        }

        val tickText = when {
            !isMe -> ""
            message.status == "sending" || message.status == "pending" -> "◷"
            message.status == "failed" -> "!"
            message.readAt != null -> "✓✓"
            message.deliveredAt != null -> "✓✓"
            message.id > 0 -> "✓"
            else -> "◷"
        }

        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = deliveryText,
                fontSize = 10.sp,
                color = if (message.status == "failed") {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (tickText.isNotBlank()) {
                Text(
                    text = tickText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        message.status == "failed" -> MaterialTheme.colorScheme.error
                        message.readAt != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text("Réessayer", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


