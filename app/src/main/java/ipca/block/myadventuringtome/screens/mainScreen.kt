package ipca.block.myadventuringtome.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.block.myadventuringtome.nav.NavRoutes
import ipca.block.myadventuringtome.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isAddingFriend by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var addFriendError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var friendName by remember { mutableStateOf("") }
    var friendEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bem-vindo à tua Aventura!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate(NavRoutes.Characters.route)
        }) {
            Text("As minhas Personagens")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            navController.navigate(NavRoutes.Campaigns.route)
        }) {
            Text("As minhas Campanhas")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            showAddFriendDialog = true
        }) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Amigo")
        }

        Spacer(modifier = Modifier.height(64.dp))

        OutlinedButton(
            onClick = { showDeleteConfirmation = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            enabled = !isDeleting
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminando...")
            } else {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar Conta")
            }
        }

        deleteError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // Add Friend Dialog
    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAddingFriend) {
                    showAddFriendDialog = false
                    friendName = ""
                    friendEmail = ""
                    addFriendError = null
                }
            },
            title = { Text("Adicionar Amigo") },
            text = {
                Column {
                    Text("Adicione um amigo inserindo o nome e email:")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = friendName,
                        onValueChange = {
                            friendName = it
                            addFriendError = null
                        },
                        label = { Text("Nome") },
                        singleLine = true,
                        enabled = !isAddingFriend,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = {
                            friendEmail = it
                            addFriendError = null
                        },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !isAddingFriend,
                        modifier = Modifier.fillMaxWidth()
                    )

                    addFriendError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (friendName.isNotEmpty() && friendEmail.isNotEmpty()) {
                            coroutineScope.launch {
                                addFriendError = null
                                isAddingFriend = true

                                try {
                                    val (success, message) = authViewModel.addFriend(friendName, friendEmail)

                                    if (success) {
                                        println("Friend added successfully")
                                        showAddFriendDialog = false
                                        friendName = ""
                                        friendEmail = ""
                                    } else {
                                        println("Failed to add friend: $message")
                                        addFriendError = message ?: "Erro ao adicionar amigo. Tente novamente."
                                    }
                                } catch (e: Exception) {
                                    println("Unexpected error adding friend: ${e.message}")
                                    e.printStackTrace()
                                    addFriendError = "Erro inesperado: ${e.message}"
                                } finally {
                                    isAddingFriend = false
                                }
                            }
                        } else {
                            addFriendError = "Por favor, preencha todos os campos."
                        }
                    },
                    enabled = !isAddingFriend && friendName.isNotEmpty() && friendEmail.isNotEmpty()
                ) {
                    if (isAddingFriend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Adicionar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isAddingFriend) {
                            showAddFriendDialog = false
                            friendName = ""
                            friendEmail = ""
                            addFriendError = null
                        }
                    },
                    enabled = !isAddingFriend
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showDeleteConfirmation = false
                    deleteError = null
                }
            },
            title = { Text("Eliminar Conta") },
            text = {
                Text("Tem certeza que deseja eliminar permanentemente a sua conta? Esta ação irá remover:\n\n• Todos os seus personagens\n• Todas as suas campanhas\n• Todas as suas notas\n• Todos os dados associados\n\nEsta ação não pode ser desfeita.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        showPasswordDialog = true
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            showDeleteConfirmation = false
                            deleteError = null
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showPasswordDialog = false
                    password = ""
                    deleteError = null
                }
            },
            title = { Text("Confirme a sua Password") },
            text = {
                Column {
                    Text("Para confirmar a eliminação da conta, insira a sua password atual:")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            deleteError = null
                        },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar password" else "Mostrar password"
                                )
                            }
                        },
                        singleLine = true,
                        enabled = !isDeleting
                    )

                    deleteError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (password.isNotEmpty()) {
                            coroutineScope.launch {
                                deleteError = null
                                isDeleting = true

                                try {
                                    val (reauthSuccess, reauthMessage) = authViewModel.reauthenticateUser(password)

                                    if (reauthSuccess) {
                                        println("Re-authentication successful, proceeding with deletion...")
                                        val (deleteSuccess, deleteMessage) = authViewModel.deleteUserAccount()

                                        if (deleteSuccess) {
                                            println("Account deletion successful, navigating to login...")
                                            navController.navigate(NavRoutes.Login.route) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            println("Account deletion failed: $deleteMessage")
                                            deleteError = deleteMessage ?: "Erro ao eliminar conta. Tente novamente."
                                        }
                                    } else {
                                        println("Re-authentication failed: $reauthMessage")
                                        deleteError = reauthMessage ?: "Password incorreta. Tente novamente."
                                    }
                                } catch (e: Exception) {
                                    println("Unexpected error during deletion: ${e.message}")
                                    e.printStackTrace()
                                    deleteError = "Erro inesperado: ${e.message}"
                                } finally {
                                    isDeleting = false
                                    if (deleteError != null) {
                                        println("Keeping dialog open due to error: $deleteError")
                                    } else {
                                        showPasswordDialog = false
                                        password = ""
                                    }
                                }
                            }
                        } else {
                            deleteError = "Por favor, insira a sua password."
                        }
                    },
                    enabled = !isDeleting && password.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Eliminar Permanentemente")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            showPasswordDialog = false
                            password = ""
                            deleteError = null
                        }
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}