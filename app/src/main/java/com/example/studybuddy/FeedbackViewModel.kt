package com.example.studybuddy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

data class Feedback(
    val userId: String = "",
    val userEmail: String = "",
    val rating: Float = 5.0f,
    val emojiRating: String = "",
    val feedbackType: String = "",
    val message: String = "",
    val contactInfo: String = "",
    val timestamp: Date = Date(),
    val appVersion: String = "1.0"
)

class FeedbackViewModel : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _isSubmitting = MutableLiveData<Boolean>()
    val isSubmitting: LiveData<Boolean> = _isSubmitting
    
    private val _submitSuccess = MutableLiveData<Boolean>()
    val submitSuccess: LiveData<Boolean> = _submitSuccess
    
    private val _submitError = MutableLiveData<String?>()
    val submitError: LiveData<String?> = _submitError
    
    fun submitFeedback(
        rating: Float,
        emojiRating: String,
        feedbackType: String,
        message: String,
        contactInfo: String
    ) {
        if (message.trim().isEmpty()) {
            _submitError.value = "Please provide a message for your feedback."
            return
        }
        
        _isSubmitting.value = true
        
        val currentUser = auth.currentUser
        val feedback = Feedback(
            userId = currentUser?.uid ?: "anonymous",
            userEmail = currentUser?.email ?: "",
            rating = rating,
            emojiRating = emojiRating,
            feedbackType = feedbackType,
            message = message.trim(),
            contactInfo = contactInfo.trim(),
            timestamp = Date()
        )
        
        firestore.collection("feedback")
            .add(feedback)
            .addOnSuccessListener {
                _isSubmitting.value = false
                _submitSuccess.value = true
            }
            .addOnFailureListener { exception ->
                _isSubmitting.value = false
                _submitError.value = "Failed to submit feedback: ${exception.message}"
            }
    }
    
    fun resetSubmitStatus() {
        _submitSuccess.value = false
        _submitError.value = null
    }
} 