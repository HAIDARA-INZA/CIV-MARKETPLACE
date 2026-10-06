package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.AnimatedBackground
import ci.devsphere.civmarketplace.ui.components.AutocompleteTextField
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import ci.devsphere.civmarketplace.util.RoleUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState
    val categoriesState by authViewModel.categoriesState

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var commune by remember { mutableStateOf("") }
    var quarter by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("client") }
    val selectedCategories = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        authViewModel.loadCategories()
    }

    LaunchedEffect(profileState) {
        val profile = when (val state = profileState) {
            is ProfileState.Success -> state.profile
            is ProfileState.Updated -> state.profile
            else -> null
        }
        if (profile != null) {
            name = profile.name
            email = profile.email
            phone = profile.phone.orEmpty()
            city = profile.city.orEmpty()
            commune = profile.commune.orEmpty()
            quarter = profile.quarter.orEmpty()
            address = profile.address.orEmpty()
            role = profile.role
            selectedCategories.clear()
            selectedCategories.addAll(profile.vendorCategories.orEmpty())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedBackground()

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = "Modifier le profil", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (profileState is ProfileState.Loading && name.isBlank()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    return@Column
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CIVTextField(value = name, onValueChange = { name = it }, label = "Nom complet")
                        Spacer(modifier = Modifier.height(14.dp))
                        CIVTextField(value = email, onValueChange = { email = it }, label = "E-mail")
                        Spacer(modifier = Modifier.height(14.dp))
                        CIVTextField(value = phone, onValueChange = { phone = it }, label = "Téléphone")
                        Spacer(modifier = Modifier.height(14.dp))
                        AutocompleteTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = "Ville",
                            suggestions = ci.devsphere.civmarketplace.data.LocationData.searchCities(city)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        AutocompleteTextField(
                            value = commune,
                            onValueChange = { commune = it },
                            label = "Commune",
                            suggestions = ci.devsphere.civmarketplace.data.LocationData.searchCommunes(city, commune)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        AutocompleteTextField(
                            value = quarter,
                            onValueChange = { quarter = it },
                            label = "Quartier",
                            suggestions = ci.devsphere.civmarketplace.data.LocationData.searchQuarters(commune, quarter)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CIVTextField(value = address, onValueChange = { address = it }, label = "Adresse / repère", singleLine = false)
                    }
                }

                if (RoleUtils.canSell(role)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Mes Catégories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            when (val state = categoriesState) {
                                CategoriesState.Idle, CategoriesState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                is CategoriesState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                                is CategoriesState.Success -> {
                                    state.categories.forEach { category ->
                                        CategoryRow(
                                            text = category.name,
                                            checked = selectedCategories.contains(category.name),
                                            onToggle = {
                                                if (selectedCategories.contains(category.name)) {
                                                    selectedCategories.remove(category.name)
                                                } else {
                                                    selectedCategories.add(category.name)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                CIVButton(
                    text = "Mettre à jour",
                    onClick = {
                        viewModel.updateProfile(
                            name = name,
                            email = email,
                            phone = phone,
                            city = city,
                            commune = commune,
                            quarter = quarter,
                            address = address,
                            vendorCategories = selectedCategories.toList()
                        )
                    },
                    isLoading = profileState is ProfileState.Saving
                )

                if (profileState is ProfileState.Updated) {
                    Text(
                        text = "Profil enregistré avec succès !",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked, 
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

