package com.tecsup.nexusmobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.User
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun register(
        email: String,
        password: String,
        username: String,
        fullName: String
    ): Result<User> {
        return try {
            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario")

            // Crear documento en Firestore
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                fullName = fullName,
                role = "USER",
                active = true
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Autenticar con Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Usuario no encontrado")

            // Obtener datos del usuario desde Firestore
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)?.copy(id = userDoc.id)
                ?: throw Exception("Datos de usuario no encontrados")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            username = "",
            fullName = ""
        )
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}