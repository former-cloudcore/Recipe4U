package com.cc.recipe4u.ViewModels

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cc.recipe4u.DB.RecipeDatabase
import com.cc.recipe4u.Dao.RecipeDao
import com.cc.recipe4u.DataClass.Recipe
import com.cc.recipe4u.Models.FirestoreModel
import com.cc.recipe4u.Objects.RecipeLocalTime
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {
    private lateinit var context: Context
    private lateinit var recipeDao: RecipeDao
    private val _allRecipes: MutableLiveData<List<Recipe>> = MutableLiveData()
    private val allRecipes: LiveData<List<Recipe>> = _allRecipes
    private var removedDeletedRecipes = false

    fun setContextAndDB(context: Context) {
        this.context = context
        recipeDao = RecipeDatabase.db(context).recipeDao()
        val allRecipes = recipeDao.getAll()
        allRecipes.observeForever { recipes ->
            _allRecipes.postValue(recipes)
            if (!removedDeletedRecipes && recipes.isNotEmpty()) {
                removedDeletedRecipes = true
                removeDeletedRecipes(recipes)
            }
        }
    }
    fun getAllRecipes(): LiveData<List<Recipe>> {
        val localLastUpdated = RecipeLocalTime.getLocalLastUpdated(context)
        FirestoreModel.getAllRecipes(localLastUpdated) { recipes ->
            var lastUpdated = 0L
            for (recipe in recipes) {
                CoroutineScope(Dispatchers.IO).launch {
                    recipeDao.insert(recipe)
                }
                if (lastUpdated < recipe.lastUpdated) {
                    lastUpdated = recipe.lastUpdated
                }
            }

            RecipeLocalTime.setLocalLastUpdated(context, lastUpdated)
        }
        return allRecipes
    }

    private fun removeDeletedRecipes(recipes: List<Recipe>) {
        FirestoreModel.checkForDeletedRecipes(recipes.map { it.recipeId }) { deletedRecipeIds ->
            CoroutineScope(Dispatchers.IO).launch {
                for (deletedRecipeId in deletedRecipeIds) {
                    recipeDao.deleteById(deletedRecipeId)
                }
            }
        }
    }

    fun getByCategory(category: String): LiveData<List<Recipe>> {
        return recipeDao.getByCategory(category)
    }

    fun getByOwner(ownerId: String): LiveData<List<Recipe>> {
        return recipeDao.getByOwner(ownerId)
    }

    fun createRecipe(recipe: Recipe, listener: (Recipe) -> Unit) {
        val newRecipe = recipe.copy()
        if (newRecipe.imageUri == "null") {
            createRecipeWithoutUploadingImage(newRecipe, listener)
        } else {
            FirestoreModel.uploadImage(newRecipe.imageUri.toUri(), onSuccess = { imageUri ->
                newRecipe.imageUri = imageUri
                createRecipeWithoutUploadingImage(newRecipe, listener)
            }, onFailure = {
                // Handle failure
            })
        }
    }

    fun deleteRecipe(recipeId: String, onSuccess: () -> Unit) {
        FirestoreModel.deleteRecipe(recipeId) {
            CoroutineScope(Dispatchers.IO).launch {
                recipeDao.deleteById(recipeId)
                onSuccess()
            }
        }
    }
    fun removeRating(recipe: Recipe, rating: Float, onSuccess: (Recipe) -> Unit) {
        val newRecipe = recipe.copy()
        if (newRecipe.numberOfRatings == 1) {
            newRecipe.rating = 0.0f
        } else {
            newRecipe.rating = ((newRecipe.rating * newRecipe.numberOfRatings) - rating) / (newRecipe.numberOfRatings - 1)
        }
        newRecipe.numberOfRatings -= 1
        newRecipe.lastUpdated = System.currentTimeMillis()
        FirestoreModel.updateRecipe(newRecipe) {
            CoroutineScope(Dispatchers.IO).launch {
                recipeDao.insert(newRecipe)
                onSuccess(newRecipe)
            }
        }
    }

    fun addRating(recipe: Recipe, rating: Float, onSuccess: (Recipe) -> Unit) {
        val newRecipe = recipe.copy()
        val newRating = ((newRecipe.rating * newRecipe.numberOfRatings) + rating) / (newRecipe.numberOfRatings + 1)
        newRecipe.rating = newRating
        newRecipe.numberOfRatings += 1
        newRecipe.lastUpdated = System.currentTimeMillis()
        FirestoreModel.updateRecipe(newRecipe) {
            CoroutineScope(Dispatchers.IO).launch {
                recipeDao.insert(newRecipe)
                onSuccess(newRecipe)
            }
        }
    }
    private fun createRecipeWithoutUploadingImage(recipe: Recipe, listener: (Recipe) -> Unit) {
        FirestoreModel.createRecipe(recipe){recipeWithId ->
            CoroutineScope(Dispatchers.IO).launch {
                recipeDao.insert(recipeWithId)
                listener(recipeWithId)
            }
        }
    }
}

