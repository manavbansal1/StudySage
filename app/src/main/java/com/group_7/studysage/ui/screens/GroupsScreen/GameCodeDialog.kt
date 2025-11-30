package com.group_7.studysage.ui.screens.GroupsScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GameCodeDialog(
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit
) {
    var gameCode by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Game Invite", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter the 6-character game code to invite others.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = gameCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            gameCode = it.uppercase()
                        }
                    },
                    label = { Text("Game Code") },
                    placeholder = { Text("ABC123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendCode(gameCode) },
                enabled = gameCode.length == 6
            ) {
                Text("Send Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
