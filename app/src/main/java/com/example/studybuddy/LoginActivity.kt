package com.example.studybuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.util.Patterns

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginsignup)
        
        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToHomePage()
            return
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val loginForm = findViewById<LinearLayout>(R.id.loginForm)
        val signupForm = findViewById<LinearLayout>(R.id.signupForm)
        val testLoginButton = findViewById<Button>(R.id.testLoginButton)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signupButton)
        
        // Get input fields
val loginEmail = findViewById<EditText>(R.id.emailEditText)
        val loginPassword = findViewById<EditText>(R.id.passwordEditText)
        val signupEmail = findViewById<EditText>(R.id.signupEmailEditText)
        val signupPassword = findViewById<EditText>(R.id.signupPasswordEditText)
        val signupName = findViewById<EditText>(R.id.signupNameEditText)

        // Tab switch logic
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    loginForm.visibility = View.VISIBLE
                    signupForm.visibility = View.GONE
                } else {
                    loginForm.visibility = View.GONE
                    signupForm.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        // Default: Login tab selected
        loginForm.visibility = View.VISIBLE
        signupForm.visibility = View.GONE
        tabLayout.getTabAt(0)?.select()

        // Test Login button logic (for demo purposes)
        testLoginButton.setOnClickListener {
            signInWithTestAccount()
        }

        // Real login logic
        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()
            
            if (validateLoginInput(email, password)) {
                signInUser(email, password)
            }
        }
        
        // Real signup logic
        signupButton.setOnClickListener {
            val email = signupEmail.text.toString().trim()
            val password = signupPassword.text.toString().trim()
            val name = signupName.text.toString().trim()
            
            if (validateSignupInput(email, password, name)) {
                createUser(email, password, name)
            }
        }
    }
    
    private fun validateLoginInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showToast("Please enter email")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return false
        }
        if (password.isEmpty()) {
            showToast("Please enter password")
            return false
        }
        return true
    }
    
    private fun validateSignupInput(email: String, password: String, name: String): Boolean {
        if (name.isEmpty()) {
            showToast("Please enter your name")
            return false
        }
        if (email.isEmpty()) {
            showToast("Please enter email")
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email")
            return false
        }
        if (password.isEmpty()) {
            showToast("Please enter password")
            return false
        }
        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }
        return true
    }
    
    private fun signInUser(email: String, password: String) {
        showProgressBar(true)
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgressBar(false)
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithEmail:success")
                    showToast("Login successful")
                    navigateToHomePage()
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }
    
    private fun createUser(email: String, password: String, name: String) {
        showProgressBar(true)
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.let {
                        createUserProfile(it.uid, name, email)
                    }
                } else {
                    showProgressBar(false)
                    Log.w("LoginActivity", "createUserWithEmail:failure", task.exception)
                    showToast("Account creation failed: ${task.exception?.message}")
                }
            }
    }
    
    private fun createUserProfile(uid: String, name: String, email: String) {
        val userProfile = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "year" to "",
            "branch" to "",
            "profileImageUrl" to "",
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                showProgressBar(false)
                Log.d("LoginActivity", "User profile created successfully")
                showToast("Account created successfully")
                navigateToHomePage()
            }
            .addOnFailureListener { e ->
                showProgressBar(false)
                Log.w("LoginActivity", "Error creating user profile", e)
                showToast("Error creating profile: ${e.message}")
            }
    }
    
    private fun signInWithTestAccount() {
        // Create a test account for demo purposes
        val testEmail = "test@studybuddy.com"
        val testPassword = "test123"
        
        showProgressBar(true)
        
        auth.signInWithEmailAndPassword(testEmail, testPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showProgressBar(false)
                    showToast("Test Login successful")
                    navigateToHomePage()
                } else {
                    // If test account doesn't exist, create it
                    createTestAccount(testEmail, testPassword)
                }
            }
    }
    
    private fun createTestAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        createUserProfile(it.uid, "Test User", email)
                    }
                } else {
                    showProgressBar(false)
                    showToast("Test account creation failed")
                }
            }
    }
    
    private fun navigateToHomePage() {
        startActivity(Intent(this, HomePage::class.java))
        finish()
    }
    
    private fun showProgressBar(show: Boolean) {
        // You can add a progress bar to your layout and show/hide it here
        // For now, we'll use a toast to indicate loading
        if (show) {
            showToast("Loading...")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
