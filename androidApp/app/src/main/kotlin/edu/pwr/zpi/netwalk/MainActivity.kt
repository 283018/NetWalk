package edu.pwr.zpi.netwalk

import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import edu.pwr.zpi.netwalk.ui.IperfLogScreen
import edu.pwr.zpi.netwalk.ui.NetworkInfoScreen
import edu.pwr.zpi.netwalk.ui.NetworkViewModel
import edu.pwr.zpi.netwalk.ui.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel as _viewModel

class MainActivity : ComponentActivity() {
    lateinit var tm: TelephonyManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsRepository = SettingsRepository(this)

        setContent {
            val viewModel: NetworkViewModel = _viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        NetworkViewModel(settingsRepository) as T
                },
            )

            MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "network") {
                    composable("network") {
                        NetworkInfoScreen(
                            tm = tm,
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToIperf = { navController.navigate("iperf") },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable("iperf") {
                        IperfLogScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
