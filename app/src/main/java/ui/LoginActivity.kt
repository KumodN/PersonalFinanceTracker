package com.example.personalfinancetracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val logoImage = findViewById<ImageView>(R.id.logoImage)

        btnLogin.setOnClickListener {
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ui.MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogout.setOnClickListener {
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()

            btnLogout.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
        }
    }
}
