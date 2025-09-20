package com.example.studybuddy

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile

    private val _uploadProgress = MutableLiveData<Double>()
    val uploadProgress: LiveData<Double> = _uploadProgress

    private val _uploadSuccess = MutableLiveData<Boolean>()
    val uploadSuccess: LiveData<Boolean> = _uploadSuccess
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun fetchUserProfile() {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                _isLoading.value = false
                if (document.exists()) {
                    try {
                        val profile = document.toObject(UserProfile::class.java)
                        _userProfile.value = profile
                        Log.d("ProfileViewModel", "User profile fetched successfully")
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Error parsing user profile", e)
                        _error.value = "Error loading profile: ${e.message}"
                    }
                } else {
                    Log.d("ProfileViewModel", "Profile not found, creating default profile")
                    createDefaultProfile(currentUserId)
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.w("ProfileViewModel", "Error fetching user profile", e)
                _error.value = "Failed to load profile: ${e.message}"
            }
    }

    private fun createDefaultProfile(userId: String) {
        val currentUser = auth.currentUser
        val defaultProfile = UserProfile(
            uid = userId,
            name = currentUser?.displayName ?: "User",
            email = currentUser?.email ?: "",
            year = "First Year",
            branch = "Computer Science",
            profileImageUrl = ""
        )
        
        firestore.collection("users").document(userId)
            .set(defaultProfile)
            .addOnSuccessListener {
                _userProfile.value = defaultProfile
                Log.d("ProfileViewModel", "Default profile created successfully")
            }
            .addOnFailureListener { e ->
                Log.w("ProfileViewModel", "Error creating default profile", e)
                _error.value = "Failed to create profile: ${e.message}"
            }
    }

    fun updateUserProfile(name: String, email: String, year: String, branch: String) {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        val updatedProfile = UserProfile(
            uid = currentUserId,
            name = name,
            email = email,
            year = year,
            branch = branch,
            profileImageUrl = _userProfile.value?.profileImageUrl ?: ""
        )
        
        firestore.collection("users").document(currentUserId)
            .set(updatedProfile)
            .addOnSuccessListener {
                _isLoading.value = false
                _userProfile.value = updatedProfile
                Log.d("ProfileViewModel", "Profile updated successfully")
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.w("ProfileViewModel", "Error updating profile", e)
                _error.value = "Failed to update profile: ${e.message}"
            }
    }

    fun uploadProfileImage(imageUri: Uri, context: Context) {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            Log.e("ProfileViewModel", "User not authenticated for image upload")
            return
        }
        
        // Validate image URI
        if (imageUri == Uri.EMPTY) {
            _error.value = "Invalid image selected"
            Log.e("ProfileViewModel", "Empty image URI provided")
            return
        }
        
        // Check if user is authenticated with Firebase
        if (auth.currentUser?.isEmailVerified == false && auth.currentUser?.email != null) {
            Log.w("ProfileViewModel", "User email not verified, but proceeding with upload")
        }
        
        _uploadProgress.value = 0.0
        _uploadSuccess.value = false
        _error.value = null
        
        Log.d("ProfileViewModel", "Starting image upload for user: $currentUserId")
        Log.d("ProfileViewModel", "Image URI: $imageUri")
        
        try {
            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val fileName = "profile_images/${currentUserId}_${timestamp}_${UUID.randomUUID()}.jpg"
            val imageRef: StorageReference = storageRef.child(fileName)
            
            Log.d("ProfileViewModel", "Uploading to path: $fileName")
            
            val uploadTask = imageRef.putFile(imageUri)
            
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                _uploadProgress.value = progress
                Log.d("ProfileViewModel", "Upload progress: ${progress.toInt()}%")
            }.addOnSuccessListener { taskSnapshot ->
                Log.d("ProfileViewModel", "Image uploaded successfully to Firebase Storage")
                Log.d("ProfileViewModel", "Bytes transferred: ${taskSnapshot.bytesTransferred}")
                
                // Get download URL
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("ProfileViewModel", "Download URL obtained: $downloadUri")
                    updateProfileImageUrl(downloadUri.toString())
                }.addOnFailureListener { e ->
                    Log.e("ProfileViewModel", "Error getting download URL", e)
                    _uploadSuccess.value = false
                    _error.value = "Failed to get image URL: ${e.message}"
                    // Try to get more specific error information
                    when (e) {
                        is com.google.firebase.storage.StorageException -> {
                            when (e.errorCode) {
                                com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND -> {
                                    _error.value = "Image not found after upload. Please try again."
                                }
                                com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED -> {
                                    _error.value = "Upload unauthorized. Please check your account."
                                }
                                com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED -> {
                                    _error.value = "Storage quota exceeded. Please contact support."
                                }
                                else -> {
                                    _error.value = "Storage error: ${e.message}"
                                }
                            }
                        }
                        else -> {
                            _error.value = "Network error: ${e.message}"
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Error uploading image to Firebase Storage", e)
                _uploadSuccess.value = false
                
                // Provide more specific error messages
                when (e) {
                    is com.google.firebase.storage.StorageException -> {
                        when (e.errorCode) {
                            com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED -> {
                                _error.value = "Upload unauthorized. Please check your account and try again."
                            }
                            com.google.firebase.storage.StorageException.ERROR_QUOTA_EXCEEDED -> {
                                _error.value = "Storage quota exceeded. Please contact support."
                            }
                            com.google.firebase.storage.StorageException.ERROR_INVALID_CHECKSUM -> {
                                _error.value = "Image file corrupted. Please select a different image."
                            }
                            com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                                _error.value = "Upload failed due to network issues. Please check your connection and try again."
                            }
                            else -> {
                                _error.value = "Storage error: ${e.message}"
                            }
                        }
                    }
                    is java.net.SocketTimeoutException -> {
                        _error.value = "Upload timed out. Please check your internet connection and try again."
                    }
                    is java.net.UnknownHostException -> {
                        _error.value = "No internet connection. Please check your network and try again."
                    }
                    else -> {
                        _error.value = "Upload failed: ${e.message}"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Exception during upload setup", e)
            _uploadSuccess.value = false
            _error.value = "Failed to start upload: ${e.message}"
        }
    }
    
    private fun updateProfileImageUrl(imageUrl: String) {
        val currentUserId = auth.currentUser?.uid
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        firestore.collection("users").document(currentUserId)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener {
                val updatedProfile = _userProfile.value?.copy(profileImageUrl = imageUrl)
                _userProfile.value = updatedProfile
                _uploadSuccess.value = true
                Log.d("ProfileViewModel", "Profile image URL updated successfully")
            }
            .addOnFailureListener { e ->
                Log.w("ProfileViewModel", "Error updating profile image URL", e)
                _uploadSuccess.value = false
                _error.value = "Failed to update profile image: ${e.message}"
            }
    }
    
    fun deleteProfileImage() {
        val currentUserId = auth.currentUser?.uid
        val currentImageUrl = _userProfile.value?.profileImageUrl
        
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }
        
        if (currentImageUrl.isNullOrEmpty()) {
            _error.value = "No profile image to delete"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        // Delete from Firebase Storage
        val imageRef = storage.getReferenceFromUrl(currentImageUrl)
        imageRef.delete().addOnSuccessListener {
            // Remove URL from Firestore
            firestore.collection("users").document(currentUserId)
                .update("profileImageUrl", "")
                .addOnSuccessListener {
                    _isLoading.value = false
                    val updatedProfile = _userProfile.value?.copy(profileImageUrl = "")
                    _userProfile.value = updatedProfile
                    Log.d("ProfileViewModel", "Profile image deleted successfully")
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    Log.w("ProfileViewModel", "Error removing image URL from profile", e)
                    _error.value = "Failed to update profile: ${e.message}"
                }
        }.addOnFailureListener { e ->
            _isLoading.value = false
            Log.w("ProfileViewModel", "Error deleting image from storage", e)
            _error.value = "Failed to delete image: ${e.message}"
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun resetUploadState() {
        _uploadProgress.value = 0.0
        _uploadSuccess.value = false
    }
}
