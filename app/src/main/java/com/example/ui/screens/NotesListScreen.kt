package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChecklistItem
import com.example.data.ChecklistSerializer
import com.example.data.NoteEntity
import com.example.ui.NoteViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DrawerCategory {
    NOTES, REMINDERS, ARCHIVE, TRASH, LABEL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesListScreen(
    viewModel: NoteViewModel,
    onNavigateToEdit: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val trashedNotes by viewModel.trashedNotes.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilterColor by viewModel.selectedFilterColor.collectAsStateWithLifecycle()
    val selectedFilterLabel by viewModel.selectedFilterLabel.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentCategory by remember { mutableStateOf(DrawerCategory.NOTES) }
    var currentLabelFilter by remember { mutableStateOf<String?>(null) }
    var isGridView by remember { mutableStateOf(true) }
    var isSnoozed by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentNotesList = when (currentCategory) {
        DrawerCategory.NOTES -> activeNotes
        DrawerCategory.REMINDERS -> activeNotes.filter { it.reminderTime != null }
        DrawerCategory.ARCHIVE -> archivedNotes
        DrawerCategory.TRASH -> trashedNotes
        DrawerCategory.LABEL -> activeNotes.filter { it.label == currentLabelFilter }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NotePad+",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(if (currentCategory == DrawerCategory.NOTES) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb, contentDescription = null) },
                    label = { Text("Notes", fontWeight = FontWeight.SemiBold) },
                    selected = currentCategory == DrawerCategory.NOTES,
                    onClick = {
                        currentCategory = DrawerCategory.NOTES
                        viewModel.setFilterLabel(null)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(if (currentCategory == DrawerCategory.REMINDERS) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive, contentDescription = null) },
                    label = { Text("Reminders", fontWeight = FontWeight.SemiBold) },
                    selected = currentCategory == DrawerCategory.REMINDERS,
                    onClick = {
                        currentCategory = DrawerCategory.REMINDERS
                        viewModel.setFilterLabel(null)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Dynamic Labels Section
                if (allLabels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Labels",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allLabels.forEach { labelName ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                            label = { Text(labelName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = currentCategory == DrawerCategory.LABEL && currentLabelFilter == labelName,
                            onClick = {
                                currentCategory = DrawerCategory.LABEL
                                currentLabelFilter = labelName
                                viewModel.setFilterLabel(labelName)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(if (currentCategory == DrawerCategory.ARCHIVE) Icons.Filled.Archive else Icons.Outlined.Archive, contentDescription = null) },
                    label = { Text("Archive", fontWeight = FontWeight.SemiBold) },
                    selected = currentCategory == DrawerCategory.ARCHIVE,
                    onClick = {
                        currentCategory = DrawerCategory.ARCHIVE
                        viewModel.setFilterLabel(null)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(if (currentCategory == DrawerCategory.TRASH) Icons.Filled.Delete else Icons.Outlined.Delete, contentDescription = null) },
                    label = { Text("Trash", fontWeight = FontWeight.SemiBold) },
                    selected = currentCategory == DrawerCategory.TRASH,
                    onClick = {
                        currentCategory = DrawerCategory.TRASH
                        viewModel.setFilterLabel(null)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Info, contentDescription = "About NotePad+") },
                    label = { Text("About NotePad+", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    // Sleek Brand Top Header (Mockup matched!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Custom App Logo (N+)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Indigo600, RoundedCornerShape(12.dp))
                                    .border(2.dp, Indigo800, RoundedCornerShape(12.dp))
                                    .clickable { scope.launch { drawerState.open() } },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "N",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "+",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }
                            Text(
                                text = "NotePad+",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) Color.White else Slate900,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // JD Avatar on Right
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSystemInDarkTheme()) Slate900 else Slate100)
                                .border(1.dp, if (isSystemInDarkTheme()) Slate400 else Slate200, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemInDarkTheme()) Color.White else Slate900
                            )
                        }
                    }

                    // Modern Search bar input (Mockup styled!)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("search_bar_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemInDarkTheme()) DarkSurface else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSystemInDarkTheme()) Slate900 else Slate200
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("drawer_menu_button")
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Open Navigation Menu",
                                    tint = if (isSystemInDarkTheme()) Color.White else Slate900
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.padding(start = 4.dp).size(20.dp)
                            )

                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Search your notes",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate400
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_text_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isSystemInDarkTheme()) Color.White else Slate900
                                ),
                                singleLine = true
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Slate400
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isGridView = !isGridView },
                                modifier = Modifier.testTag("layout_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                                    tint = if (isSystemInDarkTheme()) Color.White else Slate900
                                )
                            }
                        }
                    }

                    // Smart Notifications (Simulating Reminder Ringing Banner from Design HTML)
                    AnimatedVisibility(
                        visible = !isSnoozed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Indigo600)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Ringing Alarm Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🔔", fontSize = 16.sp)
                                    }

                                    Column {
                                        Text(
                                            text = "REMINDER RINGING",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "Meeting in 5 mins",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }

                                Button(
                                    onClick = { isSnoozed = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.15f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("SNOOZE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Horizontal scrolling quick filters for colors (Active only)
                    if (currentCategory == DrawerCategory.NOTES && activeNotes.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Filters:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Color Filter Toggle Indicator
                            val isColorFiltered = selectedFilterColor != null
                            InputChip(
                                selected = isColorFiltered,
                                onClick = {
                                    if (isColorFiltered) {
                                        viewModel.setFilterColor(null)
                                    } else {
                                        viewModel.setFilterColor(1) // coral
                                    }
                                },
                                label = { Text("Color coded") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Show Banner for Trashed or Archived view
                    if (currentCategory == DrawerCategory.TRASH) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes in trash are deleted permanently.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.emptyTrash() },
                                    modifier = Modifier.testTag("empty_trash_button")
                                ) {
                                    Text("Empty Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Customized elegant bottom bar with quick compose tools & Fab (Keep style)
                BottomAppBar(
                    actions = {
                        IconButton(
                            onClick = { onNavigateToEdit(-1L, true) }, // Create checklist
                            modifier = Modifier.testTag("quick_checklist_button")
                        ) {
                            Icon(Icons.Outlined.CheckBox, contentDescription = "New Checklist Note")
                        }
                        IconButton(
                            onClick = { onNavigateToEdit(-1L, false) } // Create drawing placeholder / note
                        ) {
                            Icon(Icons.Outlined.Brush, contentDescription = "New Draw Note")
                        }
                        IconButton(
                            onClick = { onNavigateToEdit(-1L, false) }
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = "Voice Note")
                        }
                        IconButton(
                            onClick = { onNavigateToEdit(-1L, false) }
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Add Image")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { onNavigateToEdit(-1L, false) }, // Create standard note
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("add_note_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Text Note")
                        }
                    },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (currentNotesList.isEmpty()) {
                    // Beautiful Centered Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (currentCategory) {
                                DrawerCategory.NOTES -> Icons.Outlined.Lightbulb
                                DrawerCategory.REMINDERS -> Icons.Outlined.Notifications
                                DrawerCategory.ARCHIVE -> Icons.Outlined.Archive
                                DrawerCategory.TRASH -> Icons.Outlined.Delete
                                DrawerCategory.LABEL -> Icons.Outlined.Label
                            },
                            contentDescription = "No notes",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (currentCategory) {
                                DrawerCategory.NOTES -> "Notes you add appear here"
                                DrawerCategory.REMINDERS -> "Notes with upcoming reminders appear here"
                                DrawerCategory.ARCHIVE -> "Archived notes appear here"
                                DrawerCategory.TRASH -> "No notes in Trash"
                                DrawerCategory.LABEL -> "No notes with this label"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Display either 2-Column Staggered Grid (Google Keep default) or 1-Column List
                    if (isGridView) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp
                        ) {
                            items(currentNotesList, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onCardClick = { onNavigateToEdit(note.id, note.type == "checklist") },
                                    onPinToggle = { viewModel.togglePin(note) },
                                    isTrash = currentCategory == DrawerCategory.TRASH
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentNotesList, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onCardClick = { onNavigateToEdit(note.id, note.type == "checklist") },
                                    onPinToggle = { viewModel.togglePin(note) },
                                    isTrash = currentCategory == DrawerCategory.TRASH
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    onCardClick: () -> Unit,
    onPinToggle: () -> Unit,
    isTrash: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val noteBgColor = getNoteColor(note.colorIndex, isDark)
    val noteOnColor = getNoteOnColor(note.colorIndex, isDark)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (note.colorIndex == 0) {
                    if (isDark) Color.White.copy(alpha = 0.15f) else Slate200
                } else {
                    noteBgColor.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onCardClick
            )
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = noteBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = noteOnColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!isTrash) {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("pin_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else noteOnColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Text Snippet or Checklist Item list preview
            if (note.type == "checklist") {
                val items = remember(note.checklistItemsJson) {
                    ChecklistSerializer.fromJson(note.checklistItemsJson)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Show top 4 items
                    items.take(4).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = noteOnColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.isChecked) noteOnColor.copy(alpha = 0.5f) else noteOnColor,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (items.size > 4) {
                        Text(
                            text = "+ ${items.size - 4} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = noteOnColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                        )
                    }
                }
            } else {
                if (note.content.isNotEmpty()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = noteOnColor.copy(alpha = 0.9f),
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Chips for tags/reminders
            if (note.label.isNotEmpty() || (note.reminderTime != null && !isTrash)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reminder Chip
                    if (note.reminderTime != null && !isTrash) {
                        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        val dateText = sdf.format(Date(note.reminderTime))
                        val hasPassed = note.reminderTime < System.currentTimeMillis()

                        Surface(
                            color = if (hasPassed) noteOnColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (note.isReminderTriggered) Icons.Default.NotificationsNone else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (hasPassed) noteOnColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (hasPassed) noteOnColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Label Chip (Mockup styled!)
                    if (note.label.isNotEmpty()) {
                        Surface(
                            color = if (isDark) Indigo800.copy(alpha = 0.4f) else Indigo50,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(
                                text = note.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isDark) Indigo100 else Indigo600,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Indigo600)
            ) {
                Text("CLOSE", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDark) DarkSurface else Color.White,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large styled "N+" App Icon representation in dialog
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Indigo600, RoundedCornerShape(16.dp))
                        .border(2.dp, Indigo800, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "N",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NotePad+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Slate900
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider(color = if (isDark) Slate900 else Slate100)

                // ABOUT DEVELOPER SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👨‍💻", fontSize = 18.sp)
                        Text(
                            text = "About Developer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Indigo100 else Indigo800
                        )
                    }
                    Text(
                        text = "Prince AR Abdur Rahman",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Slate400 else Slate900.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }

                // CONTACTS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Contact & Socials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Indigo100 else Indigo800
                    )

                    // WhatsApp 1
                    ContactItem(
                        icon = "💬",
                        label = "WhatsApp (Primary)",
                        value = "01707424006",
                        onClick = {
                            try {
                                uriHandler.openUri("https://wa.me/8801707424006")
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString("01707424006"))
                                Toast.makeText(context, "Number copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // WhatsApp 2
                    ContactItem(
                        icon = "💬",
                        label = "WhatsApp (Alternative)",
                        value = "01796951709",
                        onClick = {
                            try {
                                uriHandler.openUri("https://wa.me/8801796951709")
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString("01796951709"))
                                Toast.makeText(context, "Number copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Facebook
                    ContactItem(
                        icon = "🌐",
                        label = "Facebook",
                        value = "Visit Profile",
                        onClick = {
                            uriHandler.openUri("https://www.facebook.com/share/1BNn32qoJo/")
                        }
                    )

                    // Instagram
                    ContactItem(
                        icon = "📸",
                        label = "Instagram",
                        value = "@ur___abdur____rahman__2008",
                        onClick = {
                            uriHandler.openUri("https://www.instagram.com/ur___abdur____rahman__2008")
                        }
                    )
                }

                HorizontalDivider(color = if (isDark) Slate900 else Slate100)

                // ABOUT COMPANY SECTION
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏢", fontSize = 18.sp)
                        Text(
                            text = "About Company",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Indigo100 else Indigo800
                        )
                    }
                    Text(
                        text = "NexVora Lab's Ofc",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Slate900
                    )
                    Text(
                        text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Slate400 else Slate900.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Our Mission",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Indigo100 else Indigo800
                    )
                    Text(
                        text = "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Slate400 else Slate900.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }

                HorizontalDivider(color = if (isDark) Slate900 else Slate100)

                // CREDITS & COPYRIGHT
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Developed by Prince AR Abdur Rahman",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Published by NexVora Lab's Ofc",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

@Composable
fun ContactItem(
    icon: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkBackground else Slate50
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Slate900 else Slate100
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(icon, fontSize = 16.sp)
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Slate900,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
