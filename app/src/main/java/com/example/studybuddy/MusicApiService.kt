package com.example.studybuddy

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicApiService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseUrl = "https://your-railway-app.railway.app/api"
    
    companion object {
        private const val TAG = "MusicApiService"
    }
    
    data class ApiResponse<T>(
        val success: Boolean,
        val count: Int? = null,
        val songs: List<T>? = null,
        val song: T? = null,
        val message: String? = null,
        val error: String? = null
    )
    
    suspend fun fetchSongs(): List<LofiTrack> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/songs")
                    .build()
                
                val response: Response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    response.close()
                    
                    if (responseBody != null) {
                        val apiResponse = gson.fromJson<ApiResponse<LofiTrack>>(
                            responseBody,
                            object : TypeToken<ApiResponse<LofiTrack>>() {}.type
                        )
                        
                        if (apiResponse.success && apiResponse.songs != null) {
                            Log.d(TAG, "Successfully fetched ${apiResponse.count} songs")
                            return@withContext apiResponse.songs
                        } else {
                            Log.e(TAG, "API error: ${apiResponse.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "HTTP error: ${response.code}")
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ", e)
            }
            
            // Return fallback tracks if API fails
            return@withContext getFallbackTracks()
        }
    }
    
    suspend fun fetchSongById(id: Int): LofiTrack? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/songs/$id")
                    .build()
                
                val response: Response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    response.close()
                    
                    if (responseBody != null) {
                        val apiResponse = gson.fromJson<ApiResponse<LofiTrack>>(
                            responseBody,
                            object : TypeToken<ApiResponse<LofiTrack>>() {}.type
                        )
                        
                        if (apiResponse.success && apiResponse.song != null) {
                            return@withContext apiResponse.song
                        }
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching song by ID: ", e)
            }
            
            return@withContext null
        }
    }
    
    suspend fun searchSongs(query: String, genre: String? = null): List<LofiTrack> {
        return withContext(Dispatchers.IO) {
            try {
                var url = "$baseUrl/search?q=$query"
                if (genre != null) {
                    url += "&genre=$genre"
                }
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response: Response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    response.close()
                    
                    if (responseBody != null) {
                        val apiResponse = gson.fromJson<ApiResponse<LofiTrack>>(
                            responseBody,
                            object : TypeToken<ApiResponse<LofiTrack>>() {}.type
                        )
                        
                        if (apiResponse.success && apiResponse.songs != null) {
                            return@withContext apiResponse.songs
                        }
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search error: ", e)
            }
            
            return@withContext emptyList()
        }
    }
    
    private fun getFallbackTracks(): List<LofiTrack> {
        return listOf(
            LofiTrack(
                title = "Offline Mode",
                artist = "StudyBuddy",
                url = "",
                coverArtUrl = "https://picsum.photos/200",
                duration = "0:00"
            )
        )
    }
    
    fun updateBaseUrl(newUrl: String) {
        // Allow dynamic URL updates for testing
        // baseUrl = newUrl
    }
}
