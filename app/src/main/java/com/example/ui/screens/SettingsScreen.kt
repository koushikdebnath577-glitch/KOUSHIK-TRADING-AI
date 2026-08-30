package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.BackendConfig
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    defaultRisk: Double,
    defaultTargetRR: Double,
    onUpdateRisk: (Double, Double) -> Unit,
    onUpdateBackendConfig: (BackendConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var backendUrl by remember { mutableStateOf("https://koushik-trading-ai.onrender.com/api") }
    var wsUrl by remember { mutableStateOf("wss://koushik-trading-ai.onrender.com/ws/market") }
    var riskAmountText by remember(defaultRisk) {
        val initial = if (defaultRisk % 1.0 == 0.0) defaultRisk.toInt().toString() else String.format(java.util.Locale.US, "%.2f", defaultRisk)
        mutableStateOf(initial)
    }
    var riskErrorText by remember { mutableStateOf<String?>(null) }
    var riskSaveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var selectedRR by remember(defaultTargetRR) { mutableStateOf(defaultTargetRR) }
    var isSimulatedMode by remember { mutableStateOf(true) }

    val presetAmounts = listOf(500.0, 1000.0, 1500.0, 2500.0, 5000.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SETTINGS & SYSTEM ARCHITECTURE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Angel One SmartAPI Secure Proxy & Risk Control Configuration",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        // 1. Backend Security Architecture Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backend_architecture_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Text(text = "SECURE BACKEND ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 0.8.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To guarantee institutional security, Angel One API credentials, Client ID, PIN, TOTP secret, and JWT tokens are NEVER stored in this Android application. All SmartAPI authentication and feed multiplexing are handled by your private backend server.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Backend Endpoints
                    OutlinedTextField(
                        value = backendUrl,
                        onValueChange = {
                            backendUrl = it
                            onUpdateBackendConfig(BackendConfig(it, wsUrl, isSimulatedMode))
                        },
                        label = { Text("Secure Backend API Base URL", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BgCardBorder,
                            focusedContainerColor = BgCardElevated,
                            unfocusedContainerColor = BgCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = wsUrl,
                        onValueChange = {
                            wsUrl = it
                            onUpdateBackendConfig(BackendConfig(backendUrl, it, isSimulatedMode))
                        },
                        label = { Text("WebSocket Streaming Feed URL", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BgCardBorder,
                            focusedContainerColor = BgCardElevated,
                            unfocusedContainerColor = BgCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Server Environment Variables Checklist Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "REQUIRED BACKEND ENVIRONMENT SECRETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configure these on your server backend (.env) before streaming live trades:",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val envKeys = listOf(
                        "ANGEL_API_KEY" to "SmartAPI Application API Key",
                        "ANGEL_CLIENT_ID" to "Angel One Trading Account Client ID",
                        "ANGEL_PIN" to "4-digit Trading Security PIN",
                        "ANGEL_TOTP_SECRET" to "Time-based OTP Base32 Secret",
                        "ANGEL_PUBLIC_IP" to "Public IP for IP Whitelisting",
                        "ANGEL_LOCAL_IP" to "Internal Local Network IP",
                        "ANGEL_MAC_ADDRESS" to "Server Hardware MAC Identifier"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgCardElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        envKeys.forEach { (key, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = desc,
                                    fontSize = 9.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Risk Management Rules
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth().testTag("intraday_risk_controls_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTRADAY RISK CONTROLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KeyLevelYellowBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, KeyLevelYellow.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Active: ₹${if (defaultRisk % 1.0 == 0.0) defaultRisk.toInt() else String.format(java.util.Locale.US, "%.2f", defaultRisk)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KeyLevelYellow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Current Active Risk Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BgCardElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Current Capital Risk per Trade", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    text = "₹${if (defaultRisk % 1.0 == 0.0) defaultRisk.toInt() else String.format(java.util.Locale.US, "%.2f", defaultRisk)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Saved Permanently",
                                fontSize = 9.sp,
                                color = BullishGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Quick Preset Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Select Preset Risk Amount:", fontSize = 10.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetAmounts.forEach { preset ->
                                val isSel = riskAmountText.toDoubleOrNull() == preset
                                Surface(
                                    onClick = {
                                        riskAmountText = preset.toInt().toString()
                                        riskErrorText = null
                                        riskSaveSuccessMessage = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) CyanAccent else BgPillInactive,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSel) CyanAccent else BgCardBorder
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .testTag("settings_preset_${preset.toInt()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "₹${preset.toInt()}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom Input Field with Validation
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = riskAmountText,
                            onValueChange = {
                                riskAmountText = it
                                riskErrorText = null
                                riskSaveSuccessMessage = null
                            },
                            label = { Text("Capital Risk per Trade (₹)") },
                            leadingIcon = {
                                Text(
                                    text = "₹",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                                )
                            },
                            isError = riskErrorText != null,
                            supportingText = {
                                if (riskErrorText != null) {
                                    Text(
                                        text = riskErrorText!!,
                                        color = BearishRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (riskSaveSuccessMessage != null) {
                                    Text(
                                        text = riskSaveSuccessMessage!!,
                                        color = BullishGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BgCardBorder,
                                errorBorderColor = BearishRed,
                                focusedContainerColor = BgCardElevated,
                                unfocusedContainerColor = BgCardElevated
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_risk_input_field")
                        )

                        // Save and Cancel Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val fallback = if (defaultRisk % 1.0 == 0.0) defaultRisk.toInt().toString() else String.format(java.util.Locale.US, "%.2f", defaultRisk)
                                    riskAmountText = fallback
                                    riskErrorText = null
                                    riskSaveSuccessMessage = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("settings_cancel_risk_button")
                            ) {
                                Text(text = "Cancel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val trimmed = riskAmountText.trim()
                                    if (trimmed.isEmpty()) {
                                        riskErrorText = "Risk amount cannot be empty"
                                        return@Button
                                    }
                                    val r = trimmed.toDoubleOrNull()
                                    if (r == null) {
                                        riskErrorText = "Please enter a valid numeric amount"
                                        return@Button
                                    }
                                    if (r <= 0.0) {
                                        riskErrorText = "Risk amount must be greater than ₹0"
                                        return@Button
                                    }
                                    riskErrorText = null
                                    onUpdateRisk(r, selectedRR)
                                    riskSaveSuccessMessage = "Saved ₹${if (r % 1.0 == 0.0) r.toInt() else String.format(java.util.Locale.US, "%.2f", r)} successfully!"
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("settings_save_risk_button")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Save Risk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Minimum Target Risk:Reward Filter:", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.5, 2.0, 2.5).forEach { rr ->
                            val isSel = selectedRR == rr
                            Surface(
                                onClick = {
                                    selectedRR = rr
                                    val currentR = riskAmountText.toDoubleOrNull() ?: defaultRisk
                                    onUpdateRisk(currentR, rr)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) CyanAccent else BgPillInactive,
                                border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "1 : $rr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Regulatory & Compliance Notice
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = null, tint = KeyLevelYellow, modifier = Modifier.size(16.dp))
                        Text(text = "REGULATORY & EXECUTION DISCLAIMER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KeyLevelYellow, letterSpacing = 0.8.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. KOUSHIK TRADING AI is an algorithmic market analysis and educational tool.\n2. The application will NEVER automatically place a trade or manage brokerage balances.\n3. All Buy/Sell signals require independent manual verification and personal risk judgment.\n4. Trading in stock markets and derivatives involves substantial risk of loss.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
