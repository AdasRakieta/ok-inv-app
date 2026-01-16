package com.example.inventoryapp.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Immediately forward to MainActivity; background shows splash via theme
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
