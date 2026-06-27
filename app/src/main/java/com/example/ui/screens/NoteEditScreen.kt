package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChecklistItem
import com.example.data.ChecklistSerializer
import com.example.data.NoteEntity
import com.example.ui.NoteViewModel
import com.example.ui.theme.NoteColorsList
import com.example.ui.theme.getNoteColor
import com.example.ui.theme.getNoteOnColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    isChecklistInitial: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Note draft state
    var note by remember { mutableStateOf<NoteEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var colorIndex by remember { mutableStateOf(0) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isTrashed by remember { mutableStateOf(false) }
    var labelText by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var isReminderTriggered by remember { mutableStateOf(false) }

    // Checklist specific states
    var checklistItems by remember { mutableStateOf<List<ChecklistItem>>(emptyList()) }
    var newChecklistItemText by remember { mutableStateOf("") }

    // Dialog flags
    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var isCheckedSectionExpanded by remember { mutableStateOf(true) }

    // Load note if editing existing, or create new draft template
    LaunchedEffect(noteId) {
        if (noteId != -1L) {
            viewModel.getNoteById(noteId).collect { loadedNote ->
                if (loadedNote != null && note == null) {
                    note = loadedNote
                    title = loadedNote.title
                    content = loadedNote.content
                    colorIndex = loadedNote.colorIndex
                    isPinned = loadedNote.isPinned
                    isArchived = loadedNote.isArchived
                    isTrashed = loadedNote.isTrashed
                    labelText = loadedNote.label
                    reminderTime = loadedNote.reminderTime
                    isReminderTriggered = loadedNote.isReminderTriggered
                    if (loadedNote.type == "checklist") {
                        checklistItems = ChecklistSerializer.fromJson(loadedNote.checklistItemsJson)
                    }
                }
            }
        } else {
            // New note draft
            val newDraft = NoteEntity(
                id = 0,
                type = if (isChecklistInitial) "checklist" else "text"
            )
            note = newDraft
        }
    }

    // Function to trigger autosave
    val saveNoteLambda = {
        note?.let { currentNote ->
            if (!isTrashed) {
                val serializedChecklist = ChecklistSerializer.toJson(checklistItems)
                val noteToSave = currentNote.copy(
                    title = title,
                    content = content,
                    colorIndex = colorIndex,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    isTrashed = isTrashed,
                    label = labelText,
                    reminderTime = reminderTime,
                    isReminderTriggered = isReminderTriggered,
                    checklistItemsJson = serializedChecklist
                )
                viewModel.saveNote(noteToSave) { savedId ->
                    if (note?.id == 0L) {
                        note = noteToSave.copy(id = savedId)
                    }
                }
            }
        }
    }

    // Autosave when key fields change
    LaunchedEffect(title, content, colorIndex, isPinned, isArchived, labelText, reminderTime, checklistItems) {
        if (note != null) {
            saveNoteLambda()
        }
    }

    val noteBgColor = getNoteColor(colorIndex, isDark)
    val noteOnColor = getNoteOnColor(colorIndex, isDark)

    Scaffold(
        containerColor = noteBgColor,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveNoteLambda()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Save and Go Back",
                            tint = noteOnColor
                        )
                    }
                },
                actions = {
                    if (!isTrashed) {
                        // Pin action
                        IconButton(
                            onClick = { isPinned = !isPinned },
                            modifier = Modifier.testTag("edit_pin_button")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else noteOnColor
                            )
                        }

                        // Alarm / Reminder action
                        IconButton(
                            onClick = {
                                val currentCalendar = Calendar.getInstance()
                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val timePickerDialog = TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val scheduledCalendar = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                }
                                                val triggerTime = scheduledCalendar.timeInMillis
                                                reminderTime = triggerTime
                                                isReminderTriggered = false
                                                
                                                // Trigger scheduling logic inside ViewModel
                                                note?.let {
                                                    val checklistItemsJson = ChecklistSerializer.toJson(checklistItems)
                                                    val n = it.copy(
                                                        title = title,
                                                        content = content,
                                                        colorIndex = colorIndex,
                                                        checklistItemsJson = checklistItemsJson
                                                    )
                                                    viewModel.setNoteReminder(n, triggerTime)
                                                }
                                            },
                                            currentCalendar.get(Calendar.HOUR_OF_DAY),
                                            currentCalendar.get(Calendar.MINUTE),
                                            false
                                        )
                                        timePickerDialog.show()
                                    },
                                    currentCalendar.get(Calendar.YEAR),
                                    currentCalendar.get(Calendar.MONTH),
                                    currentCalendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePickerDialog.show()
                            },
                            modifier = Modifier.testTag("edit_reminder_button")
                        ) {
                            Icon(
                                imageVector = if (reminderTime != null) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsActive,
                                contentDescription = "Schedule Reminder",
                                tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else noteOnColor
                            )
                        }

                        // Archive Action
                        IconButton(
                            onClick = {
                                isArchived = !isArchived
                                if (isArchived) isPinned = false // unpin archived note
                                saveNoteLambda()
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("edit_archive_button")
                        ) {
                            Icon(
                                imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Outlined.Archive,
                                contentDescription = "Archive Note",
                                tint = noteOnColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.Transparent,
                actions = {
                    if (!isTrashed) {
                        // Color Selector Toggle
                        IconButton(
                            onClick = { showColorPicker = !showColorPicker },
                            modifier = Modifier.testTag("color_picker_button")
                        ) {
                            Icon(Icons.Outlined.Palette, contentDescription = "Choose Note Color", tint = noteOnColor)
                        }

                        // Label assigning Toggle
                        IconButton(
                            onClick = { showLabelDialog = true },
                            modifier = Modifier.testTag("label_picker_button")
                        ) {
                            Icon(Icons.Outlined.Label, contentDescription = "Assign Label", tint = noteOnColor)
                        }

                        // Move to Trash
                        IconButton(
                            onClick = {
                                if (noteId != -1L) {
                                    note?.let { viewModel.moveToTrash(it) }
                                }
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("delete_button")
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Move to Trash", tint = noteOnColor)
                        }
                    } else {
                        // Trash specific tools
                        TextButton(
                            onClick = {
                                note?.let { viewModel.restoreFromTrash(it) }
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("restore_note_button")
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        TextButton(
                            onClick = {
                                note?.let { viewModel.deleteNotePermanently(it) }
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("delete_permanent_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Forever", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Trash State Notice Banner
            if (isTrashed) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Note is in Trash. You cannot edit it unless you restore it first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Display Active Reminder Chip if configured
            if (reminderTime != null && !isTrashed) {
                val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                val reminderLabel = sdf.format(Date(reminderTime!!))
                val isExpired = reminderTime!! < System.currentTimeMillis()

                SuggestionChip(
                    onClick = {
                        // Cancel reminder
                        note?.let { viewModel.cancelNoteReminder(it) }
                        reminderTime = null
                    },
                    label = {
                        Text(
                            text = if (isReminderTriggered) "Triggered: $reminderLabel" else "Alarm: $reminderLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) noteOnColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (isReminderTriggered) Icons.Default.NotificationsNone else Icons.Default.NotificationsActive,
                            contentDescription = "Cancel Reminder",
                            tint = if (isExpired) noteOnColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("reminder_chip"),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isExpired) noteOnColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                )
            }

            // Display active Tag label chip
            if (labelText.isNotEmpty()) {
                InputChip(
                    selected = true,
                    onClick = { labelText = "" },
                    label = { Text(labelText) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.padding(bottom = 12.dp).testTag("label_chip")
                )
            }

            // Title TextField
            TextField(
                value = title,
                onValueChange = { if (!isTrashed) title = it },
                placeholder = { Text("Title", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = noteOnColor.copy(alpha = 0.5f)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = noteOnColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                readOnly = isTrashed
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-editor depending on Type: Checklist vs Text note
            if (note?.type == "checklist") {
                val uncheckedItems = checklistItems.filter { !it.isChecked }
                val checkedItems = checklistItems.filter { it.isChecked }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("checklist_items_container"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Unchecked Items list
                    items(uncheckedItems, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { isChecked ->
                                    if (!isTrashed) {
                                        checklistItems = checklistItems.map {
                                            if (it.id == item.id) it.copy(isChecked = isChecked) else it
                                        }
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            TextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    if (!isTrashed) {
                                        checklistItems = checklistItems.map {
                                            if (it.id == item.id) it.copy(text = newText) else it
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                readOnly = isTrashed
                            )
                            if (!isTrashed) {
                                IconButton(
                                    onClick = {
                                        checklistItems = checklistItems.filter { it.id != item.id }
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = noteOnColor.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }

                    // 2. Add Checklist Item quick action field
                    if (!isTrashed) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = noteOnColor.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(12.dp)
                                )
                                TextField(
                                    value = newChecklistItemText,
                                    onValueChange = { newChecklistItemText = it },
                                    placeholder = { Text("List item", color = noteOnColor.copy(alpha = 0.5f)) },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = noteOnColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("new_checklist_item_input"),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newChecklistItemText.isNotBlank()) {
                                                checklistItems = checklistItems + ChecklistItem(text = newChecklistItemText)
                                                newChecklistItemText = ""
                                            }
                                        }
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (newChecklistItemText.isNotBlank()) {
                                            checklistItems = checklistItems + ChecklistItem(text = newChecklistItemText)
                                            newChecklistItemText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // 3. Checked Completed items (with collapsible header!)
                    if (checkedItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = noteOnColor.copy(alpha = 0.1f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCheckedSectionExpanded = !isCheckedSectionExpanded }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCheckedSectionExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = noteOnColor.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${checkedItems.size} Checked items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = noteOnColor.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (isCheckedSectionExpanded) {
                            items(checkedItems, key = { it.id }) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { isChecked ->
                                            if (!isTrashed) {
                                                checklistItems = checklistItems.map {
                                                    if (it.id == item.id) it.copy(isChecked = isChecked) else it
                                                }
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = noteOnColor.copy(alpha = 0.5f),
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    if (!isTrashed) {
                                        IconButton(
                                            onClick = {
                                                checklistItems = checklistItems.filter { it.id != item.id }
                                            }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", tint = noteOnColor.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Text Note content field
                TextField(
                    value = content,
                    onValueChange = { if (!isTrashed) content = it },
                    placeholder = { Text("Note", style = MaterialTheme.typography.bodyLarge, color = noteOnColor.copy(alpha = 0.5f)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = noteOnColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("note_content_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    readOnly = isTrashed
                )
            }

            // Real-time Color Palette Bottom Picker (shows horizontally above bottom navigation bar!)
            AnimatedVisibility(
                visible = showColorPicker && !isTrashed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = noteBgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            "Background Color",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = noteOnColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NoteColorsList.forEachIndexed { index, colorTheme ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) colorTheme.darkColor else colorTheme.lightColor)
                                        .border(
                                            width = if (colorIndex == index) 2.dp else 1.dp,
                                            color = if (colorIndex == index) MaterialTheme.colorScheme.primary else noteOnColor.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable { colorIndex = index }
                                        .testTag("color_circle_$index")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic tag/label edit dialog
    if (showLabelDialog) {
        var tempLabelText by remember { mutableStateOf(labelText) }
        AlertDialog(
            onDismissRequest = { showLabelDialog = false },
            title = { Text("Set Note Label") },
            text = {
                OutlinedTextField(
                    value = tempLabelText,
                    onValueChange = { tempLabelText = it },
                    label = { Text("Tag Label (e.g., Personal, Urgent)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_label_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        labelText = tempLabelText.trim()
                        showLabelDialog = false
                    },
                    modifier = Modifier.testTag("dialog_label_confirm_button")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLabelDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
