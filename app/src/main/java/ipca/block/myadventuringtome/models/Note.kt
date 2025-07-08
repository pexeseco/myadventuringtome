package ipca.block.myadventuringtome.models

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val tag: NoteTag = NoteTag.DESCONFIADO,
    val imagePath: String? = null
)

enum class NoteTag {
    ALIADO, INIMIGO, DESCONFIADO, FAMILIAR
}
