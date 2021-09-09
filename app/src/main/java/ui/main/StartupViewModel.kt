package ui.main

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import constants.FirebaseConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import utils.Preference

class StartupViewModel(application: Application) : AndroidViewModel(application) {

    private val writePermissionMutableFlow = MutableSharedFlow<Boolean>()
    val writePermissionFlow = writePermissionMutableFlow.asSharedFlow()

    val signInMutableFlow = MutableSharedFlow<Boolean>()

    fun checkWritePermission() {
        viewModelScope.launch {
            val writePermission = withContext(Dispatchers.IO) {
                checkWritePermissionStart()
            }
            writePermissionMutableFlow.emit(writePermission)
        }
    }

    private suspend fun checkWritePermissionStart(): Boolean {
        try {
            val email = Firebase.auth.currentUser?.email ?: return false

            val result = Firebase.firestore
                .collection(FirebaseConstants.permissionWrite)
                .get()
                .await()

            val documents = result.documents
            for (document in documents) {
                if (document.id == email) {
                    return true
                }
            }

            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun signOut(activity: Activity) {
        Firebase.auth.signOut()
        Firebase.crashlytics.setUserId("")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        googleSignInClient.signOut()

        Preference.preferences(getApplication()).edit().clear().apply()
    }
}