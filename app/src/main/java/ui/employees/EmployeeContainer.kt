package ui.employees

import ui.employees.pojo.Employee
import ui.employees.pojo.EmployeeEntity

class EmployeeContainer(
    val employees: List<Employee>,
    val dataSource: List<EmployeeEntity>,
    val cities: Array<String>,
    val selectedCities: BooleanArray)