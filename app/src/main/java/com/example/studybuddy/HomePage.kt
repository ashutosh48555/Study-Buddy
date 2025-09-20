package com.example.studybuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.studybuddy.databinding.ActivityHomePageBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class HomePage : AppCompatActivity() {
    private lateinit var binding: ActivityHomePageBinding
    private lateinit var auth: FirebaseAuth
    private var selectedTabId: Int = R.id.navigation_todo

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        checkUserSession()

        // Set up logout button
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set up bottom navigation
        setupBottomNavigation()

        // Restore selected tab or set initial fragment
        if (savedInstanceState != null) {
            // Restore selected tab from saved state
            selectedTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.navigation_todo)
            binding.bottomNavigation.selectedItemId = selectedTabId
            // Don't load fragment manually - FragmentManager will restore it
        } else {
            // First time creation - load initial fragment
            selectedTabId = R.id.navigation_todo
            binding.bottomNavigation.selectedItemId = selectedTabId
            loadFragment(ToDoFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            selectedTabId = item.itemId
            when (item.itemId) {
                R.id.navigation_todo -> {
                    loadFragment(ToDoFragment())
                    true
                }
                R.id.navigation_tasks -> {
                    loadFragment(TasksFragment())
                    true
                }
                R.id.navigation_pomodoro -> {
                    loadFragment(PomodoroFragment())
                    true
                }
                R.id.navigation_analytics -> {
                    loadFragment(AnalyticsFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTabId)
    }

    private fun checkUserSession() {
        if (auth.currentUser == null) {
            // Redirect to login if not signed in
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Check if user is still logged in
        checkUserSession()
        
        // Check if permissions are still available (optional - for security)
        if (!PermissionUtils.hasAllPermissions(this)) {
            // Show a subtle reminder that some features might be limited
            // This is optional and can be removed if you don't want to nag users
        }
    }
}