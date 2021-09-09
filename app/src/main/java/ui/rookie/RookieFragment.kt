package ui.rookie

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.services.it.feel.R
import com.services.it.feel.databinding.FragmentRookieBinding
import ui.employees.pojo.Employee
import utils.BaseFragment
import android.content.Intent
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import constants.RequestCodes
import ui.employees.EmployeesViewModel
import utils.Preference
import utils.Utils
import java.io.File

// TODO looks like rename all rookie
// TODO upload 2 different extension and verify if deleted on server side
class RookieFragment : BaseFragment(R.layout.fragment_rookie) {
    private var binding: FragmentRookieBinding? = null

    private val viewModel by activityViewModels<RookieViewModel>()

    private val employeesViewModel by activityViewModels<EmployeesViewModel>()

    private var employee: Employee? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRookieBinding.bind(view)
        this.binding = binding

        val hasWritePermission = Preference.getWritePermission(view.context)

        with(binding) {
            progress.visibility = View.INVISIBLE

            if (hasWritePermission) {
                submit.setOnClickListener(::onClickSubmit)
                photo.setOnClickListener(::onClickPhoto)
            } else {
                editContainer.visibility = View.GONE
                submit.visibility = View.GONE
                changeState(isEnabled = false, withProgress = false)
            }
        }

        employee = arguments?.getParcelable("employee")
        if (employee == null) {
            loadPhoto(Any())
        } else {
            populate()
            setHasOptionsMenu(true)
        }

        viewModel.clearPhoto()
        viewModel.photoAction = StoragePhotoAction.NOTHING
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.rookie_menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.icon_menu_delete -> {
                if (inProgress()) {
                    return false
                }

                fun onPositive(dialog: DialogInterface, @Suppress("UNUSED_PARAMETER") which: Int) {
                    dialog.dismiss()

                    employee?.let {
                        viewModel.deleteEmployee(it) {
                            employeesViewModel.requestDataSourceFromNetwork()
                            findNavController().popBackStack()
                        }
                    }
                }

                fun onNegative(dialog: DialogInterface, @Suppress("UNUSED_PARAMETER") which: Int) {
                    dialog.dismiss()
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.application_name)
                    .setMessage(R.string.are_you_sure_want_to_delete_this_account)
                    .setPositiveButton(android.R.string.ok, ::onPositive)
                    .setNegativeButton(android.R.string.cancel, ::onNegative)
                    .show()
                true
            }
            else -> false
        }
    }

    private fun onClickSubmit(@Suppress("UNUSED_PARAMETER") view: View) {
        val binding = binding ?: return

        if (binding.firstName.text.isNullOrEmpty()) {
            binding.firstNameLayout.error = getString(R.string.required_field) //TODO common string
            return
        }

        if (binding.lastName.text.isNullOrEmpty()) {
            binding.lastNameLayout.error = getString(R.string.required_field)
            return
        }

        if (binding.phoneNumber.text.isNullOrEmpty()) {
            binding.phoneNumberLayout.error = getString(R.string.required_field)
            return
        }

        if (binding.specialization.text.isNullOrEmpty()) {
            binding.specializationLayout.error = getString(R.string.required_field)
            return
        }

        if (binding.city.text.isNullOrEmpty()) {
            binding.cityLayout.error = getString(R.string.required_field)
            return
        }

        binding.firstName.clearFocus()
        binding.lastName.clearFocus()
        binding.corporateEmail.clearFocus()
        binding.phoneNumber.clearFocus()
        binding.skype.clearFocus()
        binding.specialization.clearFocus()
        binding.city.clearFocus()
        binding.remote.clearFocus()
        binding.ex.clearFocus()

        val firstName = binding.firstName.text?.toString()?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        val lastName = binding.lastName.text?.toString()?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
        val corporateEmail = binding.corporateEmail.text?.toString()?.trim()?.lowercase() ?: ""
        val phoneNumber = binding.phoneNumber.text?.toString()?.trim() ?: ""
        val skype = binding.skype.text?.toString()?.trim() ?: ""
        val specialization = binding.specialization.text?.toString()?.trim() ?: ""
        val city = binding.city.text?.toString()?.trim() ?: ""
        val remote = binding.remote.isChecked
        val ex = binding.ex.isChecked

        changeState(false)

        val isRookie = employee == null
        val documentId = if (isRookie) {
            null
        } else {
            employee?.documentId
        }

        // TODO create viewModel
        val employee = Employee(
            documentId = documentId,
            firstName = firstName,
            lastName = lastName,
            corporateEmail = corporateEmail,
            phoneNumber = phoneNumber,
            skype = skype,
            specialization = specialization,
            city = city,
            remote = remote,
            ex = ex
        )

        val onSuccess = if (isRookie) {
            ::onAddRookieSuccess
        } else {
            employee.photo = this.employee?.photo
            ::onUpdateEmployeeSuccess
        }

        viewModel.saveEmployee(employee) {
            it.fold(onSuccess, ::onError)
        }
    }

    private fun populate() {
        val employee = employee ?: return
        val binding = binding ?: return

        loadPhoto(employee.photo ?: Any())

        binding.firstName.setText(employee.firstName ?: "")
        binding.lastName.setText(employee.lastName ?: "")
        binding.corporateEmail.setText(employee.corporateEmail ?: "")
        binding.phoneNumber.setText(employee.phoneNumber ?: "")
        binding.skype.setText(employee.skype ?: "")
        binding.specialization.setText(employee.specialization ?: "")
        binding.city.setText(employee.city ?: "")
        binding.remote.isChecked = employee.remote ?: false
        binding.ex.isChecked = employee.ex ?: false
    }

    private fun loadPhoto(model: Any) {
        val employee = this.employee

        val result = if (employee == null) {
            Pair("XY", "XY")
        } else {
            val fullName = requireContext().getString(R.string.combine_two_words, employee.firstName.toString().trim(), employee.lastName.toString().trim())

            val initials = if (employee.firstName.isNullOrEmpty()) {
                if (employee.lastName.isNullOrEmpty()) {
                    "XY"
                } else {
                    employee.lastName[0].toString()
                }
            } else {
                var initials = employee.firstName[0].toString()
                if (!employee.lastName.isNullOrEmpty()) {
                    initials += employee.lastName[0].toString()
                }
                initials
            }

            Pair(fullName, initials)
        }

        val binding = binding ?: return

        binding.initials.text = result.second
        binding.initials.visibility = View.INVISIBLE
        binding.editContainer.visibility = View.INVISIBLE

        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                binding.initials.visibility = View.VISIBLE
                binding.editContainer.visibility = View.VISIBLE
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                binding.initials.visibility = View.INVISIBLE
                binding.editContainer.visibility = View.VISIBLE

                if (model is File) {
                    viewModel.photoAction = StoragePhotoAction.UPLOAD
                }

                return false
            }
        }

        val pair = if (model is File) {
            Pair(DiskCacheStrategy.NONE, true)
        } else {
            Pair(DiskCacheStrategy.ALL, false)
        }

        val requestOptions = RequestOptions.circleCropTransform()
            .error(Utils.generateOvalDrawable(result.first))
            .diskCacheStrategy(pair.first)
            .skipMemoryCache(pair.second)

        Glide.with(binding.root)
            .load(model)
            .listener(listener)
            .apply(requestOptions)
            .into(binding.photo)
    }

    private fun onUpdateEmployeeSuccess(employee: Employee) {
        this.employee = employee
        onSaveEmployeeSuccess()
        showMessage(R.string.employee_was_successfully_updated)
    }

    private fun onAddRookieSuccess(@Suppress("UNUSED_PARAMETER") employee: Employee) {
        onSaveEmployeeSuccess()
        clear()
        showMessage(R.string.employee_was_successfully_added)
    }

    private fun onSaveEmployeeSuccess() {
        changeState(true)
        viewModel.photoAction = StoragePhotoAction.NOTHING
        viewModel.clearPhoto()

        employeesViewModel.requestDataSourceFromNetwork()
    }

    private fun showMessage(id: Int) {
        binding?.let {
            Snackbar.make(it.root, id, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onError(error: Throwable) {
        error.printStackTrace()

        changeState(true)

        binding?.let {
            Snackbar.make(it.root, error.message.toString(), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun changeState(isEnabled: Boolean, withProgress: Boolean = true) {
        binding?.let {
            if (withProgress) {
                if (isEnabled) {
                    it.progress.visibility = View.INVISIBLE
                } else {
                    it.progress.visibility = View.VISIBLE
                }
            }

            it.firstNameLayout.isEnabled = isEnabled
            it.lastNameLayout.isEnabled = isEnabled
            it.corporateEmailLayout.isEnabled = isEnabled
            it.phoneNumberLayout.isEnabled = isEnabled
            it.skypeLayout.isEnabled = isEnabled
            it.specializationLayout.isEnabled = isEnabled
            it.cityLayout.isEnabled = isEnabled
            it.remote.isEnabled = isEnabled
            it.ex.isEnabled = isEnabled
            it.photo.isEnabled = isEnabled

            it.submit.isEnabled = isEnabled
        }
    }

    private fun clear() {
        loadPhoto(Any())

        binding?.let {
            it.firstName.text?.clear()
            it.firstNameLayout.error = null

            it.lastName.text?.clear()
            it.lastNameLayout.error = null

            it.corporateEmail.text?.clear()
            it.corporateEmailLayout.error = null

            it.phoneNumber.text?.clear()
            it.phoneNumberLayout.error = null

            it.skype.text?.clear()
            it.skypeLayout.error = null

            it.specialization.text?.clear()
            it.specializationLayout.error = null

            it.city.text?.clear()
            it.cityLayout.error = null

            it.remote.isChecked = false
            it.ex.isChecked = false
        }
    }

    private fun onClickPhoto(view: View) {
        val hasPhoto = binding?.initials?.visibility == View.VISIBLE
        if (hasPhoto) {
            selectPhotoIntent()
        } else {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.rookie_menu_photo, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.remove_photo -> {
                        viewModel.photoAction = StoragePhotoAction.DELETE
                        viewModel.clearPhoto()
                        loadPhoto(Any())
                    }
                    R.id.select_photo -> selectPhotoIntent()
                }
                return@setOnMenuItemClickListener true
            }
            popup.show()
        }
    }

    private fun selectPhotoIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RequestCodes.requestCodeSelectPhoto)
    }

    //TODO try to use new API for Activity Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCodes.requestCodeSelectPhoto) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data ?: return
                viewModel.copyPhoto(uri) {
                    it.fold(::loadPhoto, ::onError)
                }
            }
        }
    }

    override fun canFinish(): Boolean {
        if (!inProgress()) {
            return true
        }

        binding?.let {
            Snackbar.make(it.root, R.string.please_wait_a_little, Snackbar.LENGTH_SHORT).show() //TODO common string
        }
        return false
    }

    private fun inProgress(): Boolean {
        val binding = binding ?: return false
        if (binding.progress.visibility == View.INVISIBLE) {
            return false
        }
        return true
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}