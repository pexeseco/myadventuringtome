package ipca.block.myadventuringtome.models

import com.google.firebase.firestore.PropertyName

data class Campaign(
    @get:PropertyName("id") val id: String = "",
    @get:PropertyName("name") val name: String = "",
    @get:PropertyName("userId") val userId: String = "",
    @get:PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("notes") val notes: List<Note> = emptyList()
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", 0L, emptyList())
}