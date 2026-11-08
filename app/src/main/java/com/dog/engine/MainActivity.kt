package com.dog.engine

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var libraryText: TextView
    private lateinit var playButton: Button
    private var selectedGame: File? = null

    private val background = Color.rgb(12, 18, 31)
    private val panel = Color.rgb(23, 33, 52)
    private val accent = Color.rgb(45, 190, 145)
    private val white = Color.WHITE
    private val muted = Color.rgb(170, 184, 202)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedGame = savedInstanceState?.getString("selected_game")?.let { File(it) }
            ?.takeIf { it.exists() }
        showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun makeText(value: String, size: Float, color: Int): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
        }
    }

    private fun makeButton(value: String): Button {
        return Button(this).apply {
            text = value
            isAllCaps = false
            textSize = 16f
        }
    }

    private fun showHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(background)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(makeText("DOG GAME ENGINE", 27f, white))
        content.addView(makeText("MOBILE GAME LIBRARY  •  VERSION 1.0", 12f, accent).apply {
            setPadding(0, dp(5), 0, dp(22))
        })

        val intro = makeText(
            "Import your own .moto game files and manage them in one place.",
            15f,
            muted
        )
        intro.setPadding(dp(15), dp(16), dp(15), dp(16))
        intro.setBackgroundColor(panel)
        content.addView(intro)

        val importButton = makeButton("＋   IMPORT .MOTO GAME")
        val importParams = LinearLayout.LayoutParams(-1, dp(56))
        importParams.topMargin = dp(22)
        content.addView(importButton, importParams)
        importButton.setOnClickListener { openGamePicker() }

        content.addView(makeText("GAME LIBRARY", 17f, white).apply {
            setPadding(0, dp(28), 0, dp(12))
        })

        libraryText = makeText("No games imported yet.", 14f, muted)
        libraryText.setPadding(dp(15), dp(18), dp(15), dp(18))
        libraryText.setBackgroundColor(panel)
        content.addView(libraryText)

        playButton = makeButton("PLAY GAME")
        playButton.isEnabled = selectedGame?.exists() == true
        val playParams = LinearLayout.LayoutParams(-1, dp(54))
        playParams.topMargin = dp(16)
        content.addView(playButton, playParams)
        playButton.setOnClickListener { showGameScreen() }

        statusText = makeText("ENGINE READY  •  .MOTO IMPORT ENABLED", 12f, accent)
        statusText.setPadding(0, dp(24), 0, 0)
        content.addView(statusText)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshLibrary()
    }

    private fun openGamePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, REQUEST_GAME)
        } catch (e: Exception) {
            Toast.makeText(this, "File picker could not be opened", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated by Android; retained for compatibility with the current project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_GAME || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val name = getDisplayName(uri) ?: "game.moto"
        if (!name.lowercase(Locale.ROOT).endsWith(".moto")) {
            Toast.makeText(this, "Please select a .moto file", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val folder = File(filesDir, "games").apply { mkdirs() }
            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destination = uniqueFile(folder, safeName)
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Cannot read selected file")
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            selectedGame = destination
            statusText.text = "IMPORTED: ${destination.name}"
            refreshLibrary()
            Toast.makeText(this, "Game imported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else null
        } finally {
            cursor?.close()
        }
    }

    private fun uniqueFile(folder: File, name: String): File {
        val base = name.removeSuffix(".moto")
        var candidate = File(folder, name)
        var number = 1
        while (candidate.exists()) {
            candidate = File(folder, "${base}_$number.moto")
            number++
        }
        return candidate
    }

    private fun refreshLibrary() {
        val game = selectedGame?.takeIf { it.exists() }
        if (::libraryText.isInitialized) {
            libraryText.text = if (game == null) {
                "No games imported yet.\nTap IMPORT .MOTO GAME to add one."
            } else {
                "${game.name}\n${formatSize(game.length())}  •  Stored on this device"
            }
        }
        if (::playButton.isInitialized) playButton.isEnabled = game != null
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    private fun showGameScreen() {
        val game = selectedGame?.takeIf { it.exists() } ?: run {
            refreshLibrary()
            return
        }
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(background)
        }
        screen.addView(makeText("DOG GAME PLAYER", 25f, white))
        screen.addView(makeText(game.name, 15f, accent).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(18))
        })
        val message = makeText(
            "Your .moto file is imported.\n\nThe DOG .moto runtime is the next development step. This first APK manages the game file but cannot run its game content yet.",
            15f,
            muted
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(20), dp(16), dp(20))
            setBackgroundColor(panel)
        }
        screen.addView(message)
        val back = makeButton("BACK TO LIBRARY")
        val params = LinearLayout.LayoutParams(-1, dp(54))
        params.topMargin = dp(22)
        screen.addView(back, params)
        back.setOnClickListener { showHome() }
        setContentView(screen)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedGame?.let { outState.putString("selected_game", it.absolutePath) }
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_GAME = 100
    }
}
