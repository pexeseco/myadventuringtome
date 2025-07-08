package ipca.block.myadventuringtome.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.block.myadventuringtome.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Email validation
    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Password validation (at least 6 characters)
    val isPasswordValid = remember(password) {
        password.length >= 6
    }

    // Form validation
    val isFormValid = isEmailValid && isPasswordValid && email.isNotBlank() && password.isNotBlank()

    // Function to create user document in Firestore after successful signup
    suspend fun createUserDocument(user: com.google.firebase.auth.FirebaseUser) {
        try {
            val userData = mapOf(
                "email" to user.email?.lowercase(),
                "uid" to user.uid,
                "createdAt" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .set(userData)
                .await()
        } catch (e: Exception) {
            // Log error but don't block user flow
            println("Error creating user document: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Adventuring Tome",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Error message display
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it.trim()
                errorMessage = null // Clear error when user starts typing
            },
            label = { Text("Email") },
            placeholder = { Text("exemplo@email.com") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            isError = email.isNotBlank() && !isEmailValid,
            supportingText = {
                if (email.isNotBlank() && !isEmailValid) {
                    Text("Email inválido", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null // Clear error when user starts typing
            },
            label = { Text("Password") },
            placeholder = { Text("Mínimo 6 caracteres") },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (isFormValid && !isLoading) {
                        // Trigger login
                    }
                }
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Ocultar senha" else "Mostrar senha"
                    )
                }
            },
            isError = password.isNotBlank() && !isPasswordValid,
            supportingText = {
                if (password.isNotBlank() && !isPasswordValid) {
                    Text("Senha deve ter pelo menos 6 caracteres", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                authViewModel.signInWithEmailPassword(email, password) { success ->
                    isLoading = false
                    if (success) {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        errorMessage = "Erro no login. Verifique suas credenciais."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                isLoading = true
                errorMessage = null
                authViewModel.signUpWithEmailPassword(email, password) { success ->
                    isLoading = false
                    if (success) {
                        // Create user document in Firestore
                        coroutineScope.launch {
                            FirebaseAuth.getInstance().currentUser?.let { user ->
                                createUserDocument(user)
                            }
                        }
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        errorMessage = "Erro ao criar conta. Tente novamente."
                    }
                }
            },
            enabled = isFormValid && !isLoading
        ) {
            Text("Criar conta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional: Add forgot password functionality
        TextButton(
            onClick = {
                if (email.isNotBlank() && isEmailValid) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnSuccessListener {
                            errorMessage = "Email de recuperação enviado!"
                        }
                        .addOnFailureListener {
                            errorMessage = "Erro ao enviar email de recuperação"
                        }
                } else {
                    errorMessage = "Digite um email válido para recuperar a senha"
                }
            },
            enabled = !isLoading
        ) {
            Text("Esqueci minha senha", style = MaterialTheme.typography.bodySmall)
        }
    }
}