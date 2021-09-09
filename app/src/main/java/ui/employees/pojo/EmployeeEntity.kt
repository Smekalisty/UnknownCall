package ui.employees.pojo

open class EmployeeEntity

data class EmployeeEntityCity(val city: String) : EmployeeEntity()

data class EmployeeEntityExtended(val payload: Employee, var expanded: Boolean) : EmployeeEntity()