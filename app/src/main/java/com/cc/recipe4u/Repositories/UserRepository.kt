package com.cc.recipe4u.Repositories

import android.net.Uri
import android.util.Log
import com.cc.recipe4u.DataClass.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class UserRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Function to initialize user document
    fun initializeUserDocument(userId: String) {
        val initialUserData = hashMapOf(
            "userId" to userId,
            "name" to "",
            "photoUrl" to "",
            "recipeIds" to emptyList<String>(),
            "favoriteRecipeIds" to emptyList<String>(),
            "ratedRecipes" to emptyMap<String, Int>()
        )

        db.collection("users")
            .document(userId)
            .set(initialUserData, SetOptions.mergeFields("userId"))
            .addOnSuccessListener {
                // Document initialization successful
                Log.d("initializeUser", "success")
            }
            .addOnFailureListener { exception ->
                // Handle failure
                Log.d("initializeUser", "failed: " + exception.message)
            }
    }

    // Function to fetch user data from Firestore
    fun fetchUser(userId: String, onSuccess: (User) -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    user?.let { onSuccess(it) }
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to update user data in Firestore
    fun updateUser(user: User, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(user.userId)
            .set(user)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to update user name in Firestore
    fun updateUserName(userId: String, newName: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(userId)
            .update("name", newName)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to update user photo in Firestore
    fun updateUserPhoto(userId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        uploadImage(
            userId,
            imageUri,
            onSuccess = { newPhotoUrl ->
                db.collection("users")
                    .document(userId)
                    .update("photoUrl", newPhotoUrl)
                    .addOnSuccessListener {
                        onSuccess(newPhotoUrl)
                    }
                    .addOnFailureListener { exception ->
                        // Handle failure
                        onFailure()
                    }
            },
            onFailure = {
                onFailure()
            }
        )
    }

    // Function to update user recipe IDs in Firestore
    fun updateUserRecipeIds(userId: String, newRecipeIds: List<String>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(userId)
            .update("recipeIds", newRecipeIds)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to update user favorite recipe IDs in Firestore
    fun updateUserFavoriteRecipeIds(userId: String, newFavoriteRecipeIds: List<String>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(userId)
            .update("favoriteRecipeIds", newFavoriteRecipeIds)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to update user rated recipes in Firestore
    fun updateUserRatedRecipes(userId: String, newRatedRecipes: Map<String, Int>, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("users")
            .document(userId)
            .update("ratedRecipes", newRatedRecipes)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Handle failure
                onFailure()
            }
    }

    // Function to upload an image to Firestore Storage
    private fun uploadImage(userId: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        val storageRef: StorageReference = storage.reference.child("user_photos/$userId/${UUID.randomUUID()}")
        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                // Image uploaded successfully, get download URL
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure()
            }
    }
}
