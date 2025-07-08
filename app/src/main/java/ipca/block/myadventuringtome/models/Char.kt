package ipca.block.myadventuringtome.models

import com.google.firebase.firestore.PropertyName
import ipca.block.myadventuringtome.models.Note

data class Character(
    @get:PropertyName("id") val id: String = "",
    @get:PropertyName("name") val name: String = "",
    @get:PropertyName("race") val race: String = "",
    @get:PropertyName("className") val className: String = "",
    @get:PropertyName("campaignId") val campaignId: String = "",
    @get:PropertyName("userId") val userId: String = "",
    @get:PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("notes") val notes: List<Note> = emptyList(),
    @get:PropertyName("imagePath") val imagePath: String? = null // Changed from "image" to "imagePath"
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", "", 0L, emptyList(), null)
}