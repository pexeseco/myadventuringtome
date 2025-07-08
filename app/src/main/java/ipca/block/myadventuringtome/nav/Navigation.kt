import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ipca.block.myadventuringtome.models.Campaign
import ipca.block.myadventuringtome.nav.NavRoutes
import ipca.block.myadventuringtome.screens.*
import ipca.block.myadventuringtome.viewmodels.CampaignViewModel

@Composable
fun AppNavigation(campaigns: List<Campaign>) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login.route
    ) {
        composable(NavRoutes.Main.route) {
            MainScreen(navController)
        }
        composable(NavRoutes.Characters.route) {
            CharacterScreen(
                navController = navController,
                campaignId = "",
            )
        }
        composable("campaign_details/{campaignId}") { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            val campaignViewModel: CampaignViewModel = viewModel()
            CampaignDetailsScreen(navController, campaignId, campaignViewModel)
        }

        composable(NavRoutes.Campaigns.route) {
            val campaignViewModel: CampaignViewModel = viewModel()
            CampaignScreen(navController, campaignViewModel)
        }

        composable(
            route = NavRoutes.CharacterNotes.route,
            arguments = listOf(navArgument("characterId") { type = NavType.StringType })
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
            CharacterNotesScreen(characterId = characterId, navController = navController)
        }
        composable(NavRoutes.Login.route) {
            LoginScreen(navController)
        }
        composable(
            route = NavRoutes.CreateCharacter.route,
            arguments = listOf(
                navArgument("campaignId") { type = NavType.StringType },
                navArgument("characterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val campaignId = backStackEntry.arguments?.getString("campaignId") ?: ""
            val characterId = backStackEntry.arguments?.getString("characterId")

            CreateCharacterScreen(
                navController = navController,
                campaignId = campaignId,
                characterId = if (characterId == "null") null else characterId,
                availableCampaigns = campaigns // Pass available campaigns
            )
        }
    }
}