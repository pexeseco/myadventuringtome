package ipca.block.myadventuringtome.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.block.myadventuringtome.models.Campaign
import ipca.block.myadventuringtome.models.NoteTag
import ipca.block.myadventuringtome.viewmodels.CampaignViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp

@Composable
fun CampaignScreen(
    navController: NavController,
    campaignViewModel: CampaignViewModel = viewModel()
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var campaignToDelete by remember { mutableStateOf<Campaign?>(null) }
    var campaignName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Load campaigns when the screen is first displayed
    LaunchedEffect(Unit) {
        campaignViewModel.loadCampaigns()
    }

    val campaigns by campaignViewModel.campaigns
    val isLoading by campaignViewModel.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("As Minhas Campanhas", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(campaigns) { campaign ->
                    CampaignCard(
                        campaign = campaign,
                        onClick = {
                            navController.navigate("campaign_details/${campaign.id}")
                        },
                        onDelete = {
                            campaignToDelete = campaign
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Criar nova Campanha")
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // Create Campaign Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                campaignName = ""
                errorMessage = ""
            },
            title = { Text("Criar Nova Campanha") },
            text = {
                Column {
                    OutlinedTextField(
                        value = campaignName,
                        onValueChange = {
                            campaignName = it
                            errorMessage = ""
                        },
                        label = { Text("Nome da Campanha") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                        if (campaignName.isBlank()) {
                            errorMessage = "Por favor, insira um nome para a campanha"
                            return@TextButton
                        }

                        val newCampaign = Campaign(
                            name = campaignName.trim()
                        )

                        campaignViewModel.saveCampaign(
                            campaign = newCampaign,
                            onSuccess = {
                                showCreateDialog = false
                                campaignName = ""
                                errorMessage = ""
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        campaignName = ""
                        errorMessage = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Campaign Confirmation Dialog
    if (showDeleteDialog && campaignToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                campaignToDelete = null
            },
            title = {
                Text(
                    "Eliminar Campanha",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("Tem a certeza que deseja eliminar a campanha:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${campaignToDelete!!.name}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Esta ação não pode ser desfeita.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        campaignViewModel.deleteCampaign(
                            campaignId = campaignToDelete!!.id,
                            onSuccess = {
                                showDeleteDialog = false
                                campaignToDelete = null
                                errorMessage = ""
                            },
                            onError = { error ->
                                errorMessage = error
                                showDeleteDialog = false
                                campaignToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        campaignToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
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


@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
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
                // Campaign name with handwritten-like styling
                Text(
                    text = campaign.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp) // Indent like handwritten notes
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campaign details with paper-like styling
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ID: ${campaign.id}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )

                    if (campaign.notes.isNotEmpty()) {
                        Text(
                            text = "${campaign.notes.size} notas",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
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

            // Delete button positioned at bottom-right
            PaperIconButton(
                onClick = { onDelete() },
                icon = Icons.Default.Delete,
                contentDescription = "Eliminar campanha",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }
}