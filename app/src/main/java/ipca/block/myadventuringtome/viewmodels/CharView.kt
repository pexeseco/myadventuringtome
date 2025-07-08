package ipca.block.myadventuringtome.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import ipca.block.myadventuringtome.models.Character

class CharacterViewModel : ViewModel() {

    // Use Firebase.firestore for the instance
    private val firestore = Firebase.firestore

    private val _characters = mutableStateOf<List<Character>>(emptyList())
    val characters = _characters

    private val _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    fun saveCharacter(
        character: Character,
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

        val characterWithUserId = character.copy(userId = userId)

        firestore.collection("characters")
            .add(characterWithUserId)
            .addOnSuccessListener { documentReference ->
                _isLoading.value = false
                // Update the character with the generated ID
                val updatedCharacter = characterWithUserId.copy(id = documentReference.id)
                firestore.collection("characters")
                    .document(documentReference.id)
                    .set(updatedCharacter)

                // Update local list
                val currentList = _characters.value.toMutableList()
                currentList.add(updatedCharacter)
                _characters.value = currentList

                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to save character: ${exception.message}")
            }
    }

    fun updateCharacter(
        character: Character,
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

        val characterWithUserId = character.copy(userId = userId)

        firestore.collection("characters")
            .document(character.id)
            .set(characterWithUserId)
            .addOnSuccessListener {
                _isLoading.value = false
                // Update the local list
                val currentList = _characters.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == character.id }
                if (index != -1) {
                    currentList[index] = characterWithUserId
                    _characters.value = currentList
                }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to update character: ${exception.message}")
            }
    }

    fun loadCharacters(campaignId: String? = null) {
        _isLoading.value = true

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            _isLoading.value = false
            return
        }

        var query = firestore.collection("characters")
            .whereEqualTo("userId", userId)

        // If campaignId is provided, filter by campaign
        if (campaignId != null) {
            query = query.whereEqualTo("campaignId", campaignId)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val characterList = documents.mapNotNull { document ->
                    document.toObject(Character::class.java)?.copy(id = document.id)
                }
                _characters.value = characterList
                _isLoading.value = false
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                // Handle error if needed
            }
    }

    fun deleteCharacter(
        characterId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true

        firestore.collection("characters")
            .document(characterId)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                // Remove from local list
                val currentList = _characters.value.toMutableList()
                currentList.removeAll { it.id == characterId }
                _characters.value = currentList
                onSuccess()
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                onError("Failed to delete character: ${exception.message}")
            }
    }
}