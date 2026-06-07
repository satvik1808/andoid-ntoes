package com.example.infinitenotes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.infinitenotes.ui.filemanager.FileManagerScreen
import com.example.infinitenotes.ui.main.NoteEditorScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(FileManager)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<FileManager> {
          FileManagerScreen(
              onNoteClick = { fileName -> backStack.add(NoteEditor(fileName)) },
              modifier = Modifier.fillMaxSize()
          )
        }
        entry<NoteEditor> {
          NoteEditorScreen(
              fileName = it.fileName,
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
