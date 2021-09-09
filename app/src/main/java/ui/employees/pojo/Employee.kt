package ui.employees.pojo

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Employee(
    @DocumentId
    val documentId: String? = null,

    val firstName: String? = null,

    val lastName: String? = null,

    val corporateEmail: String? = null,

    val phoneNumber: String? = null,

    val skype: String? = null,

    val specialization: String? = null,

    val city: String? = null,

    val remote: Boolean? = null,

    val ex: Boolean? = null,

    var photo: String? = null
) : Parcelable