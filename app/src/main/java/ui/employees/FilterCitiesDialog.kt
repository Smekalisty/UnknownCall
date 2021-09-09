package ui.employees

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.services.it.feel.R

class FilterCitiesDialog : DialogFragment(), DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
    private val viewModel by activityViewModels<EmployeesViewModel>()

    private val selectedCities by lazy { viewModel.selectedCities.clone() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cities)
            .setMultiChoiceItems(viewModel.cities, selectedCities, this)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                dialog?.dismiss()
                viewModel.applySelectedCities(selectedCities)
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                dialog?.dismiss()
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, index: Int, isSelected: Boolean) {
        try {
            selectedCities[index] = isSelected
        } catch (e: Exception) {
            e.printStackTrace()
            Firebase.crashlytics.recordException(e)
        }
    }
}