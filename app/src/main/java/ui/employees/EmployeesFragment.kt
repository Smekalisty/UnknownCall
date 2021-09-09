package ui.employees

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestoreException
import com.services.it.feel.R
import com.services.it.feel.databinding.FragmentEmployeesBinding
import kotlinx.coroutines.flow.collect
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityExtended

class EmployeesFragment : Fragment(R.layout.fragment_employees), SearchView.OnQueryTextListener {
    private var binding: FragmentEmployeesBinding? = null

    private var adapter: EmployeesAdapter? = null

    private val viewModel by activityViewModels<EmployeesViewModel>()

    private var query: String? = null

    private var justSigned = false

    private var force = false

    init {
        lifecycleScope.launchWhenCreated {
            viewModel.employeesFlow
                .collect(::onDataSource)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentEmployeesBinding.bind(view)
        this.binding = binding

        justSigned = arguments?.getBoolean("justSigned", false) ?: false

        with(binding) {
            infoLayout.visibility = View.GONE

            refresh.isRefreshing = true
            refresh.setOnRefreshListener {
                force = true
                viewModel.requestDataSourceFromNetwork()
            }

            recyclerView.addItemDecoration(SpaceItemDecoration(view.context))
            recyclerView.layoutManager = LinearLayoutManager(view.context)
            adapter = EmployeesAdapter(::onSelected)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }

        if (viewModel.dataSource.isEmpty()) {
            viewModel.requestDataSourceFromStorage()
            viewModel.requestDataSourceFromNetwork()
        } else {
            onDataSourceSuccess(viewModel.dataSource)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.employees_menu, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filterProperties -> {
                findNavController().navigate(R.id.employees_to_filter_properties)
                true
            }
            R.id.filterCities -> {
                findNavController().navigate(R.id.employees_to_filter_cities)
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun onDataSource(result: Result<List<EmployeeEntity>>) {
        result.fold(::onDataSourceSuccess, ::onDataSourceError)
    }

    private fun onDataSourceSuccess(dataSource: List<EmployeeEntity>) {
        force = false

        val selectedEmployee = adapter?.currentList?.firstOrNull { it is EmployeeEntityExtended && it.expanded }
        if (selectedEmployee != null) {
            val documentId = (selectedEmployee as EmployeeEntityExtended).payload.documentId
            val newSelectedEmployee = dataSource.firstOrNull { it is EmployeeEntityExtended && it.payload.documentId == documentId }
            (newSelectedEmployee as? EmployeeEntityExtended)?.expanded = true
        }

        updateUI(dataSource.isNotEmpty())
        adapter?.submitList(dataSource)

        if (justSigned) {
            arguments?.putBoolean("justSigned", false)
            justSigned = false

            fun onPositive(dialog: DialogInterface, @Suppress("UNUSED_PARAMETER") which: Int) {
                dialog.dismiss()
                findNavController().navigate(R.id.employees_to_permission)
            }

            fun onNegative(dialog: DialogInterface, @Suppress("UNUSED_PARAMETER") which: Int) {
                dialog.dismiss()
            }

            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.application_name)
                    .setMessage(R.string.permission_message)
                    .setPositiveButton(R.string.check_permissions, ::onPositive)
                    .setNegativeButton(android.R.string.cancel, ::onNegative)
                    .show()
        }
    }

    private fun onDataSourceError(error: Throwable) {
        val itemCount = binding?.recyclerView?.adapter?.itemCount
        val hasItems = if (itemCount == null) {
            false
        } else {
            itemCount > 0
        }

        updateUI(hasItems)

        if (force) {
            force = false

            if (error is FirebaseFirestoreException) {
                when (error.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE -> {
                        showMessage(getString(R.string.probably_no_connection))
                        return
                    }
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        showMessage(getString(R.string.ask_your_manager_to_give_you_access))
                        return
                    }
                    else -> { }
                }
            }

            val message = error.message ?: error.toString()
            showMessage(message)
        }
    }

    private fun updateUI(hasItems: Boolean) {
        binding?.let {
            it.refresh.isRefreshing = false

            if (hasItems) {
                it.infoLayout.visibility = View.GONE
            } else {
                it.infoLayout.visibility = View.VISIBLE

                if (query.isNullOrEmpty()) {
                    it.message.setText(R.string.the_list_of_employees_is_not_loaded)
                } else {
                    it.message.setText(R.string.no_matches)
                }
            }
        }
    }

    private fun onSelected(position: Int) {
        val adapter = adapter ?: return

        // un-select previous
        val previous = adapter.currentList.firstOrNull { it is EmployeeEntityExtended && it.expanded }
        if (previous != null) {
            (previous as? EmployeeEntityExtended)?.expanded = false
            val positionOfPrevious = adapter.currentList.indexOf(previous)
            adapter.notifyItemChanged(positionOfPrevious)
        }

        // select current
        val employeeHolder = try {
            adapter.getEmployeeEntity(position)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (previous == employeeHolder) {
            return
        }

        if (employeeHolder is EmployeeEntityExtended) {
            employeeHolder.expanded = !employeeHolder.expanded
            adapter.notifyItemChanged(position)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(query: String?): Boolean {
        this.query = query

        viewModel.filterByQuery(query)

        return true
    }

    private fun showMessage(message: String) {
        binding?.let {
            Snackbar.make(it.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}