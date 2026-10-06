package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.CIVTextField
import kotlinx.coroutines.delay

@Composable
fun VerificationScreen(
    email: String,
    onVerificationSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var otp by remember { mutableStateOf("") }
    val authState by viewModel.authState

    // Cooldown pour le bouton "Renvoyer"
    var cooldown by remember { mutableIntStateOf(0) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown--
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onVerificationSuccess()
            is AuthState.OtpSent -> {
                // Code renvoyé → reset cooldown à 60s
                cooldown = 60
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Vérifiez votre compte",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Saisissez le code à 6 chiffres envoyé à\n$email",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        CIVTextField(
            value = otp,
            onValueChange = { value -> if (value.all(Char::isDigit) && value.length <= 6) otp = value },
            label = "Code à 6 chiffres",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )

        if (authState is AuthState.Error || authState is AuthState.OtpSent) {
            val msg = when (val s = authState) {
                is AuthState.Error  -> "Le code n'a pas pu être vérifié : ${s.message}. Vérifiez votre connexion puis réessayez."
                is AuthState.OtpSent -> "✅ Nouveau code envoyé !"
                else -> ""
            }
            val color = if (authState is AuthState.OtpSent)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error

            Text(
                text = msg,
                color = color,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CIVButton(
            text = "Vérifier le compte",
            onClick = { viewModel.verifyEmail(email, otp) },
            isLoading = authState is AuthState.Loading,
            enabled = otp.length == 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton Renvoyer avec cooldown
        TextButton(
            onClick = {
                viewModel.resendOtp(email)
                cooldown = 60
            },
            enabled = cooldown == 0 && authState !is AuthState.Loading
        ) {
            if (cooldown > 0) {
                Text(
                    text = "Renvoyer le code (${cooldown}s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "Renvoyer le code",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

