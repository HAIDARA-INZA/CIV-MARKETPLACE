package ci.devsphere.civmarketplace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ci.devsphere.civmarketplace.ui.components.CIVButton
import ci.devsphere.civmarketplace.ui.components.CIVTextField

import androidx.compose.runtime.LaunchedEffect

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    onNavigateToReset: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val authState by viewModel.authState

    LaunchedEffect(authState) {
        if (authState is AuthState.OtpSent) {
            onNavigateToReset(email)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Récupération",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Entrez votre email pour recevoir un code de réinitialisation.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        CIVTextField(
            value = email,
            onValueChange = { email = it },
            label = "Votre adresse e-mail"
        )

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        CIVButton(
            text = "Envoyer le code",
            onClick = { if (email.isNotBlank()) viewModel.forgotPassword(email) },
            isLoading = authState is AuthState.Loading
        )
        
        TextButton(onClick = onBackToLogin) {
            Text("Retour à la connexion")
        }
    }
}

