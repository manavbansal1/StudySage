package com.group_7.studysage.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.group_7.studysage.data.models.GameType

@Composable
fun GameScreen(navController: NavController) {
    val games = listOf(
        Game("Quiz Race", Icons.Default.EmojiEvents, GameType.QUIZ_RACE),
        Game("Flashcard Battle", Icons.Default.Style, GameType.FLASHCARD_BATTLE),
        Game("Team Trivia", Icons.Default.Groups, GameType.TEAM_TRIVIA),
        Game("Speed Match", Icons.Default.Timer, GameType.SPEED_MATCH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Games", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                actions = {
                    IconButton(onClick = { /* TODO: Navigate to stats */ }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = paddingValues,
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(games) { game ->
                GameCard(game = game) {
                    navController.navigate("game_lobby/${game.gameType}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCard(game: Game, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = game.icon,
                contentDescription = game.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class Game(val title: String, val icon: ImageVector, val gameType: GameType)