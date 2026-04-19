package com.example.edusync.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.edusync.data.User
import com.example.edusync.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivateAccountScreen(
    onActivationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TeacherViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(1) } // 1: Kod Girme, 2: Bilgi Belirleme
    var codeInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var activatedTeacherName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hesap Aktivasyonu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (step == 1) Icons.Default.VpnKey else Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PrimaryBlue
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (step == 1) {
                Text("Aktivasyon Kodunu Girin", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Admin tarafından size iletilen 6 haneli kodu giriniz.", 
                    color = Color.Gray, 
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 6) codeInput = it },
                    label = { Text("6 Haneli Aktivasyon Kodu") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ErrorRed, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            val vCode = viewModel.checkVerificationCode(codeInput)
                            if (vCode != null && !vCode.isUsed) {
                                val teacher = viewModel.getTeacherById(vCode.teacherId ?: -1)
                                activatedTeacherName = "${teacher?.title} ${teacher?.name} ${teacher?.surname}"
                                step = 2
                            } else {
                                errorMessage = "Geçersiz veya daha önce kullanılmış kod!"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = codeInput.length == 6 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) 
                    else Text("KODU DOĞRULA", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Hoşgeldiniz,", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text(activatedTeacherName, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PrimaryBlue)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Lütfen sisteme giriş için kullanacağınız bilgileri belirleyin.", 
                    color = Color.Gray, 
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = PrimaryBlue) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Yeni Şifre") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = PrimaryBlue) }
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = ErrorRed, modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val success = viewModel.activateAccount(codeInput, usernameInput, passwordInput)
                            if (success) {
                                onActivationSuccess()
                            } else {
                                errorMessage = "Hesap oluşturulurken bir hata oluştu!"
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = usernameInput.isNotBlank() && passwordInput.length >= 6 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("HESABI TAMAMLA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
