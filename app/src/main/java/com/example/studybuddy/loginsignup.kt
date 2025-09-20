package com.example.studybuddy

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.FirebaseNetworkException
import android.util.Log
import com.example.studybuddy.NetworkConnectivityManager

class loginsignup : AppCompatActivity() {

    private lateinit var loginForm: LinearLayout
    private lateinit var signupForm: LinearLayout
    private lateinit var tvSwitchForm: TextView
    private lateinit var btnGoogle: LinearLayout
    private lateinit var btnGitHub: LinearLayout
    private lateinit var toggleLoginPassword: ImageView
    private lateinit var toggleSignupPassword: ImageView
    private lateinit var etLoginPassword: EditText
    private lateinit var etSignupPassword: EditText
    private var isLoginFormVisible = true

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var networkManager: NetworkConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginsignup)

        loginForm = findViewById(R.id.loginForm)
        signupForm = findViewById(R.id.signupForm)
        tvSwitchForm = findViewById(R.id.tvSwitchForm)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnGitHub = findViewById(R.id.btnGitHub)
        toggleLoginPassword = findViewById(R.id.ivToggleLoginPassword)
        toggleSignupPassword = findViewById(R.id.ivToggleSignupPassword)
        etLoginPassword = findViewById(R.id.etLoginPassword)
        etSignupPassword = findViewById(R.id.etSignupPassword)

        networkManager = NetworkConnectivityManager(this)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        // Listen for network changes
        networkManager.startListening { isConnected ->
            btnLogin.isEnabled = isConnected
            btnSignup.isEnabled = isConnected
            if (!isConnected) {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
            }
        }

        // Eye toggle for login password
        toggleLoginPassword.setOnClickListener {
            if (etLoginPassword.transformationMethod is PasswordTransformationMethod) {
                etLoginPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleLoginPassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etLoginPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleLoginPassword.setImageResource(R.drawable.ic_eye)
            }
            // Move cursor to end
            etLoginPassword.setSelection(etLoginPassword.text.length)
        }

        // Eye toggle for signup password
        toggleSignupPassword.setOnClickListener {
            if (etSignupPassword.transformationMethod is PasswordTransformationMethod) {
                etSignupPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                toggleSignupPassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etSignupPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleSignupPassword.setImageResource(R.drawable.ic_eye)
            }
            // Move cursor to end
            etSignupPassword.setSelection(etSignupPassword.text.length)
        }

        // Firebase setup
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        googleClient = GoogleSignIn.getClient(this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        // Login Button Click
        btnLogin.setOnClickListener {
            if (!networkManager.isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val email = findViewById<EditText>(R.id.etLoginEmail).text.toString().trim()
            val password = etLoginPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        Log.d("Auth", "Sign in successful: ${user?.uid}")
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        updateUI(user)
                    } else {
                        val exception = task.exception
                        when (exception) {
                            is FirebaseNetworkException -> {
                                Log.e("Auth", "Network error: Check internet connection")
                                Toast.makeText(this, "Network error: Check your internet connection", Toast.LENGTH_SHORT).show()
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                Log.e("Auth", "Invalid credentials")
                                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                            }
                            is FirebaseAuthInvalidUserException -> {
                                Log.e("Auth", "User not found")
                                Toast.makeText(this, "No user found with this email", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Log.e("Auth", "Authentication failed: ${exception?.message}")
                                Toast.makeText(this, exception?.localizedMessage ?: "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        }

        // Spinner setup for year
        val spinner: Spinner = findViewById(R.id.spinnerYear)
        ArrayAdapter.createFromResource(
            this,
            R.array.year_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        // Signup Button Click
        btnSignup.setOnClickListener {
            if (!networkManager.isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = findViewById<EditText>(R.id.etSignupName).text.toString().trim()
            val age = findViewById<EditText>(R.id.etSignupAge).text.toString().trim()
            val spinner: Spinner = findViewById(R.id.spinnerYear)
            val year = spinner.selectedItem?.toString() ?: ""
            val email = findViewById<EditText>(R.id.etSignupEmail).text.toString().trim()
            val password = etSignupPassword.text.toString().trim()

            if (name.isEmpty() || age.isEmpty() || year.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userProfile = hashMapOf(
                            "uid" to (user?.uid ?: ""),
                            "name" to name,
                            "age" to age,
                            "year" to year,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis()
                        )
                        user?.let {
                            firestore.collection("users").document(it.uid)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                                    updateUI(user)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        val exception = task.exception
                        val errorMsg = exception?.localizedMessage ?: "Account creation failed"
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Switch between login/signup
        tvSwitchForm.setOnClickListener {
            isLoginFormVisible = !isLoginFormVisible
            if (isLoginFormVisible) {
                loginForm.visibility = View.VISIBLE
                signupForm.visibility = View.GONE
                tvSwitchForm.text = "Don't have an account? Sign Up"
            } else {
                loginForm.visibility = View.GONE
                signupForm.visibility = View.VISIBLE
                tvSwitchForm.text = "Already have an account? Login"
            }
        }

        // Google Sign-In
        btnGoogle.setOnClickListener {
            startActivityForResult(googleClient.signInIntent, 1001)
        }

        // GitHub Sign-In
        btnGitHub.setOnClickListener {
            val provider = OAuthProvider.newBuilder("github.com")
            provider.scopes = listOf("user:email")
            val pending = auth.pendingAuthResult
            if (pending != null) {
                pending.addOnSuccessListener { updateUI(it.user) }
                    .addOnFailureListener { showError(it) }
            } else {
                auth.startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener { updateUI(it.user) }
                    .addOnFailureListener { showError(it) }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { account ->
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnSuccessListener { updateUI(it.user) }
                        .addOnFailureListener { showError(it) }
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Welcome ${user.email}", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showError(e: Exception) {
        Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkManager.stopListening()
    }
} 