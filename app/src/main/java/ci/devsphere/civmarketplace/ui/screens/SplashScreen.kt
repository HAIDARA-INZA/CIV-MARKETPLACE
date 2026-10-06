package ci.devsphere.civmarketplace.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ci.devsphere.civmarketplace.R
import ci.devsphere.civmarketplace.ui.theme.AccentContent
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (String) -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SplashViewModel.SplashEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                is SplashViewModel.SplashEvent.NavigateToLogin -> onNavigateToLogin()
                is SplashViewModel.SplashEvent.NavigateToHome -> onNavigateToHome(event.role)
                is SplashViewModel.SplashEvent.NavigateToVerification -> onNavigateToHome("verify_email/${Uri.encode(event.email)}")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(150.dp),
                shape = CircleShape,
                color = AccentContent,
                shadowElevation = 8.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.inza),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize().padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CIV Marketplace",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

