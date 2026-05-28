package edu.pwr.zpi.netwalk

import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import edu.pwr.zpi.netwalk.ui.IperfLogScreen
import edu.pwr.zpi.netwalk.ui.NetworkInfoScreen
import edu.pwr.zpi.netwalk.ui.NetworkViewModel
import edu.pwr.zpi.netwalk.ui.SettingsScreen
import androidx.lifecycle.viewmodel.compose.viewModel as _viewModel

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    object Network : BottomNavItem("network", "Network", Icons.Default.Info)

    object Iperf : BottomNavItem("iperf", "iPerf", Icons.Default.Speed)
}

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

            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()

                val items = listOf(
                    BottomNavItem.Network,
                    BottomNavItem.Iperf,
                )

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Network.route,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(BottomNavItem.Network.route) {
                            NetworkInfoScreen(
                                tm = tm,
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                        composable(BottomNavItem.Iperf.route) {
                            IperfLogScreen(
                                viewModel = viewModel,
                            )
                        }
                    }
                }
            }
        }
    }
}
