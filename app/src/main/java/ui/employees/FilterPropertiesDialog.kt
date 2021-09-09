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

class FilterPropertiesDialog : DialogFragment(), DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
    private val viewModel by activityViewModels<EmployeesViewModel>()

    private val selectedProperties by lazy { viewModel.selectedProperties.clone() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.properties)
            .setMultiChoiceItems(viewModel.properties, selectedProperties, this)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                dialog?.dismiss()
                viewModel.applySelectedProperties(selectedProperties)
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                dialog?.dismiss()
            }
        }
    }

    override fun onClick(dialog: DialogInterface?, index: Int, isSelected: Boolean) {
        try {
            selectedProperties[index] = isSelected
        } catch (e: Exception) {
            e.printStackTrace()
            Firebase.crashlytics.recordException(e)
        }
    }
}