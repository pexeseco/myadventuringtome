    package ipca.block.myadventuringtome.screens

    import android.net.Uri
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material.icons.filled.Delete
    import androidx.compose.material.icons.filled.Edit
    import androidx.compose.material.icons.filled.Image
    import androidx.compose.material.icons.filled.Share
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.layout.ContentScale
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.unit.dp
    import androidx.navigation.NavController
    import coil.compose.rememberAsyncImagePainter
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import ipca.block.myadventuringtome.models.CharacterNote
    import ipca.block.myadventuringtome.models.Note
    import ipca.block.myadventuringtome.models.NoteTag
    import ipca.block.myadventuringtome.utils.LocalImageUtils
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.tasks.await
    import java.util.*
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.heightIn
    import androidx.compose.foundation.layout.wrapContentHeight
    import androidx.compose.ui.window.Dialog
    import androidx.compose.foundation.gestures.rememberTransformableState
    import androidx.compose.foundation.gestures.transformable
    import androidx.compose.ui.graphics.graphicsLayer
    import androidx.compose.foundation.background
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.ui.graphics.Color
    import androidx.compose.foundation.Canvas
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.border
    import androidx.compose.ui.graphics.Paint
    import androidx.compose.ui.graphics.Path
    import androidx.compose.ui.graphics.drawscope.DrawScope
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.material.icons.filled.Person


    @Composable
    private fun getTagColor(tag: NoteTag): Color {
        return when (tag) {
            NoteTag.ALIADO -> Color(0xFF4CAF50)
            NoteTag.INIMIGO -> Color(0xFFF44336)
            NoteTag.DESCONFIADO -> Color(0xFFFF9800)
            NoteTag.FAMILIAR -> Color(0xFF2196F3)
        }
    }

    @Composable
    fun CharacterNotePaperTagColor(tag: NoteTag): Color {
        return when (tag) {
            NoteTag.ALIADO -> Color(0xFF2E7D32)
            NoteTag.INIMIGO -> Color(0xFFC62828)
            NoteTag.DESCONFIADO -> Color(0xFFE65100)
            NoteTag.FAMILIAR -> Color(0xFF1565C0)
        }
    }

    @Composable
    fun CharacterNotesScreen(characterId: String, navController: NavController) {
        val db = FirebaseFirestore.getInstance()
        val coroutineScope = rememberCoroutineScope()

        // State to manage the list of notes
        var notes by remember { mutableStateOf<List<CharacterNote>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var refreshTrigger by remember { mutableStateOf(0) }

        // State for dialogs
        var showEditDialog by remember { mutableStateOf(false) }
        var showAddDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showShareDialog by remember { mutableStateOf(false) }
        var selectedNote by remember { mutableStateOf<CharacterNote?>(null) }

        var characterName by remember { mutableStateOf("") }
        var campaignName by remember { mutableStateOf("") }
        var campaignId by remember { mutableStateOf("") }

        var showImageDialog by remember { mutableStateOf(false) }
        var selectedImagePath by remember { mutableStateOf<String?>(null) }

        suspend fun loadCharacterInfo() {
            try {
                val characterDoc = db.collection("characters")
                    .document(characterId)
                    .get()
                    .await()

                if (characterDoc.exists()) {
                    characterName = characterDoc.getString("name") ?: ""
                    campaignName = characterDoc.getString("campaignName") ?: ""
                    campaignId = characterDoc.getString("campaignId") ?: ""

                    println("=== CHARACTER INFO ===")
                    println("Character name: '$characterName'")
                    println("Campaign name: '$campaignName'")
                    println("Campaign ID: '$campaignId'")
                    println("Character document data: ${characterDoc.data}")
                } else {
                    println("Character document does not exist: $characterId")
                }
            } catch (e: Exception) {
                println("Error loading character info: ${e.message}")
                e.printStackTrace()
            }
        }
        // Function to load notes from Firebase

        suspend fun loadNotes() {
            try {
                isLoading = true
                error = null

                // Check if user is authenticated
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    error = "Usuário não autenticado. Faça login novamente."
                    isLoading = false
                    return
                }

                println("Loading notes for character: $characterId")
                println("Current user: ${currentUser.uid}")

                val notesSnapshot = db.collection("characters")
                    .document(characterId)
                    .collection("notes")
                    .get()
                    .await()

                println("Notes snapshot size: ${notesSnapshot.size()}")

                notes = notesSnapshot.documents.mapNotNull { document ->
                    try {
                        val noteData = document.data
                        println("Processing document: ${document.id}, data: $noteData")

                        if (noteData != null) {
                            val note = Note(
                                id = document.id,
                                title = noteData["title"] as? String ?: "",
                                content = noteData["content"] as? String ?: "",
                                tag = NoteTag.valueOf(noteData["tag"] as? String ?: "ALIADO"),
                                imagePath = noteData["imagePath"] as? String
                            )
                            val tag = NoteTag.valueOf(
                                noteData["characterTag"] as? String ?: noteData["tag"] as? String
                                ?: "ALIADO"
                            )
                            CharacterNote(note = note, tag = tag)
                        } else {
                            println("Document ${document.id} has null data")
                            null
                        }
                    } catch (e: Exception) {
                        println("Error processing document ${document.id}: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }

                println("Loaded ${notes.size} notes")
                isLoading = false

            } catch (e: Exception) {
                println("Error loading notes: ${e.message}")
                e.printStackTrace()

                // Provide more specific error messages
                error = when {
                    e.message?.contains("PERMISSION_DENIED") == true ->
                        "Sem permissão para acessar as notas. Verifique as regras do Firestore."

                    e.message?.contains("UNAUTHENTICATED") == true ->
                        "Usuário não autenticado. Faça login novamente."

                    e.message?.contains("NOT_FOUND") == true ->
                        "Personagem não encontrado."

                    else -> "Erro ao carregar notas: ${e.message}"
                }
                isLoading = false
            }
        }

        // Function to find existing note by title
        suspend fun findNoteByTitle(title: String): String? {
            return try {
                val notesSnapshot = db.collection("characters")
                    .document(characterId)
                    .collection("notes")
                    .whereEqualTo("title", title)
                    .get()
                    .await()

                notesSnapshot.documents.firstOrNull()?.id
            } catch (e: Exception) {
                null
            }
        }

        // Function to delete a note
        suspend fun deleteNote(noteId: String): Boolean {
            return try {
                db.collection("characters")
                    .document(characterId)
                    .collection("notes")
                    .document(noteId)
                    .delete()
                    .await()
                true
            } catch (e: Exception) {
                error = "Erro ao apagar a nota: ${e.message}"
                false
            }
        }

        // Helper function to create the shared note
        suspend fun createSharedNote(
            note: CharacterNote,
            targetCharacterId: String,
            targetCharacterName: String,
            currentCharacterName: String
        ): Boolean {
            return try {
                val currentUser = FirebaseAuth.getInstance().currentUser!!

                // Check if note with same title already exists
                val existingNotesSnapshot = db.collection("characters")
                    .document(targetCharacterId)
                    .collection("notes")
                    .whereEqualTo("title", note.note.title)
                    .get()
                    .await()

                println("Existing notes with same title: ${existingNotesSnapshot.documents.size}")

                // Prepare the shared note data
                val sharedNoteData = mapOf(
                    "title" to note.note.title,
                    "content" to note.note.content,
                    "tag" to note.note.tag.name,
                    "characterTag" to note.tag.name,
                    "imagePath" to null, // Don't share images
                    "sharedFrom" to currentUser.uid,
                    "sharedFromEmail" to (currentUser.email ?: ""),
                    "sharedFromCharacterName" to currentCharacterName,
                    "sharedAt" to System.currentTimeMillis(),
                    "originalCharacterId" to characterId,
                    "isShared" to true
                )

                // Save the note (update existing or create new)
                if (existingNotesSnapshot.documents.isNotEmpty()) {
                    val existingNoteId = existingNotesSnapshot.documents.first().id
                    println("Updating existing note: $existingNoteId")

                    db.collection("characters")
                        .document(targetCharacterId)
                        .collection("notes")
                        .document(existingNoteId)
                        .set(sharedNoteData)
                        .await()
                } else {
                    val noteId = UUID.randomUUID().toString()
                    println("Creating new note: $noteId")

                    db.collection("characters")
                        .document(targetCharacterId)
                        .collection("notes")
                        .document(noteId)
                        .set(sharedNoteData)
                        .await()
                }

                error = "Nota compartilhada com sucesso para $targetCharacterName"
                println("Note sharing completed successfully")
                true

            } catch (e: Exception) {
                println("Error creating shared note: ${e.message}")
                e.printStackTrace()
                false
            }
        }

        suspend fun shareNoteWithUser(
            note: CharacterNote,
            targetUserEmail: String,
            currentCharacterName: String,
            campaignName: String,
            campaignId: String
        ): Boolean {
            return try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    error = "Usuário não autenticado"
                    return false
                }

                println("=== SHARING DEBUG INFO ===")
                println("Source character ID: $characterId")
                println("Source character name: $currentCharacterName")
                println("Campaign name from source: '$campaignName'")
                println("Campaign ID from source: '$campaignId'")
                println("Target email: $targetUserEmail")

                // Validate campaign info
                if (campaignId.isBlank()) {
                    error = "Personagem não está associado a nenhuma campanha"
                    return false
                }

                // Step 1: Find the target user by email
                val usersSnapshot = db.collection("users")
                    .whereEqualTo("email", targetUserEmail.trim().lowercase())
                    .get()
                    .await()

                println("Users found with email: ${usersSnapshot.documents.size}")

                if (usersSnapshot.documents.isEmpty()) {
                    error = "Usuário com email '$targetUserEmail' não encontrado no sistema"
                    return false
                }

                val targetUserId = usersSnapshot.documents.first().id
                println("Target user ID: $targetUserId")

                // Step 2: Find ALL characters belonging to the target user
                val allTargetCharactersSnapshot = db.collection("characters")
                    .whereEqualTo("userId", targetUserId)
                    .get()
                    .await()

                println("Total characters for target user: ${allTargetCharactersSnapshot.documents.size}")

                // Debug: Print all target user's characters
                allTargetCharactersSnapshot.documents.forEach { doc ->
                    val charName = doc.getString("name") ?: "Unknown"
                    val charCampaignId = doc.getString("campaignId") ?: "null"
                    val charCampaignName = doc.getString("campaignName") ?: "null"
                    println("Character: $charName, CampaignId: '$charCampaignId', CampaignName: '$charCampaignName'")
                }

                // Step 3: Filter by campaign ID and name
                val targetCharactersInCampaign =
                    allTargetCharactersSnapshot.documents.filter { doc ->
                        val charCampaignId = doc.getString("campaignId") ?: ""
                        val matches = charCampaignId == campaignId && charCampaignId.isNotBlank()
                        println("Checking character ${doc.getString("name")}: campaignId='$charCampaignId' == '$campaignId' ? $matches")
                        matches
                    }

                println("Characters in same campaign (by ID): ${targetCharactersInCampaign.size}")

                if (targetCharactersInCampaign.isEmpty()) {
                    //check if campaign name matches
                    val targetCharactersByCampaignName =
                        allTargetCharactersSnapshot.documents.filter { doc ->
                            val charCampaignName = doc.getString("campaignName") ?: ""
                            val matches =
                                charCampaignName == campaignName && charCampaignName.isNotBlank()
                            println("Checking character ${doc.getString("name")}: campaignName='$charCampaignName' == '$campaignName' ? $matches")
                            matches
                        }

                    if (targetCharactersByCampaignName.isEmpty()) {
                        error =
                            "Usuário não possui personagem na campanha '$campaignName' (ID: $campaignId)"
                        return false
                    } else {
                        println("Found characters by campaign name instead of ID")
                        // Use the characters found by campaign name
                        val targetCharacterDoc = targetCharactersByCampaignName.first()
                        val targetCharacterId = targetCharacterDoc.id
                        val targetCharacterName =
                            targetCharacterDoc.getString("name") ?: "Personagem"

                        return createSharedNote(
                            note,
                            targetCharacterId,
                            targetCharacterName,
                            currentCharacterName
                        )
                    }
                }

                // Step 4: Use the first matching character
                val targetCharacterDoc = targetCharactersInCampaign.first()
                val targetCharacterId = targetCharacterDoc.id
                val targetCharacterName = targetCharacterDoc.getString("name") ?: "Personagem"

                println("Sharing to character: $targetCharacterName (ID: $targetCharacterId)")

                return createSharedNote(
                    note,
                    targetCharacterId,
                    targetCharacterName,
                    currentCharacterName
                )

            } catch (e: Exception) {
                println("Error sharing note: ${e.message}")
                e.printStackTrace()

                error = when {
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

        // Load notes from Firebase initially and when refreshTrigger changes
        LaunchedEffect(characterId, refreshTrigger) {
            loadCharacterInfo()
            loadNotes()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notas da Personagem", style = MaterialTheme.typography.headlineMedium)
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar nova nota"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                notes.isEmpty() -> {
                    Text(
                        text = "Nenhuma nota encontrada para esta personagem",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes) { characterNote ->
                            NoteCard(
                                characterNote = characterNote,
                                onEditClick = {
                                    selectedNote = characterNote
                                    showEditDialog = true
                                },
                                onDeleteClick = {
                                    selectedNote = characterNote
                                    showDeleteDialog = true
                                },
                                onShareClick = {
                                    selectedNote = characterNote
                                    showShareDialog = true
                                },
                                onImageClick = { imagePath ->
                                    selectedImagePath = imagePath
                                    showImageDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Edit Dialog
        if (showEditDialog && selectedNote != null) {
            EditNoteDialog(
                characterNote = selectedNote!!,
                existingTitles = notes.map { it.note.title }
                    .filter { it != selectedNote!!.note.title },
                onDismiss = { showEditDialog = false },
                onSave = { updatedNote ->
                    coroutineScope.launch {
                        try {
                            // Check if the new title conflicts with existing notes (excluding current note)
                            val conflictingNoteId =
                                if (updatedNote.note.title != selectedNote!!.note.title) {
                                    findNoteByTitle(updatedNote.note.title)
                                } else null

                            val noteData = mapOf(
                                "title" to updatedNote.note.title,
                                "content" to updatedNote.note.content,
                                "tag" to updatedNote.note.tag.name,
                                "characterTag" to updatedNote.tag.name,
                                "imagePath" to updatedNote.note.imagePath
                            )

                            // If there's a conflicting note, delete it first
                            conflictingNoteId?.let { conflictId ->
                                db.collection("characters")
                                    .document(characterId)
                                    .collection("notes")
                                    .document(conflictId)
                                    .delete()
                                    .await()
                            }

                            // Update the current note
                            db.collection("characters")
                                .document(characterId)
                                .collection("notes")
                                .document(updatedNote.note.id)
                                .set(noteData)
                                .await()

                            showEditDialog = false
                            // Trigger refresh by incrementing the refresh counter
                            refreshTrigger++
                        } catch (e: Exception) {
                            error = "Erro ao salvar nota: ${e.message}"
                            showEditDialog = false
                        }
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog && selectedNote != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirmar Exclusão") },
                text = { Text("Tem certeza que deseja excluir a nota \"${selectedNote!!.note.title}\"? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = deleteNote(selectedNote!!.note.id)
                                if (success) {
                                    // Also delete the local image if it exists
                                    selectedNote!!.note.imagePath?.let { imagePath ->
                                        LocalImageUtils.deleteLocalImage(imagePath)
                                    }
                                    showDeleteDialog = false
                                    refreshTrigger++
                                }
                            }
                        }
                    ) {
                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Share Dialog
        if (showShareDialog && selectedNote != null) {
            ShareNoteDialog(
                note = selectedNote!!,
                onDismiss = { showShareDialog = false },
                onShare = { targetUserEmail ->
                    coroutineScope.launch {
                        val success = shareNoteWithUser(
                            selectedNote!!,
                            targetUserEmail,
                            characterName,
                            campaignName,
                            campaignId
                        )
                        if (success) {
                            showShareDialog = false
                        }
                    }
                }
            )
        }


        // Add Dialog
        if (showAddDialog) {
            AddNoteDialog(
                existingTitles = notes.map { it.note.title },
                onDismiss = { showAddDialog = false },
                onSave = { title, content, tag, imagePath ->
                    coroutineScope.launch {
                        try {
                            // Check if a note with this title already exists
                            val existingNoteId = findNoteByTitle(title)

                            val noteData = mapOf(
                                "title" to title,
                                "content" to content,
                                "tag" to tag.name,
                                "characterTag" to tag.name,
                                "imagePath" to imagePath
                            )

                            if (existingNoteId != null) {
                                // Overwrite existing note
                                db.collection("characters")
                                    .document(characterId)
                                    .collection("notes")
                                    .document(existingNoteId)
                                    .set(noteData)
                                    .await()
                            } else {
                                // Create new note
                                val noteId = UUID.randomUUID().toString()
                                db.collection("characters")
                                    .document(characterId)
                                    .collection("notes")
                                    .document(noteId)
                                    .set(noteData)
                                    .await()
                            }

                            showAddDialog = false
                            // Trigger refresh by incrementing the refresh counter
                            refreshTrigger++
                        } catch (e: Exception) {
                            error = "Erro ao criar nota: ${e.message}"
                            showAddDialog = false
                        }
                    }
                }
            )
        }

        // Replace your existing image dialog code with this FULL SCREEN version
        if (showImageDialog && selectedImagePath != null) {
            // Full screen overlay approach
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable {
                        showImageDialog = false
                        selectedImagePath = null
                    }
            ) {
                // Close button in top-right corner
                TextButton(
                    onClick = {
                        showImageDialog = false
                        selectedImagePath = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Text("Fechar", color = Color.White)
                }

                // Full screen zoomable image
                ZoomableImage(
                    imagePath = selectedImagePath!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

    }

    @Composable
    fun NoteCard(
        characterNote: CharacterNote,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit,
        onShareClick: () -> Unit,
        onImageClick: (String) -> Unit = {}
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(min = 120.dp)
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
                        .wrapContentHeight()
                        .padding(20.dp) // Increased padding for more paper-like feel
                ) {
                    // Header with title and actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = characterNote.note.title,
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
                                onClick = onShareClick,
                                icon = Icons.Default.Share,
                                contentDescription = "Compartilhar"
                            )
                            PaperIconButton(
                                onClick = onEditClick,
                                icon = Icons.Default.Edit,
                                contentDescription = "Editar"
                            )
                            PaperIconButton(
                                onClick = onDeleteClick,
                                icon = Icons.Default.Delete,
                                contentDescription = "Deletar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main content area with image and tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Image on the left (if exists) - Make it clickable
                        characterNote.note.imagePath?.let { imagePath ->
                            if (LocalImageUtils.imageExists(imagePath)) {
                                Image(
                                    painter = rememberAsyncImagePainter(imagePath),
                                    contentDescription = "Imagem da nota",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImageClick(imagePath) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Tag and content column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tag with paper-like styling
                            PaperTagChip(
                                tag = characterNote.tag,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            // Content with handwritten-like styling
                            Text(
                                text = characterNote.note.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(start = 8.dp) // Indent like handwritten notes
                            )
                        }
                    }
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
        val tagColor = CharacterNotePaperTagColor(tag)

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


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditNoteDialog(
        characterNote: CharacterNote,
        existingTitles: List<String>,
        onDismiss: () -> Unit,
        onSave: (CharacterNote) -> Unit
    ) {
        val context = LocalContext.current
        var title by remember { mutableStateOf(characterNote.note.title) }
        var content by remember { mutableStateOf(characterNote.note.content) }
        var selectedTag by remember { mutableStateOf(characterNote.tag) }
        var expandedDropdown by remember { mutableStateOf(false) }
        var selectedImagePath by remember { mutableStateOf(characterNote.note.imagePath) }

        val titleExists = title.isNotBlank() && existingTitles.contains(title)

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val localPath = LocalImageUtils.saveImageLocally(context, it, "notes")
                selectedImagePath = localPath
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar Nota") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = titleExists,
                        supportingText = if (titleExists) {
                            { Text("Uma nota com este título já existe e será substituída", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Conteúdo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Tag Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedTag.name,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Tag") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            NoteTag.values().forEach { tag ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        color = getTagColor(tag),
                                                        shape = CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(tag.name)
                                        }
                                    },
                                    onClick = {
                                        selectedTag = tag
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Image section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Imagem:", style = MaterialTheme.typography.bodyMedium)

                        Row {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Selecionar")
                            }

                            if (selectedImagePath != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        selectedImagePath?.let { LocalImageUtils.deleteLocalImage(it) }
                                        selectedImagePath = null
                                    }
                                ) {
                                    Text("Remover", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Display selected image
                    selectedImagePath?.let { imagePath ->
                        if (LocalImageUtils.imageExists(imagePath)) {
                            Image(
                                painter = rememberAsyncImagePainter(imagePath),
                                contentDescription = "Imagem selecionada",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedNote = characterNote.copy(
                            note = characterNote.note.copy(
                                title = title,
                                content = content,
                                tag = selectedTag,
                                imagePath = selectedImagePath
                            ),
                            tag = selectedTag
                        )
                        onSave(updatedNote)
                    },
                    enabled = title.isNotBlank() && content.isNotBlank()
                ) {
                    Text(if (titleExists) "Substituir" else "Salvar")
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
    fun ZoomableImage(
        imagePath: String,
        modifier: Modifier = Modifier
    ) {
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
            // Apply zoom
            scale = (scale * zoomChange).coerceIn(0.5f, 5f)

            // Reset offset when zooming out to original size
            if (scale <= 1f) {
                offsetX = 0f
                offsetY = 0f
            } else {
                // Calculate max offset based on scale to prevent panning too far
                val maxOffsetX = (scale - 1f) * 300f
                val maxOffsetY = (scale - 1f) * 300f

                offsetX = (offsetX + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = (offsetY + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            }
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .transformable(state = transformableState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            Image(
                painter = rememberAsyncImagePainter(imagePath),
                contentDescription = "Imagem ampliada",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShareNoteDialog(
        note: CharacterNote,
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
            title = { Text("Compartilhar Nota") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Compartilhando: \"${note.note.title}\"",
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
                        text = "A nota será adicionada diretamente ao personagem do usuário que estiver na mesma campanha.",
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

    // Data class for Friend
    data class Friend(
        val email: String,
        val name: String
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddNoteDialog(
        existingTitles: List<String>,
        onDismiss: () -> Unit,
        onSave: (String, String, NoteTag, String?) -> Unit
    ) {
        val context = LocalContext.current
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var selectedTag by remember { mutableStateOf(NoteTag.ALIADO) }
        var expandedDropdown by remember { mutableStateOf(false) }
        var selectedImagePath by remember { mutableStateOf<String?>(null) }

        val titleExists = title.isNotBlank() && existingTitles.contains(title)

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val localPath = LocalImageUtils.saveImageLocally(context, it, "notes")
                selectedImagePath = localPath
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nova Nota") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = titleExists,
                        supportingText = if (titleExists) {
                            { Text("Uma nota com este título já existe e será substituída", color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Conteúdo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // Tag Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedTag.name,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Tag") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            NoteTag.values().forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag.name) },
                                    onClick = {
                                        selectedTag = tag
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Image section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Imagem:", style = MaterialTheme.typography.bodyMedium)

                        Row {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Selecionar")
                            }

                            if (selectedImagePath != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        selectedImagePath?.let { LocalImageUtils.deleteLocalImage(it) }
                                        selectedImagePath = null
                                    }
                                ) {
                                    Text("Remover", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Display selected image
                    selectedImagePath?.let { imagePath ->
                        if (LocalImageUtils.imageExists(imagePath)) {
                            Image(
                                painter = rememberAsyncImagePainter(imagePath),
                                contentDescription = "Imagem selecionada",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onSave(title, content, selectedTag, selectedImagePath)
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank()
                ) {
                    Text(if (titleExists) "Substituir" else "Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }