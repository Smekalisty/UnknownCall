package ui.welcome

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.services.it.feel.R
import utils.BaseFragment

class WelcomeFragment : BaseFragment(R.layout.fragment_welcome) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val next = view.findViewById<View>(R.id.next)
        next.setOnClickListener(::onClickNext)
    }

    private fun onClickNext(@Suppress("UNUSED_PARAMETER") view: View) {
        findNavController().navigate(R.id.welcome_to_authenticate)
    }
}