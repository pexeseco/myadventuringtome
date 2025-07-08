package ipca.block.myadventuringtome.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import ipca.block.myadventuringtome.models.Campaign
import ipca.block.myadventuringtome.models.Character
import androidx.lifecycle.viewmodel.compose.viewModel
import ipca.block.myadventuringtome.viewmodels.CharacterViewModel
import ipca.block.myadventuringtome.utils.LocalImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCharacterScreen(
    navController: NavController,
    campaignId: String,
    characterId: String? = null, // Add optional characterId for editing
    availableCampaigns: List<Campaign> = emptyList(),
    viewModel: CharacterViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var race by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var campaignName by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImagePath by remember { mutableStateOf<String?>(null) }

    // Get context properly in Compose
    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Observe loading state
    val isLoading by viewModel.isLoading
    val characters by viewModel.characters

    // Load character data if editing
    LaunchedEffect(characterId) {
        if (characterId != null && characterId != "null") {
            isEditMode = true
            viewModel.loadCharacters() // Load all characters to find the one we're editing
        }
    }

    // Populate fields when editing
    LaunchedEffect(characters, characterId) {
        if (isEditMode && characterId != null) {
            val characterToEdit = characters.find { it.id == characterId }
            characterToEdit?.let { character ->
                name = character.name
                race = character.race
                className = character.className
                campaignName = character.campaignId
                currentImagePath = character.imagePath
            }
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
            text = if (isEditMode) "Editar Personagem" else "Criar Nova Personagem",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Image selection section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Display current or selected image
            val imageToShow = selectedImageUri?.toString() ?: currentImagePath

            if (imageToShow != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageToShow),
                    contentDescription = "Imagem da personagem",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Image selection button
            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (imageToShow != null) "Alterar Imagem" else "Adicionar Imagem")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = race,
            onValueChange = { race = it },
            label = { Text("Raça") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = className,
            onValueChange = { className = it },
            label = { Text("Classe") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campaign Name Text Input (instead of dropdown)
        OutlinedTextField(
            value = campaignName,
            onValueChange = { campaignName = it },
            label = { Text("Nome da Campanha") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && race.isNotBlank() && className.isNotBlank() && campaignName.isNotBlank()) {
                    if (isEditMode && characterId != null) {
                        // Handle image for update
                        val finalImagePath = if (selectedImageUri != null) {
                            // Save new image and delete old one if exists
                            currentImagePath?.let { oldPath ->
                                LocalImageUtils.deleteLocalImage(oldPath)
                            }
                            LocalImageUtils.saveImageLocally(context, selectedImageUri!!, "character_$characterId")
                        } else {
                            currentImagePath // Keep existing image
                        }

                        // Update existing character
                        val updatedCharacter = Character(
                            id = characterId,
                            name = name,
                            race = race,
                            className = className,
                            campaignId = campaignName,
                            imagePath = finalImagePath
                        )

                        viewModel.updateCharacter(
                            character = updatedCharacter,
                            onSuccess = {
                                Toast.makeText(context, "Personagem Atualizado!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // Handle image for new character
                        val imagePath = selectedImageUri?.let { uri ->
                            LocalImageUtils.saveImageLocally(context, uri, "character_${System.currentTimeMillis()}")
                        }

                        // Create new character
                        val newCharacter = Character(
                            id = "", // Firestore will generate the ID
                            name = name,
                            race = race,
                            className = className,
                            campaignId = campaignName,
                            imagePath = imagePath
                            // userId will be set in the ViewModel
                        )

                        viewModel.saveCharacter(
                            character = newCharacter,
                            onSuccess = {
                                Toast.makeText(context, "Personagem Criado!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    Toast.makeText(context, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable button while saving
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text(if (isEditMode) "Atualizar Personagem" else "Criar Personagem")
            }
        }

        // Cancel button for edit mode
        if (isEditMode) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}