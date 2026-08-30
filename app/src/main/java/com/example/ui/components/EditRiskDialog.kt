package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun EditRiskDialog(
    currentRisk: Double,
    onDismiss: () -> Unit,
    onSaveRisk: (Double) -> Unit
) {
    var inputText by remember {
        val initial = if (currentRisk % 1.0 == 0.0) currentRisk.toInt().toString() else String.format(java.util.Locale.US, "%.2f", currentRisk)
        mutableStateOf(initial)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetAmounts = listOf(500.0, 1000.0, 1500.0, 2500.0, 5000.0, 10000.0)

    fun validateAndSave() {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Risk amount cannot be empty"
            return
        }
        val amount = trimmed.toDoubleOrNull()
        if (amount == null) {
            errorMessage = "Please enter a valid numeric amount"
            return
        }
        if (amount <= 0.0) {
            errorMessage = "Risk amount must be greater than ₹0"
            return
        }
        errorMessage = null
        onSaveRisk(amount)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("edit_risk_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanAccentBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "EDIT INTRADAY RISK",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = "Max capital risk per trade",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_risk_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Current Active Risk Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Risk Budget:",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${if (currentRisk % 1.0 == 0.0) currentRisk.toInt() else String.format(java.util.Locale.US, "%.2f", currentRisk)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KeyLevelYellow
                        )
                    }
                }

                // Quick Preset Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetAmounts) { preset ->
                            val isSelected = inputText.toDoubleOrNull() == preset
                            Surface(
                                onClick = {
                                    inputText = preset.toInt().toString()
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CyanAccent else BgCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanAccent else BgCardBorder
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("preset_risk_${preset.toInt()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "₹${preset.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Input Text Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Custom Risk Amount (₹):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            if (errorMessage != null) {
                                errorMessage = null
                            }
                        },
                        leadingIcon = {
                            Text(
                                text = "₹",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        placeholder = { Text("Enter risk (e.g. 2500)", color = TextTertiary, fontSize = 13.sp) },
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = BearishRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Used to calculate share quantity and position sizing per trade",
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { validateAndSave() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BgCardBorder,
                            errorBorderColor = BearishRed,
                            focusedContainerColor = BgCardElevated,
                            unfocusedContainerColor = BgCardElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("risk_input_field")
                    )
                }

                // Explanatory Info Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgDarkNavy)
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Position Sizing Formula:\nQuantity = Risk Amount ÷ (Entry Price - Stop Loss)",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Action Buttons: Cancel and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("cancel_risk_button")
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { validateAndSave() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_risk_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Risk",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
