package com.example.promaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.promaster.data.firebase.FirebaseManager
import com.example.promaster.navigation.PromasterNavHost
import com.example.promaster.theme.PROMASTERTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseManager.initialize(applicationContext)

    enableEdgeToEdge()
    setContent {
      PROMASTERTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          PromasterNavHost()
        }
      }
    }
  }
}
