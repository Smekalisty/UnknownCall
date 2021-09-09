package ui.employees

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.services.it.feel.R
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityExtended

class EmployeesAdapter(private val onSelected: (Int) -> Unit) : ListAdapter<EmployeeEntity, ViewHolder>(EmployeesDiff()) {
    override fun getItemViewType(position: Int): Int {
        val employeeHolder = getItem(position)

        return if (employeeHolder is EmployeeEntityExtended) {
            if (employeeHolder.expanded) {
                2
            } else {
                1
            }
        } else {
            0
        }
    }

    //TODO generic for class EmployeeEntity ???
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val view = layoutInflater.inflate(R.layout.employee_item_city, parent, false)
                CityViewHolder(view)
            }

            1 -> {
                val view = layoutInflater.inflate(R.layout.employee_item, parent, false)
                EmployeeViewHolder(view)
            }
            else -> {
                val view = layoutInflater.inflate(R.layout.employee_item_expanded, parent, false)
                EmployeeViewHolderExpanded(view)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val employeeHolder = getEmployeeEntity(position)
        viewHolder.bind(employeeHolder, onSelected)
    }

    fun getEmployeeEntity(position: Int): EmployeeEntity {
        return getItem(position)
    }
}