package ui.employees

import androidx.recyclerview.widget.DiffUtil
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityCity
import ui.employees.pojo.EmployeeEntityExtended

class EmployeesDiff : DiffUtil.ItemCallback<EmployeeEntity>() {
    override fun areItemsTheSame(old: EmployeeEntity, new: EmployeeEntity): Boolean {
        if (old is EmployeeEntityExtended && new is EmployeeEntityExtended) {
            return old.payload.documentId == new.payload.documentId
        } else if (old is EmployeeEntityCity && new is EmployeeEntityCity) {
            return old.city == new.city
        }
        return false
    }

    override fun areContentsTheSame(old: EmployeeEntity, new: EmployeeEntity): Boolean {
        if (old is EmployeeEntityExtended && new is EmployeeEntityExtended) {
            return old == new
        } else if (old is EmployeeEntityCity && new is EmployeeEntityCity) {
            return old == new
        }
        return false
    }
}