package ipca.block.myadventuringtome.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import ipca.block.myadventuringtome.models.Note
import ipca.block.myadventuringtome.models.NoteTag
import ipca.block.myadventuringtome.viewmodels.CampaignViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Person


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailsScreen(
    navController: NavController,
    campaignId: String,
    campaignViewModel: CampaignViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var sharingNote by remember { mutableStateOf<Note?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Load campaign details when screen opens
    LaunchedEffect(campaignId) {
        campaignViewModel.loadCampaign(campaignId)
    }

    val currentCampaign by campaignViewModel.currentCampaign
    val isLoading by campaignViewModel.isLoading

    // Helper function to create shared campaign note
    suspend fun createSharedCharacterNote(
        note: Note,
        targetCharacterId: String,
        targetCharacterName: String,
        sourceCampaignName: String
    ): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser!!

            // Always create a new note with a unique ID
            val noteId = UUID.randomUUID().toString()

            // Create a unique title if a note with the same title already exists
            val existingNotesSnapshot = db.collection("characters")
                .document(targetCharacterId)
                .collection("notes")
                .whereEqualTo("title", note.title)
                .get()
                .await()

            val finalTitle = if (existingNotesSnapshot.documents.isNotEmpty()) {
                "${note.title} (Compartilhada de $sourceCampaignName)"
            } else {
                note.title
            }

            println("Creating new shared note for character $targetCharacterName with title: $finalTitle")

            // Prepare the shared note data
            val sharedNoteData = mapOf(
                "title" to finalTitle,
                "content" to note.content,
                "tag" to note.tag.name,
                "sharedFrom" to currentUser.uid,
                "sharedFromEmail" to (currentUser.email ?: ""),
                "sharedFromCampaignName" to sourceCampaignName,
                "sharedAt" to System.currentTimeMillis(),
                "originalCampaignId" to campaignId,
                "isShared" to true
            )

            // Always create a new note document
            db.collection("characters")
                .document(targetCharacterId)
                .collection("notes")
                .document(noteId)
                .set(sharedNoteData)
                .await()

            errorMessage = "Nota compartilhada com sucesso para o personagem '$targetCharacterName'"
            println("Character note sharing completed successfully")
            true

        } catch (e: Exception) {
            println("Error creating shared character note: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Function to share note with user
    suspend fun shareNoteWithUser(
        note: Note,
        targetUserEmail: String,
        sourceCampaignName: String
    ): Boolean {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                errorMessage = "Usuário não autenticado"
                return false
            }

            println("=== CAMPAIGN NOTE SHARING DEBUG INFO ===")
            println("Source campaign name (to match with character's campaignId): $sourceCampaignName")
            println("Target email: $targetUserEmail")

            // Step 1: Find the target user by email
            val normalizedEmail = targetUserEmail.trim().lowercase()
            val usersSnapshot = db.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .get()
                .await()

            println("Users found with email: ${usersSnapshot.documents.size}")

            if (usersSnapshot.documents.isEmpty()) {
                errorMessage = "Usuário com email '$targetUserEmail' não encontrado no sistema"
                return false
            }

            val targetUserId = usersSnapshot.documents.first().id
            println("Target user ID: $targetUserId")

            // Step 2: Find characters belonging to this user that have campaignId = campaign.name
            val charactersSnapshot = db.collection("characters")
                .whereEqualTo("userId", targetUserId)
                .whereEqualTo("campaignId", sourceCampaignName)  // Campaign's name matches character's campaignId
                .get()
                .await()

            println("Characters found for user with campaignId matching '$sourceCampaignName': ${charactersSnapshot.documents.size}")

            if (charactersSnapshot.documents.isEmpty()) {
                errorMessage = "O usuário '$targetUserEmail' não possui personagens na campanha '$sourceCampaignName'"
                return false
            }

            // Step 3: Use the first character from the same campaign
            val targetCharacter = charactersSnapshot.documents.first()
            val targetCharacterId = targetCharacter.id
            val targetCharacterName = targetCharacter.getString("name") ?: "Personagem"

            println("Sharing to character in same campaign: $targetCharacterName (ID: $targetCharacterId)")

            // Step 4: Share the note to the CHARACTER's notes collection
            val success = createSharedCharacterNote(note, targetCharacterId, targetCharacterName, sourceCampaignName)

            return success

        } catch (e: Exception) {
            println("Error sharing campaign note: ${e.message}")
            e.printStackTrace()

            errorMessage = when {
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "Sem permissão para compartilhar nota. Verifique as regras do Firestore."
                e.message?.contains("UNAUTHENTICATED") == true ->
                    "Usuário não autenticado. Faça login novamente."
                e.message?.contains("NOT_FOUND") == true ->
                    "Usuário ou personagem não encontrado."
                e.message?.contains("NETWORK") == true ->
                    "Erro de conexão. Verifique sua internet."
                else -> "Erro ao compartilhar nota: ${e.message}"
            }
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(currentCampaign?.name ?: "Campanha")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Nota")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                currentCampaign?.let { campaign ->
                    Text(
                        text = "Campanha: ${campaign.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (campaign.notes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma nota adicionada ainda.\nToque no botão + para adicionar uma nota.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(campaign.notes) { note ->
                                NoteCard(
                                    note = note,
                                    onEdit = {
                                        editingNote = note
                                        showEditNoteDialog = true
                                    },
                                    onDelete = {
                                        campaignViewModel.deleteNoteFromCampaign(
                                            campaignId = campaignId,
                                            noteId = note.id,
                                            onSuccess = { },
                                            onError = { error ->
                                                errorMessage = error
                                            }
                                        )
                                    },
                                    onShare = {
                                        sharingNote = note
                                        showShareDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = if (errorMessage.contains("sucesso"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        NoteDialog(
            title = "Adicionar Nota",
            onDismiss = {
                showAddNoteDialog = false
                errorMessage = ""
            },
            onConfirm = { note ->
                val noteWithId = note.copy(id = UUID.randomUUID().toString())
                campaignViewModel.addNoteToCampaign(
                    campaignId = campaignId,
                    note = noteWithId,
                    onSuccess = {
                        showAddNoteDialog = false
                        errorMessage = ""
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            }
        )
    }

    // Edit Note Dialog
    if (showEditNoteDialog && editingNote != null) {
        NoteDialog(
            title = "Editar Nota",
            initialNote = editingNote,
            onDismiss = {
                showEditNoteDialog = false
                editingNote = null
                errorMessage = ""
            },
            onConfirm = { updatedNote ->
                campaignViewModel.updateNoteInCampaign(
                    campaignId = campaignId,
                    noteId = editingNote!!.id,
                    updatedNote = updatedNote,
                    onSuccess = {
                        showEditNoteDialog = false
                        editingNote = null
                        errorMessage = ""
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            }
        )
    }

    // Share Dialog
    if (showShareDialog && sharingNote != null) {
        ShareCampaignNoteDialog(
            note = sharingNote!!,
            onDismiss = {
                showShareDialog = false
                sharingNote = null
                errorMessage = ""
            },
            onShare = { targetUserEmail ->
                coroutineScope.launch {
                    val success = shareNoteWithUser(
                        sharingNote!!,
                        targetUserEmail,
                        currentCampaign?.name ?: "Campanha Desconhecida"
                    )
                    if (success) {
                        showShareDialog = false
                        sharingNote = null
                    }
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            // Paper texture background (subtle pattern)
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val paint = Paint().apply {
                    color = Color(0xFF000000).copy(alpha = 0.02f)
                    strokeWidth = 1.dp.toPx()
                }
                // Draw subtle horizontal lines to simulate ruled paper
                for (i in 0 until (size.height / 25.dp.toPx()).toInt()) {
                    val y = i * 25.dp.toPx()
                    drawLine(
                        color = Color(0xFFD7CCC8).copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp) // Increased padding for more paper-like feel
            ) {
                // Header with title and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Action buttons with paper-like styling
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PaperIconButton(
                            onClick = onShare,
                            icon = Icons.Default.Share,
                            contentDescription = "Compartilhar"
                        )
                        PaperIconButton(
                            onClick = onEdit,
                            icon = Icons.Default.Edit,
                            contentDescription = "Editar"
                        )
                        PaperIconButton(
                            onClick = onDelete,
                            icon = Icons.Default.Delete,
                            contentDescription = "Deletar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tag with paper-like styling
                PaperTagChip(
                    tag = note.tag,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content with handwritten-like styling
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp) // Indent like handwritten notes
                )
            }

            // Corner fold effect (top-right)
            Canvas(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
            ) {
                val path = Path().apply {
                    moveTo(size.width - 15.dp.toPx(), 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, 15.dp.toPx())
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0xFFD7CCC8).copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun PaperIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PaperTagChip(
    tag: NoteTag,
    modifier: Modifier = Modifier
) {
    val tagColor = getPaperTagColor(tag)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = tagColor.copy(alpha = 0.15f),
        border = BorderStroke(
            width = 1.dp,
            color = tagColor.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = tagColor.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun getPaperTagColor(tag: NoteTag): Color {
    return when (tag) {
        NoteTag.ALIADO -> Color(0xFF2E7D32) // Dark green, like ink
        NoteTag.INIMIGO -> Color(0xFFC62828) // Dark red, like dried blood
        NoteTag.DESCONFIADO -> Color(0xFFEF6C00) // Dark orange, like rust
        NoteTag.FAMILIAR -> Color(0xFF1565C0) // Dark blue, like fountain pen ink
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCampaignNoteDialog(
    note: Note,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit
) {
    var targetUserEmail by remember { mutableStateOf("") }
    var showFriendsList by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var friendsError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // Function to load friends from Firebase
    suspend fun loadFriends() {
        try {
            isLoadingFriends = true
            friendsError = null

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                friendsError = "Usuário não autenticado"
                return
            }

            val userDoc = db.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            if (userDoc.exists()) {
                val friendsData = userDoc.get("friends") as? List<Map<String, Any>> ?: emptyList()

                friends = friendsData.mapNotNull { friendMap ->
                    val email = friendMap["email"] as? String
                    val name = friendMap["name"] as? String

                    if (email != null && name != null) {
                        Friend(email = email, name = name)
                    } else null
                }
            }
        } catch (e: Exception) {
            friendsError = "Erro ao carregar amigos: ${e.message}"
            println("Error loading friends: ${e.message}")
        } finally {
            isLoadingFriends = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartilhar Nota da Campanha") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Compartilhando: \"${note.title}\"",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Email input with friends button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetUserEmail,
                        onValueChange = { targetUserEmail = it },
                        label = { Text("Email do usuário") },
                        placeholder = { Text("exemplo@email.com") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                loadFriends()
                                showFriendsList = true
                            }
                        },
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Amigos",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Amigos")
                    }
                }

                Text(
                    text = "A nota será adicionada ao primeiro personagem do usuário encontrado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Nota: Imagens não são compartilhadas entre usuários.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (targetUserEmail.isNotBlank()) {
                        onShare(targetUserEmail.trim())
                    }
                },
                enabled = targetUserEmail.isNotBlank()
            ) {
                Text("Compartilhar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Friends List Dialog
    if (showFriendsList) {
        AlertDialog(
            onDismissRequest = { showFriendsList = false },
            title = { Text("Selecionar Amigo") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    when {
                        isLoadingFriends -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        friendsError != null -> {
                            Text(
                                text = friendsError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        friends.isEmpty() -> {
                            Text(
                                text = "Nenhum amigo encontrado",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(friends) { friend ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                targetUserEmail = friend.email
                                                showFriendsList = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = friend.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = friend.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFriendsList = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
@Composable
fun NoteDialog(
    title: String,
    initialNote: Note? = null,
    onDismiss: () -> Unit,
    onConfirm: (Note) -> Unit
) {
    var noteTitle by remember { mutableStateOf(initialNote?.title ?: "") }
    var noteContent by remember { mutableStateOf(initialNote?.content ?: "") }
    var selectedTag by remember { mutableStateOf(initialNote?.tag ?: NoteTag.ALIADO) }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = {
                        noteTitle = it
                        errorMessage = ""
                    },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tag dropdown
                var expanded by remember { mutableStateOf(false) }

                Box {
                    OutlinedTextField(
                        value = selectedTag.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Dropdown"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        NoteTag.values().forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    selectedTag = tag
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteContent,
                    onValueChange = {
                        noteContent = it
                        errorMessage = ""
                    },
                    label = { Text("Conteúdo") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (noteTitle.isBlank()) {
                        errorMessage = "Por favor, insira um título"
                        return@TextButton
                    }
                    if (noteContent.isBlank()) {
                        errorMessage = "Por favor, insira um conteúdo"
                        return@TextButton
                    }

                    val note = Note(
                        id = initialNote?.id ?: "",
                        title = noteTitle.trim(),
                        content = noteContent.trim(),
                        tag = selectedTag
                    )
                    onConfirm(note)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun getTagColor(tag: NoteTag): Color {
    return when (tag) {
        NoteTag.ALIADO -> Color(0xFF4CAF50)
        NoteTag.INIMIGO -> Color(0xFFF44336)
        NoteTag.DESCONFIADO -> Color(0xFFFF9800)
        NoteTag.FAMILIAR -> Color(0xFF2196F3)
    }
}