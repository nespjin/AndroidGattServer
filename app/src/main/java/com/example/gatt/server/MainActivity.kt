package com.example.gatt.server

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.gatt.server.ui.theme.ApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel by viewModels<MainActivityViewModel>(factoryProducer = { ViewModelFactory() })


    class ViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return when (modelClass) {
                MainActivityViewModel::class.java -> {
                    val applicationContext =
                        extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    return MainActivityViewModel(applicationContext!!.applicationContext) as T
                }

                else -> super.create(modelClass, extras)
            }
        }
    }


    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            for (result in results) {
                if (!result.value) {
                    Log.d(TAG, "Permission is not granted for ${result.key}")
                    return@registerForActivityResult
                }
            }
            viewModel.startServer()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) { innerPadding ->
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    Column(modifier = Modifier.padding(innerPadding)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.Top,
                        ) {
                            item {
                                Text(text = "名称")
                            }
                            item {
                                TextField(
                                    value = state.bleName,
                                    onValueChange = viewModel::setBluetoothName,
                                )
                            }
                            item {
                                Text(text = "Service UUID")
                            }
                            item {
                                TextField(
                                    value = state.serviceUUID,
                                    onValueChange = viewModel::setServiceUUID,
                                )
                            }
                            item {
                                Text(text = "Writeable CHAR UUID")
                            }
                            item {
                                TextField(
                                    value = state.writeableCharUUID,
                                    onValueChange = viewModel::setWriteableCharUUID,
                                )
                            }
                            item {
                                Text(text = "Notify CHAR UUID")
                            }
                            item {
                                TextField(
                                    value = state.notifyCharUUID,
                                    onValueChange = viewModel::setNotifyCharUUID,
                                )
                            }
                        }
                        Button(onClick = ::requestPermissions) { Text(text = "Advertise") }
                        LogPanel(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        requestPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
            )
        )
    }

}

@Composable
fun LogPanel(
    modifier: Modifier = Modifier,
    viewModel: MainActivityViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Text(text = "Log:")
    LazyColumn {
        item {
            Text(
                text = state.log,
                modifier = modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ApplicationTheme {
        LogPanel()
    }
}
