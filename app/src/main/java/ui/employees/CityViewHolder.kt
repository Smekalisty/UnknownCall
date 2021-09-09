package ui.employees

import android.view.View
import com.google.android.material.textview.MaterialTextView
import com.services.it.feel.R
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityCity

class CityViewHolder(view: View) : ViewHolder(view) {
    private val city: MaterialTextView = view.findViewById(R.id.city)

    override fun bind(employeeEntity: EmployeeEntity, onSelected: (Int) -> Unit) {
        city.text = (employeeEntity as EmployeeEntityCity).city
    }
}