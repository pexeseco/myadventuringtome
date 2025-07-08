package ipca.block.myadventuringtome.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import ipca.block.myadventuringtome.models.Character
import ipca.block.myadventuringtome.nav.NavRoutes
import ipca.block.myadventuringtome.utils.LocalImageUtils
import ipca.block.myadventuringtome.viewmodels.CharacterViewModel

@Composable
fun CharacterScreen(
    navController: NavController,
    campaignId: String = "",
    viewModel: CharacterViewModel = viewModel()
) {
    val characters by viewModel.characters
    val isLoading by viewModel.isLoading
    var characterToDelete by remember { mutableStateOf<String?>(null) }

    // Load ALL characters for the user (not filtered by campaign)
    LaunchedEffect(Unit) {
        viewModel.loadCharacters() // Don't pass campaignId to get all characters
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Minhas Personagens", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (characters.isEmpty()) {
            Text("Nenhuma personagem encontrada.")
        } else {
            LazyColumn {
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        onCardClick = {
                            navController.navigate("character_notes/${character.id}")
                        },
                        onEditClick = {
                            navController.navigate("create_character/default/${character.id}")
                        },
                        onDeleteClick = {
                            characterToDelete = character.id
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val defaultCampaignId = if (campaignId.isNotEmpty()) campaignId else "default"
            navController.navigate(NavRoutes.CreateCharacter.createRoute(defaultCampaignId))
        }) {
            Text("Criar nova Personagem")
        }
    }

    // Delete confirmation dialog
    if (characterToDelete != null) {
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            title = { Text("Confirmar exclusão") },
            text = { Text("Tem certeza que deseja deletar esta personagem? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        characterToDelete?.let { id ->
                            // Find the character to get its image path for deletion
                            val characterToDeleteObj = characters.find { it.id == id }
                            viewModel.deleteCharacter(
                                characterId = id,
                                onSuccess = {
                                    // Delete the character's image if it exists
                                    characterToDeleteObj?.imagePath?.let { imagePath ->
                                        LocalImageUtils.deleteLocalImage(imagePath)
                                    }
                                    println("Delete successful, navigating back...")
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    println("Delete failed: $error")
                                }
                            )
                        }
                        characterToDelete = null
                    }
                ) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { characterToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CharacterCard(
    character: Character,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
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
                    .padding(20.dp)
            ) {
                // Main content area with image on left and text on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Character image on the left (if exists)
                    character.imagePath?.let { imagePath ->
                        if (LocalImageUtils.imageExists(imagePath)) {
                            Image(
                                painter = rememberAsyncImagePainter(imagePath),
                                contentDescription = "Imagem da personagem",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Character info on the right
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Character name with handwritten-like styling
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        )

                        // Character details with paper-like styling
                        Column(
                            modifier = Modifier.padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${character.race} ${character.className}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )

                            Text(
                                text = "Campanha: ${character.campaignId}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
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

            // Action buttons positioned at bottom-right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperIconButton(
                    onClick = onEditClick,
                    icon = Icons.Default.Edit,
                    contentDescription = "Editar personagem",
                    tint = MaterialTheme.colorScheme.primary
                )
                PaperIconButton(
                    onClick = onDeleteClick,
                    icon = Icons.Default.Delete,
                    contentDescription = "Deletar personagem",
                    tint = MaterialTheme.colorScheme.error
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
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
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