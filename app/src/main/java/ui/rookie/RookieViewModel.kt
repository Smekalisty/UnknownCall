package ui.rookie

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import android.content.ContentResolver
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.ktx.firestore
import constants.FirebaseConstants
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import ui.employees.pojo.Employee

class RookieViewModel(application: Application) : AndroidViewModel(application) {

    private val photoFile by lazy { File(application.cacheDir, "photo") }

    private var mimeType : String? = null
    private var extension : String = ""

    var photoAction = StoragePhotoAction.NOTHING

    // TODO change high-order function to flow
    fun copyPhoto(uri: Uri, callback: (Result<File>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                return@withContext try {
                    copyPhoto(uri)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            callback(result)
        }
    }

    @Suppress("UNUSED")
    private fun downloadPhoto(url: String, callback: (Result<File>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (photoFile.exists()) {
                        val deleted = photoFile.delete()
                        if (!deleted) {
                            Firebase.crashlytics.log("logger log file $photoFile not deleted")
                            return@withContext Result.failure(Throwable("logger log file $photoFile not deleted"))
                        }
                    }

                    val created = photoFile.createNewFile()
                    if (!created) {
                        Firebase.crashlytics.log("logger log file $photoFile not created")
                        return@withContext Result.failure(Throwable("logger log file $photoFile not created"))
                    }

                    val storage = Firebase.storage(FirebaseConstants.bucket)
                    storage.maxUploadRetryTimeMillis = 10000
                    storage.maxOperationRetryTimeMillis = 10000
                    storage.maxDownloadRetryTimeMillis = 10000

                    storage.getReferenceFromUrl(url)
                        .getFile(photoFile)
                        .await()

                    return@withContext Result.success(photoFile)
                } catch (e: Exception) {
                    return@withContext Result.failure(e)
                }
            }

            callback(result)
        }
    }

    //TODO issue why suspend not need ?
    private fun copyPhoto(uri: Uri): Result<File> {
        val application = getApplication<Application>()

        if (photoFile.exists()) {
            val deleted = photoFile.delete()
            if (!deleted) {
                Firebase.crashlytics.log("logger log file $photoFile not deleted")
                return Result.failure(Throwable("logger log file $photoFile not deleted"))
            }
        }

        val created = photoFile.createNewFile()
        if (!created) {
            Firebase.crashlytics.log("logger log file $photoFile not created")
            return Result.failure(Throwable("logger log file $photoFile not created"))
        }

        photoFile.outputStream().use { outputStream ->
            val inputStream = application.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Firebase.crashlytics.log("logger log file $photoFile not deleted")
                return Result.failure(Throwable("logger log file $photoFile not deleted"))
            }

            inputStream.use {
                it.copyTo(outputStream)
            }
        }

        val fileInfo = getFileInfo(uri)
        mimeType = fileInfo.first
        extension = fileInfo.second ?: ""

        return Result.success(photoFile)
    }

    private fun getFileInfo(uri: Uri): Pair<String?, String?> {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val contentResolver = getApplication<Application>().contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

            Pair(mimeType, extension)
        } else {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())

            Pair(mimeType, extension)
        }
    }

    //TODO high order func to flow
    //TODO too many return@withContext may be do inline? or refactor
    fun saveEmployee(employee: Employee, callback: (Result<Employee>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                //TODO data entity for Rookie, to separate insert and select actions

                when (photoAction) {
                    StoragePhotoAction.UPLOAD -> {
                        val firstName = employee.firstName ?: return@withContext Result.failure(Throwable("First name is null"))
                        val lastName = employee.lastName ?: return@withContext Result.failure(Throwable("First name is null"))

                        val uploadResult = uploadPhoto(firstName, lastName)
                        if (uploadResult.isSuccess) {
                            employee.photo = uploadResult.getOrThrow()
                        } else {
                            return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Throwable("O_o"))
                        }
                    }

                    StoragePhotoAction.DELETE -> {
                        val deleteResult = deletePhoto(employee.photo ?: "")
                        if (deleteResult.isSuccess) {
                            employee.photo = ""
                        } else {
                            return@withContext Result.failure(deleteResult.exceptionOrNull() ?: Throwable("O_o"))
                        }
                    }
                    else -> { /* Just do nothing */ }
                }

                if (employee.documentId == null) {
                    return@withContext addRookie(employee)
                } else {
                    return@withContext updateEmployee(employee)
                }
            }

            callback(result)
        }
    }

    private suspend fun uploadPhoto(firstName: String, lastName: String): Result<String> {
        return try {
            if (!photoFile.exists() || photoFile.length() == 0L) {
                return Result.success("")
            }

            val storage = Firebase.storage(FirebaseConstants.bucket)
            storage.maxUploadRetryTimeMillis = 10000
            storage.maxOperationRetryTimeMillis = 10000
            storage.maxDownloadRetryTimeMillis = 10000

            val mainReference = storage.reference
            val employeesPhotoReference = mainReference.child(FirebaseConstants.employeesPhotosPath)

            val uri = Uri.fromFile(photoFile)
            val photoReference = employeesPhotoReference.child("${firstName}_$lastName.$extension".lowercase())

            val storageMetadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .build()

            val task = photoReference
                .putFile(uri, storageMetadata)
                .await()

            if (task.error != null) {
                return Result.failure(task.error!!)
            }

            val downloadUrl = task
                .storage
                .downloadUrl
                .await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun addRookie(rookie: Employee): Result<Employee> {
        return try {
            withTimeout(10 * 1000L) {
                Firebase.firestore
                    .collection(FirebaseConstants.employees)
                    .add(rookie)
                    .await()

                Result.success(rookie)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @ExperimentalSerializationApi
    private suspend fun updateEmployee(employee: Employee): Result<Employee> {
        return try {
            withTimeout(10 * 1000L) {
                val document = Firebase.firestore
                    .collection(FirebaseConstants.employees)
                    .document(employee.documentId!!)

                val map = Properties.encodeToMap(serializer(), employee)
                val data = map.toMutableMap()
                data.remove("documentId")

                document.update(data)
                    .await()

                Result.success(employee)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearPhoto() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                mimeType = ""
                extension = ""

                val deleted = photoFile.delete()
                if (!deleted) {
                    Firebase.crashlytics.log("logger log file $photoFile not deleted")
                }
            }
        }
    }

    fun deleteEmployee(employee: Employee, callback: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (!employee.photo.isNullOrEmpty()) {
                        deletePhoto(employee.photo!!)
                    }

                    val document = Firebase.firestore
                        .collection(FirebaseConstants.employees)
                        .document(employee.documentId!!)

                    document.delete()
                        .await()

                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            callback(result)
        }
    }

    private suspend fun deletePhoto(photo: String): Result<Unit> {
        return try {
            val storage = Firebase.storage(FirebaseConstants.bucket)
            storage.maxUploadRetryTimeMillis = 10000
            storage.maxOperationRetryTimeMillis = 10000
            storage.maxDownloadRetryTimeMillis = 10000

            storage.getReferenceFromUrl(photo)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}