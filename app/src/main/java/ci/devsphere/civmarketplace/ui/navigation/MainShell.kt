package ci.devsphere.civmarketplace.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ci.devsphere.civmarketplace.ui.screens.*
import ci.devsphere.civmarketplace.ui.screens.client.*
import ci.devsphere.civmarketplace.ui.screens.seller.*
import ci.devsphere.civmarketplace.ui.theme.AccentContent
import ci.devsphere.civmarketplace.ui.theme.BackgroundDark
import ci.devsphere.civmarketplace.ui.theme.LocalAppDarkTheme
import ci.devsphere.civmarketplace.util.RoleUtils

@Composable
fun MainShell(
    role: String,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToVendorStore: (Int, String) -> Unit,
    onNavigateToChat: (Int, String, Int?) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var isBottomBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y < -8) isBottomBarVisible = false
                else if (available.y > 8) isBottomBarVisible = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val canSell = RoleUtils.canSell(role)
    val canBuy  = RoleUtils.canBuy(role)
    var sellerSpaceActive by rememberSaveable { mutableStateOf(false) }

    val clientItems = listOf(
        NavigationItem("Accueil",   Screen.Home.route,    Icons.Default.Home),
        NavigationItem("Recherche", "search",             Icons.Default.Search),
        NavigationItem("Messages",  "messages",           Icons.AutoMirrored.Filled.Message),
        NavigationItem("Panier",    Screen.Cart.route,    Icons.Default.ShoppingCart),
        NavigationItem("Profil",    Screen.Profile.route, Icons.Default.Person)
    )
    val sellerItems = listOf(
        NavigationItem("Dashboard", "seller_home",        Icons.Default.Dashboard),
        NavigationItem("Ma boutique", "manage_store",     Icons.Default.Storefront),
        NavigationItem("Messages",  "messages",           Icons.AutoMirrored.Filled.Message),
        NavigationItem("Profil",    Screen.Profile.route, Icons.Default.Person)
    )
    val mixedItems = listOf(
        NavigationItem("Accueil",   Screen.Home.route,    Icons.Default.Home),
        NavigationItem("Dashboard", "seller_home",        Icons.Default.Dashboard),
        NavigationItem("Ma boutique", "manage_store",     Icons.Default.Storefront),
        NavigationItem("Messages",  "messages",           Icons.AutoMirrored.Filled.Message),
        NavigationItem("Profil",    Screen.Profile.route, Icons.Default.Person)
    )

    val items = when {
        canSell && canBuy -> if (sellerSpaceActive) sellerItems else clientItems
        canSell           -> sellerItems
        else              -> clientItems
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            LiquidGlassNavBar(
                items = items,
                currentDestination = currentDestination,
                visible = isBottomBarVisible,
                onItemClick = { item ->
                    if (item.route == Screen.Cart.route) {
                        onNavigateToCart()
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController    = navController,
                startDestination = if (canSell && !canBuy) "seller_home" else Screen.Home.route,
                modifier         = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToCart   = onNavigateToCart,
                        onNavigateToChat   = onNavigateToChat
                    )
                }
                composable("search") {
                    SearchScreen(
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToVendorStore = onNavigateToVendorStore
                    )
                }
                composable("seller_home") {
                    SellerDashboardScreen(onManageStore = {
                        navController.navigate("manage_store") { launchSingleTop = true }
                    })
                }
                composable(Screen.Profile.route) {
                    ClientProfileScreen(
                        role                   = role,
                        onNavigateToOrders     = onNavigateToOrders,
                        onNavigateToFavorites  = onNavigateToFavorites,
                        onNavigateToSettings   = onNavigateToSettings,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToDashboard  = {
                            navController.navigate("seller_home") { launchSingleTop = true }
                        },
                        onLogout = onLogout
                    )
                }
                composable("manage_store") {
                    ManageStoreScreen(onAddProduct = { navController.navigate("add_product") })
                }
                composable("add_product") {
                    AddProductScreen(onBack = { navController.popBackStack() })
                }
                composable("messages") {
                    MessagesScreen(onNavigateToChat = onNavigateToChat)
                }
            }
            if (canSell && canBuy) {
                DraggableSpaceSwitchButton(
                    sellerSpaceActive = sellerSpaceActive,
                    onSwitch = {
                        sellerSpaceActive = !sellerSpaceActive
                        val target = if (sellerSpaceActive) "seller_home" else Screen.Home.route
                        navController.popBackStack(navController.graph.findStartDestination().id, false)
                        if (target != Screen.Home.route) {
                            navController.navigate(target) { launchSingleTop = true }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Bouton flottant pour basculer entre "Espace client" et "Espace vendeur",
 * librement déplaçable sur tout l'écran façon AssistiveTouch d'iPhone :
 * on le glisse où on veut, il se recolle au bord gauche ou droit le plus
 * proche au relâchement, et un simple tap (sans glisser) déclenche le switch.
 * Position par défaut : bord gauche.
 */
@Composable
private fun BoxScope.DraggableSpaceSwitchButton(
    sellerSpaceActive: Boolean,
    onSwitch: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val buttonSizePx = with(density) { 56.dp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Position par défaut : bord gauche, un peu au-dessus du milieu de l'écran.
        var offsetX by rememberSaveable { mutableStateOf(0f) }
        var offsetY by rememberSaveable { mutableStateOf(maxHeightPx * 0.35f) }

        val animatedX = remember { androidx.compose.animation.core.Animatable(offsetX) }
        val animatedY = remember { androidx.compose.animation.core.Animatable(offsetY) }
        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(offsetX, offsetY, isDragging) {
            if (!isDragging) {
                animatedX.animateTo(offsetX, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                animatedY.animateTo(offsetY, animationSpec = spring())
            } else {
                animatedX.snapTo(offsetX)
                animatedY.snapTo(offsetY)
            }
        }

        val alpha by animateFloatAsState(if (isDragging) 1f else 0.85f, label = "dragAlpha")

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(animatedX.value.toInt(), animatedY.value.toInt()) }
                .size(56.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Se recolle au bord le plus proche (gauche ou droite), comme sur iPhone.
                            val snapToLeft = (offsetX + buttonSizePx / 2f) < (maxWidthPx / 2f)
                            offsetX = if (snapToLeft) 0f else (maxWidthPx - buttonSizePx).coerceAtLeast(0f)
                            offsetY = offsetY.coerceIn(0f, (maxHeightPx - buttonSizePx).coerceAtLeast(0f))
                        },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, (maxWidthPx - buttonSizePx).coerceAtLeast(0f))
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, (maxHeightPx - buttonSizePx).coerceAtLeast(0f))
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSwitch
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (sellerSpaceActive) Icons.Default.Person else Icons.Default.Storefront,
                contentDescription = if (sellerSpaceActive) "Passer en espace client" else "Passer en espace vendeur",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LiquidGlassNavBar(
    items: List<NavigationItem>,
    currentDestination: androidx.navigation.NavDestination?,
    visible: Boolean,
    onItemClick: (NavigationItem) -> Unit
) {
    val accentBlue = MaterialTheme.colorScheme.primary
    val accentCyan = MaterialTheme.colorScheme.secondary
    val isDarkTheme = LocalAppDarkTheme.current

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter   = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) +
                  androidx.compose.animation.fadeIn(),
        exit    = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) +
                  androidx.compose.animation.fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().also {
                                it.asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.TRANSPARENT
                                    setShadowLayer(
                                        24f, 0f, 8f,
                                        accentBlue.copy(alpha = 0.4f).toArgb()
                                    )
                                }
                            }
                            canvas.drawRoundRect(
                                left   = 0f,
                                top    = 0f,
                                right  = size.width,
                                bottom = size.height,
                                radiusX = 36.dp.toPx(),
                                radiusY = 36.dp.toPx(),
                                paint  = paint
                            )
                        }
                    }
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        if (isDarkTheme)
                            BackgroundDark.copy(alpha = 0.85f) 
                        else 
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    )
                    .border(
                        width = 0.6.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                accentBlue.copy(alpha = 0.7f),
                                accentCyan.copy(alpha = 0.6f),
                                if (isDarkTheme) AccentContent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    LiquidNavItem(
                        item       = item,
                        selected   = selected,
                        accentBlue = accentBlue,
                        accentCyan = accentCyan,
                        onClick    = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.LiquidNavItem(
    item: NavigationItem,
    selected: Boolean,
    accentBlue: Color,
    accentCyan: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue    = if (selected) 1.12f else 1f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val bubbleAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "bubbleAlpha"
    )

    val iconAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0.45f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconAlpha"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (bubbleAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        accentBlue.copy(alpha = 0.30f * bubbleAlpha),
                                        accentCyan.copy(alpha = 0.15f * bubbleAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accentBlue.copy(alpha = 0.22f * bubbleAlpha),
                                        accentCyan.copy(alpha = 0.18f * bubbleAlpha)
                                    )
                                )
                            )
                            .border(
                                width = 0.5.dp,
                                color = AccentContent.copy(alpha = 0.18f * bubbleAlpha),
                                shape = RoundedCornerShape(18.dp)
                            )
                    )
                }

                Icon(
                    imageVector     = item.icon,
                    contentDescription = item.label,
                    tint            = if (selected) accentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = iconAlpha),
                    modifier        = Modifier
                        .size(22.dp)
                        .scale(scale)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text       = item.label,
                fontSize   = 9.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) accentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                maxLines   = 1
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

