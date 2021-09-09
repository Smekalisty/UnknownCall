package ui.employees

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import constants.FirebaseConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ui.employees.pojo.*
import utils.Preference

class EmployeesViewModel(application: Application) : AndroidViewModel(application) {

    private var employees = listOf<Employee>()

    var dataSource = listOf<EmployeeEntity>()
        private set

    var cities: Array<String> = arrayOf()
        private set

    var selectedCities = booleanArrayOf()
        private set

    val properties: Array<String> = arrayOf("Also show remote", "Also show ex") //TODO constants

    var selectedProperties = booleanArrayOf(true, false) //TODO constants
        private set

    private val employeesMutableFlow = MutableSharedFlow<Result<List<EmployeeEntity>>>()
    val employeesFlow = employeesMutableFlow.asSharedFlow()

    init {
        val selectedProperties = Preference.getSelectedProperties(getApplication())
        if (selectedProperties != null) {
            val result = properties(properties, selectedProperties)
            if (result != null) {
                this.selectedProperties = result
            }
        }
    }

    //TODO cancel from storage if requestDataSourceFromNetwork done
    fun requestDataSourceFromStorage() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val employees = Preference.getEmployees(getApplication())
                    val employeeContainer = filterEmployees(employees)

                    Result.success(employeeContainer)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            emitEmployees(result)
        }
    }

    fun requestDataSourceFromNetwork() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val response = Firebase.firestore
                        .collection(FirebaseConstants.employees)
                        .get(Source.SERVER)
                        .await()

                    val employees = response.toObjects<Employee>()
                    Preference.setEmployees(getApplication(), employees)

                    val employeeContainer = filterEmployees(employees)

                    Result.success(employeeContainer)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            emitEmployees(result)
        }
    }

    private fun filterEmployees(employees: List<Employee>): EmployeeContainer {
        // filter cities
        val selectedCities = getSelectedCities(employees)
        var filteredEmployees = filterByCities(employees, selectedCities)

        // filter properties
        val selectedProperties = getSelectedProperties(properties, selectedProperties)
        filteredEmployees = filterByProperties(filteredEmployees, selectedProperties)

        val pair = cities(employees, selectedCities)
        val dataSource = convertToDataSource(filteredEmployees)

        return EmployeeContainer(employees, dataSource, pair.first, pair.second)
    }

    private suspend fun emitEmployees(result: Result<EmployeeContainer>) {
        val container = result.getOrNull()
        if (container == null) {
            employeesMutableFlow.emit(Result.failure(result.exceptionOrNull() ?: Throwable("O_o")))
        } else {
            employees = container.employees
            dataSource = container.dataSource
            cities = container.cities
            selectedCities = container.selectedCities

            employeesMutableFlow.emit(Result.success(dataSource))
        }
    }

    fun filterByQuery(query: String?) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val filteredEmployees = if (query.isNullOrEmpty()) {
                    // filter cities
                    val selectedCities = getSelectedCities(employees)
                    var filteredEmployees = filterByCities(employees, selectedCities)

                    // filter properties
                    val selectedProperties = getSelectedProperties(properties, selectedProperties)
                    filteredEmployees = filterByProperties(filteredEmployees, selectedProperties)
                    filteredEmployees
                } else {
                    val filteredEmployees = employees.filter { it.firstName?.startsWith(query, true) ?: false || it.lastName?.startsWith(query, true) ?: false }
                    filteredEmployees
                }

                val dataSource = convertToDataSource(filteredEmployees)
                dataSource
            }

            dataSource = result
            employeesMutableFlow.emit(Result.success(result))
        }
    }

    fun applySelectedCities(newSelectedCities: BooleanArray) {
        selectedCities = newSelectedCities

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val result = mutableSetOf<String>()

                    for (i in newSelectedCities.indices) {
                        val item = newSelectedCities[i]
                        if (item) {
                            val city = cities[i]
                            result.add(city)
                        }
                    }

                    var filteredEmployees = filterByCities(employees, result)

                    val selectedProperties = getSelectedProperties(properties, selectedProperties)
                    filteredEmployees = filterByProperties(filteredEmployees, selectedProperties)

                    val dataSource = convertToDataSource(filteredEmployees)

                    //TODO return value as soon as ready so this need to extract
                    Preference.setSelectedCities(getApplication(), result)

                    Result.success(dataSource)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.failure(e)
                }
            }

            val dataSource = result.getOrNull()
            if (dataSource != null) {
                this@EmployeesViewModel.dataSource = dataSource
            }

            employeesMutableFlow.emit(result)
        }
    }

    fun applySelectedProperties(newSelectedProperties: BooleanArray) {
        selectedProperties = newSelectedProperties

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val selectedCities = getSelectedCities(employees)
                    var filteredEmployees = filterByCities(employees, selectedCities)

                    val selectedProperties = combineProperties(properties, newSelectedProperties)
                    filteredEmployees = filterByProperties(filteredEmployees, selectedProperties)

                    val dataSource = convertToDataSource(filteredEmployees)

                    //TODO return value as soon as ready so this need to extract
                    Preference.setSelectedProperties(getApplication(), selectedProperties)

                    Result.success(dataSource)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.failure(e)
                }
            }

            val dataSource = result.getOrNull()
            if (dataSource != null) {
                this@EmployeesViewModel.dataSource = dataSource
            }

            employeesMutableFlow.emit(result)
        }
    }

    private fun convertToDataSource(employees: List<Employee>): List<EmployeeEntity> {
        val groupedEmployees = employees.groupBy { it.city ?: "Wakanda" }

        val dataSource = mutableListOf<EmployeeEntity>()
        for (entry in groupedEmployees) {
            dataSource.add(EmployeeEntityCity(entry.key))

            val sorted = entry.value.sortedWith(compareBy({ it.firstName }, { it.lastName }))

            val employeesExtended = sorted.map { EmployeeEntityExtended(it, false) }
            dataSource.addAll(employeesExtended)
        }

        val groupCount = dataSource.count { it is EmployeeEntityCity }
        if (groupCount == 1) {
            val first = dataSource.first { it is EmployeeEntityCity }
            dataSource.remove(first)
        }

        return dataSource
    }

    private fun cities(employees: List<Employee>, selectedCities: Set<String>): Pair<Array<String>, BooleanArray> {
        try {
            val allCities = getAllCitiesFromEmployees(employees)
                .toTypedArray()

            val checkedItems = BooleanArray(allCities.size)

            for (i in allCities.indices) {
                val city = allCities[i]
                val isSelected = selectedCities.contains(city)
                checkedItems[i] = isSelected
            }

            return Pair(allCities, checkedItems)
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            return Pair(arrayOf(), booleanArrayOf())
        }
    }

    private fun properties(properties: Array<String>, selectedProperties: Set<String>): BooleanArray? {
        return try {
            val checkedItems = BooleanArray(properties.size)

            for (i in properties.indices) {
                val property = properties[i]
                val isSelected = selectedProperties.contains(property)
                checkedItems[i] = isSelected
            }

            checkedItems
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getSelectedCities(employees: List<Employee>): Set<String> {
        val cities = Preference.getSelectedCities(getApplication())
        return cities ?: getAllCitiesFromEmployees(employees).toSet()
    }

    private fun filterByCities(employees: List<Employee>, cities: Set<String>): List<Employee> {
        val result = mutableListOf<Employee>()

        for (employee in employees) {
            if (cities.contains(employee.city)) {
                result.add(employee)
            }
        }

        return result
    }

    private fun getAllCitiesFromEmployees(employees: List<Employee>): List<String> {
        return employees
            .distinctBy { it.city ?: "Wakanda" }
            .map { it.city ?: "Wakanda" }
    }

    private fun getSelectedProperties(properties: Array<String>, selectedProperties: BooleanArray): Set<String> {
        val cities = Preference.getSelectedProperties(getApplication())
        return cities ?: combineProperties(properties, selectedProperties)
    }

    private fun combineProperties(properties: Array<String>, selectedProperties: BooleanArray): Set<String> {
        val result = mutableSetOf<String>()

        for (i in selectedProperties.indices) {
            val item = selectedProperties[i]
            if (item) {
                val property = properties[i]
                result.add(property)
            }
        }

        return result
    }

    private fun filterByProperties(employees: List<Employee>, properties: Set<String>): List<Employee> {
        val remote = properties.contains("Also show remote")
        val ex = properties.contains("Also show ex")

        if (remote && ex) {
            return employees
        }

        if (!remote && !ex) {
            val filtered = employees.filter { it.remote == false }
            return filtered.filter { it.ex == false }
        }

        if (remote) {
            return employees.filterNot { it.ex == true }
        }

        if (ex) {
            return employees.filterNot { it.remote == true }
        }

        return emptyList()
    }
}