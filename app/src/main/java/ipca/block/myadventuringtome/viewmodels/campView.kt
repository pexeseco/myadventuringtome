package ipca.block.myadventuringtome.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import ipca.block.myadventuringtome.models.Campaign
import ipca.block.myadventuringtome.models.Note

class CampaignViewModel : ViewModel() {

    private val firestore = Firebase.firestore

    private val _campaigns = mutableStateOf<List<Campaign>>(emptyList())
    val campaigns = _campaigns

    private val _currentCampaign = mutableStateOf<Campaign?>(null)
    val currentCampaign = _currentCampaign

    private val _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    fun saveCampaign(
        campaign: Campaign,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            onError("User not logged in")
            return
        }

        val campaignWithUserId = campaign.copy(userId = userId)

        firestore.collection("campaigns")
            .add(campaignWithUserId)
            .addOnSuccessListener { documentReference ->
                _isLoading.value = false
                // Update the campaign with the generated ID
                val updatedCampaign = campaignWithUserId.copy(id = documentReference.id)
                firestore.collection("campaigns")
                    .document(documentReference.id)
                    .set(updatedCampaign)

                // Update local list
                val currentList = _campaigns.value.toMutableList()
                currentList.add(updatedCampaign)
                _campaigns.value = currentList

                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to save campaign: ${exception.message}")
            }
    }

    fun updateCampaign(
        campaign: Campaign,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            onError("User not logged in")
            return
        }

        val campaignWithUserId = campaign.copy(userId = userId)

        firestore.collection("campaigns")
            .document(campaign.id)
            .set(campaignWithUserId)
            .addOnSuccessListener {
                _isLoading.value = false
                // Update the local list
                val currentList = _campaigns.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == campaign.id }
                if (index != -1) {
                    currentList[index] = campaignWithUserId
                    _campaigns.value = currentList
                }
                // Update current campaign if it's the one being updated
                if (_currentCampaign.value?.id == campaign.id) {
                    _currentCampaign.value = campaignWithUserId
                }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to update campaign: ${exception.message}")
            }
    }

    fun loadCampaigns() {
        _isLoading.value = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            return
        }

        firestore.collection("campaigns")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val campaignList = documents.mapNotNull { document ->
                    document.toObject(Campaign::class.java)?.copy(id = document.id)
                }
                _campaigns.value = campaignList
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                // Handle error if needed
            }
    }

    fun loadCampaign(campaignId: String) {
        _isLoading.value = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            return
        }

        firestore.collection("campaigns")
            .document(campaignId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val campaign = document.toObject(Campaign::class.java)?.copy(id = document.id)
                    _currentCampaign.value = campaign
                }
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                // Handle error if needed
            }
    }

    fun deleteCampaign(
        campaignId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        firestore.collection("campaigns")
            .document(campaignId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                // Remove from local list
                val currentList = _campaigns.value.toMutableList()
                currentList.removeAll { it.id == campaignId }
                _campaigns.value = currentList

                // Clear current campaign if it's the one being deleted
                if (_currentCampaign.value?.id == campaignId) {
                    _currentCampaign.value = null
                }

                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to delete campaign: ${exception.message}")
            }
    }

    fun addNoteToCampaign(
        campaignId: String,
        note: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val campaign = _currentCampaign.value
        if (campaign == null || campaign.id != campaignId) {
            onError("Campaign not found")
            return
        }

        val updatedNotes = campaign.notes.toMutableList()
        updatedNotes.add(note)
        val updatedCampaign = campaign.copy(notes = updatedNotes)

        updateCampaign(
            campaign = updatedCampaign,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun updateNoteInCampaign(
        campaignId: String,
        noteId: String,
        updatedNote: Note,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val campaign = _currentCampaign.value
        if (campaign == null || campaign.id != campaignId) {
            onError("Campaign not found")
            return
        }

        val updatedNotes = campaign.notes.toMutableList()
        val noteIndex = updatedNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            updatedNotes[noteIndex] = updatedNote
            val updatedCampaign = campaign.copy(notes = updatedNotes)

            updateCampaign(
                campaign = updatedCampaign,
                onSuccess = onSuccess,
                onError = onError
            )
        } else {
            onError("Note not found")
        }
    }

    fun deleteNoteFromCampaign(
        campaignId: String,
        noteId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val campaign = _currentCampaign.value
        if (campaign == null || campaign.id != campaignId) {
            onError("Campaign not found")
            return
        }

        val updatedNotes = campaign.notes.toMutableList()
        updatedNotes.removeAll { it.id == noteId }
        val updatedCampaign = campaign.copy(notes = updatedNotes)

        updateCampaign(
            campaign = updatedCampaign,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}