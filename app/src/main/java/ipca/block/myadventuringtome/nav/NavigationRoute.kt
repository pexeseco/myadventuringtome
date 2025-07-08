package ipca.block.myadventuringtome.nav

sealed class NavRoutes(val route: String) {
    object Main : NavRoutes("main")
    object Characters : NavRoutes("character")
    object Campaigns : NavRoutes("campaings")
    object CharacterNotes : NavRoutes("character_notes/{characterId}")
    object Login : NavRoutes("login")
    object CreateCharacter : NavRoutes("create_character/{campaignId}/{characterId}") {
        fun createRoute(campaignId: String, characterId: String? = null) =
            "create_character/$campaignId/${characterId ?: "null"}"
    }
}