package com.group_7.studysage.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.group_7.studysage.data.repository.AuthRepository
import com.group_7.studysage.ui.screens.viewmodels.ProfileViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    navController: NavController,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(authRepository) }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var showImageSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var editName by rememberSaveable { mutableStateOf("") }
    var editBio by rememberSaveable { mutableStateOf("") }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfileImage(context, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Snackbar for messages (Instead of Toast)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D1B4E),
                            Color(0xFF3D2B5E),
                            Color(0xFF2D1B4E)
                        )
                    )
                )
        ) {
            if (uiState.isLoading && uiState.userProfile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF9333EA))
                }
            } else {
                uiState.userProfile?.let { profile ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // PROFILE HEADER CARD - Match HomeScreen style
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF3D2564)  // Match home task cards
                                ),
                                border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Profile Picture
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.clickable { showImageSourceDialog = true }
                                    ) {
                                        val profileImage = profile["profileImageUrl"] as? String
                                        if (!profileImage.isNullOrBlank()) {
                                            Image(
                                                painter = rememberAsyncImagePainter(profileImage),
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clip(CircleShape)
                                                    .border(4.dp, Color(0xFF9333EA), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(120.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(4.dp, Color(0xFF9333EA), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (profile["name"] as? String)
                                                        ?.firstOrNull()
                                                        ?.toString()
                                                        ?.uppercase() ?: "U",
                                                    color = Color(0xFF9333EA),
                                                    fontSize = 48.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = profile["name"] as? String ?: "User",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = profile["email"] as? String ?: "",
                                        fontSize = 14.sp,
                                        color = Color(0xFFB0B0C0)
                                    )
                                }
                            }

                            // Back Button - Darker circle matching home
                            IconButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 24.dp, y = 24.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2D1B4E).copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Edit Button - Darker circle matching home
                            IconButton(
                                onClick = {
                                    editName = profile["name"] as? String ?: ""
                                    editBio = profile["bio"] as? String ?: ""
                                    showEditDialog = true
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-24).dp, y = 24.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2D1B4E).copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // ABOUT CARD - Match Home Task Cards
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2564)  // Same as home task cards
                            ),
                            border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Icon container - match home page style
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF2D1B4E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF9333EA),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        "About",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = profile["bio"] as? String ?: "Hey there! I'm using StudySage ✨",
                                        fontSize = 14.sp,
                                        color = Color(0xFFB0B0C0),
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // STATS ROW - Match Home Quick Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val level = profile["level"] as? Long ?: 1
                            val xp = profile["xpPoints"] as? Long ?: 0
                            val streak = profile["streakDays"] as? Long ?: 0

                            StatCard(
                                icon = Icons.Default.Star,
                                value = level.toString(),
                                label = "Level",
                                iconColor = Color(0xFFFBBF24),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                icon = Icons.Default.FavoriteBorder,
                                value = xp.toString(),
                                label = "XP",
                                iconColor = Color(0xFFEC4899),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                icon = Icons.Default.DateRange,
                                value = streak.toString(),
                                label = "Streak",
                                iconColor = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // CUSTOMIZE PROFILE SECTION
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2564)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Section Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF9333EA).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Customize Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }

                                SettingsOption(
                                    icon = Icons.Default.CameraAlt,
                                    title = "Change Profile Picture",
                                    subtitle = "Update your avatar",
                                    onClick = { showImageSourceDialog = true }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Edit,
                                    title = "Edit Bio",
                                    subtitle = "Tell others about yourself",
                                    onClick = {
                                        editName = profile["name"] as? String ?: ""
                                        editBio = profile["bio"] as? String ?: ""
                                        showEditDialog = true
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Tune,
                                    title = "Study Preferences",
                                    subtitle = "Set your learning style",
                                    onClick = { /* TODO: Navigate to preferences */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Star,
                                    title = "Favorite Subjects",
                                    subtitle = "Choose subjects you love",
                                    onClick = { /* TODO: Navigate to subjects */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Schedule,
                                    title = "Group Study Availability",
                                    subtitle = "Set when you're available",
                                    onClick = { /* TODO: Navigate to availability */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // PRIVACY SECTION
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2564)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Section Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF9333EA).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Privacy",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }

                                SettingsOption(
                                    icon = Icons.Default.Shield,
                                    title = "Privacy Settings",
                                    subtitle = "Control who sees your profile",
                                    onClick = { /* TODO: Navigate to privacy settings */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Security,
                                    title = "Account Security",
                                    subtitle = "Password and authentication",
                                    onClick = { /* TODO: Navigate to security */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Storage,
                                    title = "Data & Storage",
                                    subtitle = "Manage your data",
                                    onClick = { /* TODO: Navigate to data storage */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // HELP & SUPPORT SECTION
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2564)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Section Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF9333EA).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Help,
                                            contentDescription = null,
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Help & Support",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }

                                SettingsOption(
                                    icon = Icons.Default.MenuBook,
                                    title = "Help Center",
                                    subtitle = "FAQs and guides",
                                    onClick = { /* TODO: Navigate to help center */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Email,
                                    title = "Contact Support",
                                    subtitle = "Get help from our team",
                                    onClick = { /* TODO: Open email or support form */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.ReportProblem,
                                    title = "Report a Problem",
                                    subtitle = "Let us know about issues",
                                    onClick = { /* TODO: Navigate to report */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Description,
                                    title = "Terms & Privacy Policy",
                                    subtitle = "Read our policies",
                                    onClick = { /* TODO: Open terms/privacy */ }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsOption(
                                    icon = Icons.Default.Info,
                                    title = "About StudySage",
                                    subtitle = "App version and info",
                                    onClick = { /* TODO: Navigate to about */ },
                                    showChevron = false,
                                    endContent = {
                                        Text(
                                            text = "v1.0.0",
                                            fontSize = 13.sp,
                                            color = Color(0xFFB0B0C0)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SIGN OUT BUTTON - Match Home Theme
                        Card(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2564)  // Match other cards
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Sign Out",
                                        color = Color(0xFFEF4444),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            showEditDialog = false
                            showImageSourceDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Profile Picture")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfile(editName, editBio)
                        showEditDialog = false
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image Source Dialog
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Profile Picture") },
            text = {
                Text("Choose how you want to update your profile picture")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Text("Choose from Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Confirm Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Yes, Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2564)  // Match home cards
        ),
        border = BorderStroke(1.dp, Color(0xFF6B4BA6).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon in dark rounded container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D1B4E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFFB0B0C0),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingsOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    endContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2D1B4E).copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF9333EA).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0C0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // End content (chevron or custom)
            if (endContent != null) {
                endContent()
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = Color(0xFFB0B0C0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
