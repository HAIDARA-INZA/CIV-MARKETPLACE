package ci.devsphere.civmarketplace.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import ci.devsphere.civmarketplace.ui.screens.seller.SellerViewModel
import ci.devsphere.civmarketplace.ui.screens.seller.UploadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    viewModel: SellerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("product") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val isService = productType == "service"

    val uploadState by viewModel.uploadState
    val categoriesState by authViewModel.categoriesState
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            return@rememberLauncherForActivityResult
        }

        val file = File(context.cacheDir, "product_photo_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        imageUri = Uri.fromFile(file).toString()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri?.toString()
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            val shouldShowRationale = (context as? Activity)
                ?.let { activity ->
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.CAMERA
                    )
                } ?: false

            val message = if (shouldShowRationale) {
                "Permission caméra refusée. Autorisez-la pour prendre une photo."
            } else {
                "Permission caméra refusée définitivement. Activez-la dans les paramètres."
            }

            if (!shouldShowRationale) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }

            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.loadCategories()
    }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            snackbarHostState.showSnackbar(
                message = "Votre article a été publié avec succès",
                duration = SnackbarDuration.Short
            )
            delay(700)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un produit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // imePadding() pousse le contenu au-dessus du clavier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showImageSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Cliquez pour choisir une image")
                }
            }

            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    title = { Text("Ajouter une photo") },
                    text = { Text("Choisissez la source de votre image.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch("image/*")
                        }) {
                            Text("Galerie")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showImageSourceDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Text("Prendre une photo")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Type d’annonce", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = productType == "product",
                    onClick = {
                        productType = "product"
                        stock = if (stock.isBlank()) "" else stock
                    },
                    label = { Text("Produit") }
                )
                FilterChip(
                    selected = productType == "service",
                    onClick = {
                        productType = "service"
                        stock = ""
                    },
                    label = { Text("Service") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            CIVTextField(value = name, onValueChange = { name = it }, label = if (productType == "service") "Nom du service" else "Nom du produit")
            Spacer(modifier = Modifier.height(16.dp))
            CIVTextField(
                value = price,
                onValueChange = { price = it },
                label = "Prix (FCFA)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CIVTextField(
                value = stock,
                onValueChange = { if (!isService) stock = it },
                label = "Stock disponible",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isService
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Catégorie", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            when (val state = categoriesState) {
                CategoriesState.Idle, CategoriesState.Loading -> CircularProgressIndicator()
                is CategoriesState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is CategoriesState.Success -> {
                    if (state.categories.isEmpty()) {
                        Text("Aucune catégorie configurée sur le serveur", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.categories) { item ->
                                FilterChip(
                                    selected = category == item.name,
                                    onClick = { category = item.name },
                                    label = { Text(item.name) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            CIVTextField(value = description, onValueChange = { description = it }, label = "Description", singleLine = false)

            if (uploadState is UploadState.Error) {
                Text(
                    text = (uploadState as UploadState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uploadState is UploadState.Pending) {
                Text(
                    text = "Publication en attente de connexion. Elle sera envoyée automatiquement.",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            CIVButton(
                text = "Publier le produit",
                onClick = {
                    viewModel.addProduct(
                        name        = name,
                        description = description,
                        price       = price,
                        stock       = stock,
                        category    = category,
                        type        = productType,
                        imageUri    = imageUri ?: ""
                    )
                },
                isLoading = uploadState is UploadState.Loading
            )
        }
    }
}

