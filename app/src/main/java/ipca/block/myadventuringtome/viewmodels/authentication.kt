package ipca.block.myadventuringtome.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    init {
        _currentUser.value = auth.currentUser
    }

    // Função para login com email e senha
    fun signInWithEmailPassword(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    // Ensure user document exists on sign-in
                    auth.currentUser?.let { user ->
                        createUserDocumentIfNotExists(user)
                    }
                }
                onResult(task.isSuccessful)
            }
    }

    // Função para registrar um novo usuário com email e senha
    fun signUpWithEmailPassword(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    // Create user document in Firestore
                    createUserDocument(auth.currentUser!!, onResult)
                } else {
                    onResult(false)
                }
            }
    }

    // Create user document in Firestore
    private fun createUserDocument(user: FirebaseUser, onResult: (Boolean) -> Unit) {
        val userData = mapOf(
            "email" to user.email,
            "uid" to user.uid
        )

        db.collection("users")
            .document(user.uid)
            .set(userData)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    // Create user document only if it doesn't exist
    private fun createUserDocumentIfNotExists(user: FirebaseUser) {
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    val userData = mapOf(
                        "email" to user.email,
                        "uid" to user.uid
                    )

                    db.collection("users")
                        .document(user.uid)
                        .set(userData)
                }
            }
    }

    // Função para fazer logout
    fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }

    // Re-authenticate user with their current password
    suspend fun reauthenticateUser(password: String): Pair<Boolean, String?> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Pair(false, "Utilizador não encontrado")
            }

            val email = user.email
            if (email == null) {
                return Pair(false, "Email do utilizador não encontrado")
            }

            println("Attempting re-authentication for user: $email")
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            println("Re-authentication successful")
            Pair(true, null)
        } catch (e: Exception) {
            println("Re-authentication failed: ${e.message}")
            e.printStackTrace()

            val errorMessage = when {
                e.message?.contains("password is invalid", ignoreCase = true) == true -> "Password incorreta"
                e.message?.contains("network", ignoreCase = true) == true -> "Erro de rede. Verifique a sua conexão"
                e.message?.contains("too-many-requests", ignoreCase = true) == true -> "Muitas tentativas. Tente novamente mais tarde"
                else -> "Erro de autenticação: ${e.message}"
            }

            Pair(false, errorMessage)
        }
    }

    // Delete user account and all associated data
    suspend fun deleteUserAccount(): Pair<Boolean, String?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Pair(false, "Utilizador não encontrado")
            }

            val userId = currentUser.uid
            println("Starting account deletion for user: $userId")

            // Delete user's characters and their notes
            try {
                println("Deleting characters...")
                val charactersSnapshot = db.collection("characters")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                println("Found ${charactersSnapshot.documents.size} characters to delete")

                for (characterDoc in charactersSnapshot.documents) {
                    val characterId = characterDoc.id
                    println("Deleting character: $characterId")

                    // Delete all notes for this character
                    try {
                        val notesSnapshot = db.collection("characters")
                            .document(characterId)
                            .collection("notes")
                            .get()
                            .await()

                        println("Found ${notesSnapshot.documents.size} notes for character $characterId")

                        for (noteDoc in notesSnapshot.documents) {
                            noteDoc.reference.delete().await()
                        }
                    } catch (e: Exception) {
                        println("Error deleting notes for character $characterId: ${e.message}")
                    }

                    // Delete the character
                    characterDoc.reference.delete().await()
                }
            } catch (e: Exception) {
                println("Error deleting characters: ${e.message}")
            }

            // Delete user's campaigns
            try {
                println("Deleting campaigns...")
                val campaignsSnapshot = db.collection("campaigns")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                println("Found ${campaignsSnapshot.documents.size} campaigns to delete")

                for (campaignDoc in campaignsSnapshot.documents) {
                    campaignDoc.reference.delete().await()
                }
            } catch (e: Exception) {
                println("Error deleting campaigns: ${e.message}")
            }

            // Delete user's shared notes
            try {
                println("Deleting shared notes...")
                val sharedNotesSnapshot = db.collection("users")
                    .document(userId)
                    .collection("sharedNotes")
                    .get()
                    .await()

                println("Found ${sharedNotesSnapshot.documents.size} shared notes to delete")

                for (sharedNoteDoc in sharedNotesSnapshot.documents) {
                    sharedNoteDoc.reference.delete().await()
                }
            } catch (e: Exception) {
                println("Error deleting shared notes: ${e.message}")
            }

            // Delete user document
            try {
                println("Deleting user document...")
                db.collection("users")
                    .document(userId)
                    .delete()
                    .await()
                println("User document deleted successfully")
            } catch (e: Exception) {
                println("Error deleting user document: ${e.message}")
            }

            //delete the Firebase Auth user
            try {
                println("Deleting Firebase Auth user...")
                currentUser.delete().await()
                println("Firebase Auth user deleted successfully")
                _currentUser.value = null
                Pair(true, null)
            } catch (e: Exception) {
                println("Error deleting Firebase Auth user: ${e.message}")
                e.printStackTrace()

                val errorMessage = when {
                    e.message?.contains("requires-recent-login", ignoreCase = true) == true ->
                        "Sessão expirada. Faça login novamente"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Erro de rede. Verifique a sua conexão"
                    e.message?.contains("user-not-found", ignoreCase = true) == true ->
                        "Utilizador não encontrado"
                    else -> "Erro ao eliminar conta: ${e.message}"
                }

                Pair(false, errorMessage)
            }

        } catch (e: Exception) {
            println("Account deletion failed: ${e.message}")
            e.printStackTrace()
            Pair(false, "Erro inesperado: ${e.message}")
        }
    }

    suspend fun addFriend(friendName: String, friendEmail: String): Pair<Boolean, String?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Pair(false, "Utilizador não autenticado")
            }

            val userId = currentUser.uid
            val userDocRef = firestore.collection("users").document(userId)

            // Create friend data
            val friendData = hashMapOf(
                "name" to friendName,
                "email" to friendEmail,
                "addedAt" to com.google.firebase.Timestamp.now()
            )

            // Add friend to the user's friends array
            userDocRef.update("friends", com.google.firebase.firestore.FieldValue.arrayUnion(friendData))
                .await()

            Pair(true, null)
        } catch (e: Exception) {
            println("Error adding friend: ${e.message}")
            e.printStackTrace()
            Pair(false, "Erro ao adicionar amigo: ${e.message}")
        }
    }
}