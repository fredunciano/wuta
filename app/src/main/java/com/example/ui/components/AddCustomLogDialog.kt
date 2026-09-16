package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BeverageType
import kotlin.math.roundToInt

@Composable
fun AddCustomLogDialog(
    initialBeverage: BeverageType = BeverageType.WATER,
    onDismiss: () -> Unit,
    onConfirm: (amountMl: Int, beverage: BeverageType, note: String) -> Unit
) {
    var amountFloat by remember { mutableFloatStateOf(350f) }
    var selectedBeverage by remember { mutableStateOf(initialBeverage) }
    var noteText by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("350") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = selectedBeverage.defaultColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Custom Drink",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Large Amount Display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${amountFloat.roundToInt()} ml",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        val effectiveMl = (amountFloat * selectedBeverage.hydrationFactor).roundToInt()
                        Text(
                            text = "Effective Hydration: $effectiveMl ml (${(selectedBeverage.hydrationFactor * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Slider
                Slider(
                    value = amountFloat,
                    onValueChange = {
                        amountFloat = it
                        amountInputText = it.roundToInt().toString()
                    },
                    valueRange = 50f..2000f,
                    steps = 38, // steps of 50ml
                    modifier = Modifier.fillMaxWidth().testTag("custom_amount_slider")
                )

                // Beverage selector
                Text(
                    text = "Drink Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BeverageType.entries.forEach { bev ->
                        val isSel = bev == selectedBeverage
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedBeverage = bev },
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(bev.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                selectedContainerColor = bev.defaultColor,
                                selectedLabelColor = Color(0xFF003258)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional encrypted note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Private Note (Encrypted)") },
                    placeholder = { Text("e.g. Post-workout, with lemon") },
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "AES-256 Encrypted",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = amountFloat.roundToInt().coerceAtLeast(10)
                    onConfirm(finalAmount, selectedBeverage, noteText)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedBeverage.defaultColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("custom_log_confirm_btn")
            ) {
                Text("Log Drink", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("custom_log_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
