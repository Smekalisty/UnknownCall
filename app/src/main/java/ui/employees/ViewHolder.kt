package ui.employees

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import ui.employees.pojo.EmployeeEntity

abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(employeeEntity: EmployeeEntity, onSelected: (Int) -> Unit)
}