package ci.devsphere.civmarketplace.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    email: String,
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val otpFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    fun scrollToField(index: Int, requester: FocusRequester) {
        coroutineScope.launch {
            requester.requestFocus()
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.ResetSuccess) {
            Toast.makeText(context, "Mot de passe modifié avec succès !", Toast.LENGTH_LONG).show()
            onSuccess()
            viewModel.resetState()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Nouveau mot de passe",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            CIVTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = "Code reçu par e-mail",
                modifier = Modifier
                    .focusRequester(otpFocusRequester)
                    .onFocusChanged { if (it.isFocused) scrollToField(1, otpFocusRequester) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        item {
            CIVTextField(
                value = password,
                onValueChange = { password = it },
                label = "Nouveau mot de passe",
                modifier = Modifier
                    .focusRequester(passwordFocusRequester)
                    .onFocusChanged { if (it.isFocused) scrollToField(2, passwordFocusRequester) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        item {
            CIVTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmer le mot de passe",
                modifier = Modifier
                    .focusRequester(confirmPasswordFocusRequester)
                    .onFocusChanged { if (it.isFocused) scrollToField(3, confirmPasswordFocusRequester) },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        if (authState is AuthState.Error) {
            item {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            CIVButton(
                text = "Réinitialiser",
                onClick = {
                    if (password == confirmPassword) {
                        viewModel.resetPassword(email, otp, password)
                    } else {
                        Toast.makeText(context, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                    }
                },
                isLoading = authState is AuthState.Loading
            )
        }
    }
}

