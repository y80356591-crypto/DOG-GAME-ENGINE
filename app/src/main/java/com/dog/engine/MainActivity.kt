package com.dog.engine

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.*
import android.graphics.Color

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(25, 40, 25, 25)
        layout.setBackgroundColor(Color.rgb(15, 20, 32))

        val title = TextView(this)
        title.text = "DOG GAME ENGINE"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        layout.addView(title)

        val subtitle = TextView(this)
        subtitle.text = "3D GRAND STRATEGY GAME PLAYER"
        subtitle.setTextColor(Color.LTGRAY)
        layout.addView(subtitle)

        val importButton = Button(this)
        importButton.text = "IMPORT GAME"
        layout.addView(importButton)

        status = TextView(this)
        status.text = "Game Engine Ready"
        status.setTextColor(Color.GREEN)
        layout.addView(status)

        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, 100)
        }

        setContentView(layout)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            status.text = "Selected: ${data?.data?.lastPathSegment}"
        }
    }
}
