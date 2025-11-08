package com.group_7.studysage.ui.screens.GameScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.group_7.studysage.viewmodels.GameViewModel
import androidx.compose.material3.Surface


@Composable
fun GameScreen(navController: NavController, gameViewModel: GameViewModel = viewModel()) {
    val showGameActionOverlay by gameViewModel.showGameActionOverlay.collectAsState()
    val selectedGameTitle by gameViewModel.selectedGameTitle.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Header Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Challenge yourself!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Games",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Stats button
                    IconButton(
                        onClick = { /* TODO: Navigate to stats screen */ },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Game Stats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxSize().offset(y = -32.dp), // Added offset
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameCard(
                        title = "Quiz Game",
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        onClick = {
                            gameViewModel.setSelectedGameTitle("Quiz Game")
                            gameViewModel.setShowGameActionOverlay(true)
                        }
                    )
                    GameCard(
                        title = "Placeholder Game 1",
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        onClick = {
                            gameViewModel.setSelectedGameTitle("Placeholder Game 1")
                            gameViewModel.setShowGameActionOverlay(true)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameCard(
                        title = "Placeholder Game 2",
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        onClick = {
                            gameViewModel.setSelectedGameTitle("Placeholder Game 2")
                            gameViewModel.setShowGameActionOverlay(true)
                        }
                    )
                    GameCard(
                        title = "Placeholder Game 3",
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        onClick = {
                            gameViewModel.setSelectedGameTitle("Placeholder Game 3")
                            gameViewModel.setShowGameActionOverlay(true)
                        }
                    )
                }
            }
        }
    }

    if (showGameActionOverlay) {
        GameActionOverlay(
            gameTitle = selectedGameTitle,
            onHostGame = {
                // TODO: Implement host game logic
                gameViewModel.setShowGameActionOverlay(false)
            },
            onJoinGame = {
                // TODO: Implement join game logic
                gameViewModel.setShowGameActionOverlay(false)
            },
            onDismiss = { gameViewModel.setShowGameActionOverlay(false) }
        )
    }
}

@Composable
fun GameCard(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}



@Composable
fun GameActionOverlay(
    gameTitle: String,
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.7f) // Reduced width to 70%
                .height(180.dp) // Increased height slightly
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp), // Fill the surface and add padding
                verticalArrangement = Arrangement.SpaceEvenly, // Distribute buttons evenly
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onHostGame,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Host Game")
                }
                Button(
                    onClick = onJoinGame,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Join Game")
                }
            }
        }
    }
}