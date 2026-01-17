package com.artic.cqt

import android.content.ComponentName
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class BridgeApprovalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callerPackage = intent.getStringExtra("callerPackage") ?: "Unknown App"
        val tileId = intent.getIntExtra("tileId", -1)
        val label = intent.getStringExtra("label") ?: "N/A"
        val actionType = intent.getStringExtra("actionType") ?: "N/A"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    ApprovalDialog(
                        callerPackage = callerPackage,
                        tileId = tileId,
                        label = label,
                        actionType = actionType,
                        onAccept = {
                            BridgeSettings(this).setWhitelisted(callerPackage, true)
                            applyChanges()
                            finish()
                        },
                        onDeny = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun applyChanges() {
        val tileId = intent.getIntExtra("tileId", -1)
        val prefs = TilePreferences(this, tileId)
        
        intent.getStringExtra("label")?.let { prefs.tileLabel = it }
        intent.getStringExtra("subtitle")?.let { prefs.tileSubtitle = it }
        intent.getStringExtra("actionType")?.let { prefs.tileActionType = it }
        intent.getStringExtra("actionValue")?.let { prefs.tileActionValue = it }
        intent.getStringExtra("iconUri")?.let { 
            prefs.iconType = "GALLERY"
            prefs.iconValue = it 
        }

        val className = "com.artic.cqt.QuickTileService$tileId"
        TileService.requestListeningState(this, ComponentName(this, className))
    }
}

@Composable
fun ApprovalDialog(
    callerPackage: String,
    tileId: Int,
    label: String,
    actionType: String,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    "Bridge Request",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "An external app is trying to modify your Quick Tiles.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRow("Caller App:", callerPackage)
                    InfoRow("Target Tile:", "#$tileId")
                    InfoRow("New Label:", label)
                    InfoRow("New Action:", actionType)
                }

                HorizontalDivider()

                Text(
                    "Whitelisting this app will allow it to modify your tiles in the future without asking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeny,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Block")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Allow")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
