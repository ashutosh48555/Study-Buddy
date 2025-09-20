package com.example.studybuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import android.widget.Button

import android.content.Intent
import com.example.studybuddy.LoginActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var bottomNav: BottomNavigationView
    private var selectedTabId: Int = R.id.navigation_todo

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase Auth instance: $auth")
        setContentView(R.layout.activity_home_page)

        // Set up logout button
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            checkUserSession() // Redirect to login
        }

        // Set up bottom navigation
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        bottomNav.setOnItemSelectedListener { item ->
            selectedTabId = item.itemId
            when (item.itemId) {
                R.id.navigation_todo      -> loadFragment(ToDoFragment())
                R.id.navigation_tasks     -> loadFragment(TasksFragment())
                R.id.navigation_pomodoro  -> loadFragment(PomodoroFragment())
                R.id.navigation_analytics -> loadFragment(AnalyticsFragment())
                R.id.navigation_profile   -> loadFragment(ProfileFragment())
                else -> false
            }
            true
        }

        // Restore selected tab or set initial fragment
        if (savedInstanceState != null) {
            // Restore selected tab from saved state
            selectedTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.navigation_todo)
            bottomNav.selectedItemId = selectedTabId
            // Don't load fragment manually - FragmentManager will restore it
        } else {
            // First time creation - load initial fragment
            selectedTabId = R.id.navigation_todo
            bottomNav.selectedItemId = selectedTabId
            loadFragment(ToDoFragment())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTabId)
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            if (addToBackStack) addToBackStack(null)
            commit()
        }
    }

    private fun checkUserSession() {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}