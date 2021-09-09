
package ui.authentication.google

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.services.it.feel.R
import com.services.it.feel.databinding.FragmentGoogleAuthenticationBinding
import constants.RequestCodes
import kotlinx.coroutines.launch
import ui.main.StartupViewModel
import utils.BaseFragment

class GoogleAuthenticationFragment : BaseFragment(R.layout.fragment_google_authentication) {

    private var binding: FragmentGoogleAuthenticationBinding? = null

    private val startupViewModel by activityViewModels<StartupViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentGoogleAuthenticationBinding.bind(view)
        this.binding = binding

        binding.signIn.setOnClickListener(::auth)
    }

    private fun auth(@Suppress("UNUSED_PARAMETER") view: View) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), options)

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RequestCodes.requestCodeSignIn)
    }

    //TODO try change onActivityResult to new API
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.requestCodeSignIn) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                Firebase.auth
                    .signInWithCredential(credential)
                    .addOnCompleteListener(onSuccess)
                    .addOnFailureListener(onError)
            } catch (e: ApiException) {
                onError.onFailure(e)
            }
        }
    }

    private val onSuccess = OnCompleteListener<AuthResult> {
        if (it.isSuccessful) {
            startupViewModel.checkWritePermission()

            lifecycleScope.launch {
                startupViewModel.signInMutableFlow.emit(true)
            }

            val bundle = Bundle()
            bundle.putBoolean("justSigned", true)
            findNavController().navigate(R.id.google_authentication_to_employees, bundle)

            try {
                Firebase.crashlytics.setUserId(Firebase.auth.currentUser?.displayName ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            onError.onFailure(Exception())
        }
    }

    private val onError = OnFailureListener { error ->
        error.printStackTrace()

        binding?.let { binding ->
            Snackbar.make(binding.root, getString(R.string.sign_in_failed), Snackbar.LENGTH_SHORT).show()
        }
    }
}