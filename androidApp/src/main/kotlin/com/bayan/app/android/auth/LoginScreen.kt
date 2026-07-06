package com.bayan.app.android.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class AuthMode { LOGIN, SIGNUP }

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val passwordsMatch = mode == AuthMode.LOGIN || password == confirmPassword
    val canSubmit = email.isNotBlank() && password.length >= 6 && passwordsMatch && !isSubmitting

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "بيان",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "حساب واحد لكل أجهزتك — يشتغل بلا نت ويتزامن لما يتوفر الاتصال",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == AuthMode.LOGIN,
                    onClick = { mode = AuthMode.LOGIN; viewModel.clearError() },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("تسجيل الدخول") }
                )
                SegmentedButton(
                    selected = mode == AuthMode.SIGNUP,
                    onClick = { mode = AuthMode.SIGNUP; viewModel.clearError() },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("حساب جديد") }
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور (٦ أحرف على الأقل)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.SIGNUP) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("تأكيد كلمة المرور") },
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Text(
                        text = "كلمتا المرور غير متطابقتين",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (mode == AuthMode.LOGIN) viewModel.signIn(email, password)
                    else viewModel.signUp(email, password)
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (mode == AuthMode.LOGIN) "دخول" else "إنشاء الحساب")
                }
            }

            if (mode == AuthMode.SIGNUP) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "بعد إنشاء الحساب سجّل بنفس البيانات على أي جهاز آخر (كمبيوتر أو آيفون أو أندرويد) وبتلاقي نفس بياناتك.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
