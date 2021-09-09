package utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.services.it.feel.BuildConfig
import ui.employees.pojo.Employee

object Preference {
    fun preferences(context: Context): SharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    fun getEmployees(context: Context): List<Employee> = Gson().fromJson(preferences(context).getString("employees", "[]"), object : TypeToken<List<Employee>>(){}.type)
    fun setEmployees(context: Context, value: List<Employee>) = preferences(context).edit().putString("employees", Gson().toJson(value, object : TypeToken<List<Employee>>() {}.type)).apply()

    fun getWritePermission(context: Context): Boolean = preferences(context).getBoolean("writePermission", false)
    fun setWritePermission(context: Context, value: Boolean) = preferences(context).edit().putBoolean("writePermission", value).apply()

    fun getSelectedCities(context: Context): Set<String>? = preferences(context).getStringSet("selectedCities", null)
    fun setSelectedCities(context: Context, value: Set<String>) = preferences(context).edit().putStringSet("selectedCities", value).apply()

    fun getSelectedProperties(context: Context): Set<String>? = preferences(context).getStringSet("selectedProperties", null)
    fun setSelectedProperties(context: Context, value: Set<String>) = preferences(context).edit().putStringSet("selectedProperties", value).apply()
}