package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.NoteViewModel
import com.example.ui.screens.NoteEditScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Notification Permission Launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt user for Notification Permission
        checkNotificationPermission()

        val viewModel = ViewModelProvider(this)[NoteViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Smart launcher integration: if launched from Notification, open NoteEdit Screen immediately
                LaunchedEffect(intent) {
                    val openNoteId = intent?.getLongExtra("OPEN_NOTE_ID", -1L) ?: -1L
                    if (openNoteId != -1L) {
                        navController.navigate("note_edit/$openNoteId/false")
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = "notes_list"
                    ) {
                        composable("notes_list") {
                            NotesListScreen(
                                viewModel = viewModel,
                                onNavigateToEdit = { noteId, isChecklist ->
                                    navController.navigate("note_edit/$noteId/$isChecklist")
                                }
                            )
                        }

                        composable(
                            route = "note_edit/{noteId}/{isChecklist}",
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.LongType },
                                navArgument("isChecklist") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                            val isChecklist = backStackEntry.arguments?.getBoolean("isChecklist") ?: false
                            NoteEditScreen(
                                viewModel = viewModel,
                                noteId = noteId,
                                isChecklistInitial = isChecklist,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
