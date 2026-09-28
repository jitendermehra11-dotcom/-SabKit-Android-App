package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.viewmodel.SabKitUiState
import com.example.viewmodel.SabKitViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanProfileScreen(
    state: SabKitUiState,
    viewModel: SabKitViewModel,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(state.loanFullName) }
    var mobile by remember { mutableStateOf(state.loanMobile) }
    var udyamNo by remember { mutableStateOf(state.loanUdyamNo) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loanOptions = listOf(
        "PM SVANidhi Loan (स्ट्रीट वेंडर लोन)",
        "Mudra Loan (शिशु, किशोर, तरुण)",
        "MSME Business Loan (बिजनेस लोन)",
        "PMEGP योजना (सब्सिडी लोन)",
        "स्टैंड-अप इंडिया (Stand-Up India)"
    )
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedLoanType by remember { mutableStateOf(state.selectedLoanType) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("loan_profile_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Title
            Text(
                text = "लोन प्रोफाइल सेटअप",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag("tv_loan_screen_title")
            )

            Text(
                text = "सरकारी सब्सिडी और लोन योजनाओं के लिए अपनी प्रोफाइल तुरंत सेटअप करें।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Success Confirmation Card
            if (state.loanSubmittedSuccess && state.loanSuccessMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("card_loan_success")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "आवेदन दर्ज किया गया!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = state.loanSuccessMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }

            // Input: Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                label = { Text("पूरा नाम (PAN कार्ड के अनुसार)") },
                placeholder = { Text("उदा. राहुल कुमार") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                isError = errorMessage != null && fullName.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("et_full_name")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input: Mobile Number
            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        mobile = it
                        errorMessage = null
                    }
                },
                label = { Text("मोबाइल नंबर (10 अंक)") },
                placeholder = { Text("9876543210") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                isError = errorMessage != null && mobile.length != 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("et_mobile")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input: Udyam Number (Optional)
            OutlinedTextField(
                value = udyamNo,
                onValueChange = { udyamNo = it.uppercase() },
                label = { Text("उद्यम नंबर (Optional)") },
                placeholder = { Text("UDYAM-XX-00-0000000") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("et_udyam_no")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Spinner: Loan Type Dropdown
            Text(
                text = "लोन का प्रकार चुनें",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedLoanType,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("sp_loan_type")
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    loanOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedLoanType = option
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Error Message Display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("tv_loan_error")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (fullName.trim().isEmpty()) {
                        errorMessage = "कृपया पूरा नाम दर्ज करें"
                        return@Button
                    }
                    if (mobile.trim().length != 10) {
                        errorMessage = "कृपया 10 अंकों का वैध मोबाइल नंबर दर्ज करें"
                        return@Button
                    }

                    errorMessage = null
                    viewModel.submitLoanApplication(
                        fullName = fullName.trim(),
                        mobile = mobile.trim(),
                        udyam = udyamNo.trim(),
                        loanType = selectedLoanType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_submit_loan"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6F00),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "आवेदन जमा करें",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // AdMob Banner Ad View at Bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.testTag("admob_banner_view"),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        // Official Test Ad Unit ID
                        adUnitId = "ca-app-pub-3940256099942544/6300978111"
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * Reusable Popup Dialog equivalent to dialog_loan_popup.xml
 */
@Composable
fun GovernmentLoanPopupDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onApplyNow: () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_loan_popup_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "सरकारी लोन एवं बिजनेस सहायता",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tvLoanTitle")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "क्या आपको पीएम स्वनिधि, मुद्रा या MSME लोन की आवश्यकता है? तुरंत अपनी प्रोफाइल सेटअप करें।",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tvLoanDesc")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btnCancel"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF555555))
                        ) {
                            Text(text = "बाद में", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = onApplyNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btnApplyNow"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6F00),
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "अभी सेटअप करें", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
