package com.car.convoy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 작업 0: Compose가 Pleos 에뮬레이터에서 뜨고 상호작용되는지 확인하는 임시 화면. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var count by remember { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text("Convoy Compose 확인", style = MaterialTheme.typography.headlineLarge)
                    Text("눌린 횟수: $count", style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = { count++ }) {
                        Text("눌러 보기", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
