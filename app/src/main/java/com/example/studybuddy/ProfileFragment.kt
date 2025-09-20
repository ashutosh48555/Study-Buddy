package com.example.studybuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.result.ActivityResultLauncher
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.RatingBar
import android.widget.Spinner
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.util.Log
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import android.graphics.PorterDuff
import android.widget.LinearLayout
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import android.content.pm.PackageManager
import java.io.File

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()
    private val feedbackViewModel: FeedbackViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    private lateinit var profileImageView: CircleImageView
    private lateinit var uploadImageButton: Button
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var branchTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var goalsTextView: TextView
    private lateinit var achievementsTextView: TextView
    private lateinit var setGoalsButton: Button
    private lateinit var feedbackButton: Button
    private var selectedEmojiRating: String = ""
    private var loadingDialog: AlertDialog? = null
    private var successDialog: AlertDialog? = null
    private var selectedRating = 5 // Default to 5 stars

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = it.data?.data
            if (imageUri != null) {
                Log.d("ProfileFragment", "Image selected: $imageUri")
                
                // Validate image URI
                if (validateImageUri(imageUri)) {
                    // Show loading indicator
                    uploadImageButton.isEnabled = false
                    uploadImageButton.text = "Uploading..."
                    
                    // Start upload
                    profileViewModel.uploadProfileImage(imageUri, requireContext())
                } else {
                    Log.e("ProfileFragment", "Invalid image URI")
                    Toast.makeText(requireContext(), "Selected image is not accessible. Please try another image.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("ProfileFragment", "No image URI received from picker")
                Toast.makeText(requireContext(), "Failed to select image. Please try again.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("ProfileFragment", "Image picker cancelled or failed")
        }
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is needed for profile pictures", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchGallery()
        } else {
            Toast.makeText(requireContext(), "Storage permission is needed to select images", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        profileImageView = view.findViewById(R.id.profileImageView)
        uploadImageButton = view.findViewById(R.id.uploadImageButton)
        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        yearTextView = view.findViewById(R.id.yearTextView)
        branchTextView = view.findViewById(R.id.branchTextView)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        goalsTextView = view.findViewById(R.id.goalsTextView)
        achievementsTextView = view.findViewById(R.id.achievementsTextView)
        setGoalsButton = view.findViewById(R.id.setGoalsButton)
        feedbackButton = view.findViewById(R.id.feedbackButton)

        profileViewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            if (userProfile != null) {
                // Display user data from Firebase
                nameTextView.text = "Name: ${userProfile.name}"
                emailTextView.text = "Email: ${userProfile.email}"
                yearTextView.text = "Academic Year: ${userProfile.year}"
                branchTextView.text = "Branch: ${userProfile.branch}"
                
                if (userProfile.profileImageUrl.isNotEmpty()) {
                    // Handle both local files and URLs
                    if (userProfile.profileImageUrl.startsWith("/")) {
                        // Local file path
                        val file = File(userProfile.profileImageUrl)
                        if (file.exists()) {
                            Glide.with(this)
                                .load(file)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } else {
                        // URL (Firebase or other)
                        Glide.with(this)
                            .load(userProfile.profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileImageView)
                    }
                } else {
                    // Set default placeholder image
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                // Show loading state while fetching data
                nameTextView.text = "Loading..."
                emailTextView.text = "Loading..."
                yearTextView.text = "Loading..."
                branchTextView.text = "Loading..."
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        profileViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            // Update button text with progress
            uploadImageButton.text = "Uploading... ${progress.toInt()}%"
            Log.d("ProfileFragment", "Upload progress: ${progress.toInt()}%")
        }

        profileViewModel.uploadSuccess.observe(viewLifecycleOwner) { isSuccess ->
            // Reset button state
            uploadImageButton.isEnabled = true
            uploadImageButton.text = "Upload Image"
            
            if (isSuccess) {
                Toast.makeText(requireContext(), "Profile image uploaded successfully!", Toast.LENGTH_SHORT).show()
                Log.d("ProfileFragment", "Image upload completed successfully")
            } else {
                Log.w("ProfileFragment", "Image upload failed")
                // Error will be shown by the error observer
            }
        }

        // Observe loading state
        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Show loading indicators
                nameTextView.text = "Loading..."
                emailTextView.text = "Loading..."
                yearTextView.text = "Loading..."
                branchTextView.text = "Loading..."
            }
        }

        // Observe error state
        profileViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Reset upload button state on error
                uploadImageButton.isEnabled = true
                uploadImageButton.text = "Upload Image"
                
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                Log.e("ProfileFragment", "Profile error: $it")
                
                // Show default values on error only for profile data errors
                if (it.contains("profile") || it.contains("load")) {
                    nameTextView.text = "Name: Not Available"
                    emailTextView.text = "Email: Not Available"
                    yearTextView.text = "Academic Year: Not Available"
                    branchTextView.text = "Branch: Not Available"
                }
            }
        }

        uploadImageButton.setOnClickListener { 
            openImageChooser()
        }

        editProfileButton.setOnClickListener { 
            showEditProfileDialog()
        }

        setGoalsButton.setOnClickListener { 
            showSetGoalsDialog()
        }

        feedbackButton.setOnClickListener { 
            // Add haptic feedback
            performHapticFeedback()
            
            // Add button press animation
            val scaleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in)
            feedbackButton.startAnimation(scaleAnimation)
            
            // Show feedback dialog after animation
            feedbackButton.postDelayed({
                showFeedbackDialog()
            }, 150)
        }

        // Observe feedback submission status
        feedbackViewModel.isSubmitting.observe(viewLifecycleOwner) { isSubmitting ->
            if (isSubmitting) {
                showLoadingDialog()
            } else {
                hideLoadingDialog()
            }
        }

        feedbackViewModel.submitSuccess.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                showSuccessDialog()
                feedbackViewModel.resetSubmitStatus()
            }
        }

        feedbackViewModel.submitError.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                feedbackViewModel.resetSubmitStatus()
            }
        }

        profileViewModel.fetchUserProfile()

        // Load saved goal
        val prefs = requireContext().getSharedPreferences("studybuddy_prefs", Context.MODE_PRIVATE)
        val savedGoal = prefs.getString("user_goal", "")
        goalsTextView.text = savedGoal
    }

    private fun openImageChooser() {
        // Show dialog to choose between Camera and Gallery
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkStoragePermission()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (PermissionUtils.hasCameraPermission(requireContext())) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun checkStoragePermission() {
        if (PermissionUtils.hasStoragePermission(requireContext())) {
            launchGallery()
        } else {
            val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            pickImage.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        pickImage.launch(intent)
    }
    
    private fun validateImageUri(uri: Uri): Boolean {
        return try {
            // Check if URI is accessible
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.close()
            true
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error validating image URI", e)
            false
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1001 
        const val REQUEST_CAMERA = 1002
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.editNameEditText)
        val emailEditText = dialogView.findViewById<EditText>(R.id.editEmailEditText)
        val yearEditText = dialogView.findViewById<EditText>(R.id.editYearEditText)
        val branchEditText = dialogView.findViewById<EditText>(R.id.editBranchEditText)

        // Pre-fill with current user data
        profileViewModel.userProfile.value?.let { profile ->
            nameEditText.setText(profile.name)
            emailEditText.setText(profile.email)
            yearEditText.setText(profile.year)
            branchEditText.setText(profile.branch)
        } ?: run {
            // If no profile data, use Firebase Auth user data
            auth.currentUser?.let { user ->
                nameEditText.setText(user.displayName ?: "")
                emailEditText.setText(user.email ?: "")
                yearEditText.setText("First Year")
                branchEditText.setText("Computer Science")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val name = nameEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val year = yearEditText.text.toString().trim()
                val branch = branchEditText.text.toString().trim()
                
                if (name.isNotEmpty() && email.isNotEmpty()) {
                    profileViewModel.updateUserProfile(name, email, year, branch)
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Name and email are required", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showSetGoalsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_goals, null)
        val goalDescriptionEditText = dialogView.findViewById<EditText>(R.id.goalDescriptionEditText)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Study Goal")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val goal = goalDescriptionEditText.text.toString()
                val prefs = requireContext().getSharedPreferences("studybuddy_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("user_goal", goal).apply()
                goalsTextView.text = goal
                Toast.makeText(requireContext(), "Goal saved!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun showFeedbackDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_feedback_professional, null)

        // --- Star rating logic ---
        val starContainer = dialogView.findViewById<LinearLayout>(R.id.starContainer)
        val starViews = mutableListOf<ImageView>()
        for (i in 1..5) {
            val star = ImageView(requireContext())
            val size = resources.getDimensionPixelSize(R.dimen.star_size)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(8, 0, 8, 0)
            star.layoutParams = params
            star.setImageResource(R.drawable.ic_star)
            star.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.starGold),
                PorterDuff.Mode.SRC_IN
            )
            star.isClickable = true
            star.isFocusable = true
            star.setOnClickListener {
                selectedRating = i
                updateStars(starViews, selectedRating)
            }
            starViews.add(star)
            starContainer.addView(star)
        }
        updateStars(starViews, selectedRating)

        // --- Feedback type dropdown ---
        val feedbackTypeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.feedbackTypeSpinner)
        val feedbackTypes = resources.getStringArray(R.array.feedback_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, feedbackTypes)
        feedbackTypeSpinner.setAdapter(adapter)
        feedbackTypeSpinner.threshold = 1
        feedbackTypeSpinner.setOnClickListener {
            feedbackTypeSpinner.showDropDown()
        }
        feedbackTypeSpinner.setOnTouchListener { v, event ->
            feedbackTypeSpinner.showDropDown()
            false
        }
        feedbackTypeSpinner.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) feedbackTypeSpinner.showDropDown()
        }

        // --- Inputs ---
        val feedbackMessageEditText = dialogView.findViewById<TextInputEditText>(R.id.feedbackMessageEditText)
        val contactInfoEditText = dialogView.findViewById<TextInputEditText>(R.id.contactInfoEditText)

        // --- Remove any background (force no gradient) ---
        feedbackMessageEditText.background = null
        contactInfoEditText.background = null
        feedbackTypeSpinner.background = null

        // --- Buttons ---
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<MaterialButton>(R.id.submitButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        cancelButton.setOnClickListener { dialog.dismiss() }
        submitButton.setOnClickListener {
            val feedbackType = feedbackTypeSpinner.text.toString()
            val message = feedbackMessageEditText.text?.toString() ?: ""
            val contactInfo = contactInfoEditText.text?.toString() ?: ""

            // Validate and submit feedback here
            if (message.isBlank()) {
                feedbackMessageEditText.error = "Please enter your feedback"
                return@setOnClickListener
            }

            // For testing: show the animated success dialog
            dialog.dismiss()
            showSuccessDialog()
        }

        dialog.show()

        // Add item click listener to feedbackTypeSpinner
        feedbackTypeSpinner.setOnItemClickListener { parent, view, position, id ->
            feedbackTypeSpinner.hint = ""
        }
    }

    private fun updateStars(starViews: List<ImageView>, rating: Int) {
        for (i in starViews.indices) {
            if (i < rating) {
                starViews[i].setColorFilter(
                    ContextCompat.getColor(starViews[i].context, R.color.starGold),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                starViews[i].setColorFilter(
                    ContextCompat.getColor(starViews[i].context, R.color.starUnselected),
                    PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    private fun showLoadingDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback_loading, null)
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_feedback_success, null)
        
        val rateAppButton = dialogView.findViewById<Button>(R.id.rateAppButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        successDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        rateAppButton.setOnClickListener {
            openPlayStore()
            successDialog?.dismiss()
        }

        closeButton.setOnClickListener {
            successDialog?.dismiss()
        }

        successDialog?.show()
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}"))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}"))
            startActivity(intent)
        }
    }

    private fun performHapticFeedback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
        }
    }

    // --- DIALOG UTILITIES ---
    fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showSuccessDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
