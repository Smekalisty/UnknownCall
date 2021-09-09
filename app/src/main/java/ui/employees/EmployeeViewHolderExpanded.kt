package ui.employees

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.services.it.feel.R
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityExtended
import ui.main.MainActivity

class EmployeeViewHolderExpanded(view: View) : EmployeeViewHolder(view) {
    private val email: MaterialTextView = view.findViewById(R.id.email)
    private val phone: MaterialTextView = view.findViewById(R.id.phone)
    private val actionOpen: MaterialButton = view.findViewById(R.id.action_open)
    private val actionEmail: MaterialButton = view.findViewById(R.id.action_email)
    private val actionShare: MaterialButton = view.findViewById(R.id.action_share)

    override fun bind(employeeEntity: EmployeeEntity, onSelected: (Int) -> Unit) {
        super.bind(employeeEntity, onSelected)

        val employeeEntityExtended = employeeEntity as EmployeeEntityExtended
        val employee = employeeEntityExtended.payload

        email.text = employee.corporateEmail
        phone.text = employee.phoneNumber

        actionOpen.setOnClickListener {
            var title = it.context.getString(R.string.combine_two_words, employee.firstName, employee.lastName)
            if (title.length < 2) {
                title = it.context.getString(R.string.edit_rookie)
            }

            val bundle = Bundle()
            bundle.putParcelable("employee", employee)
            bundle.putString(MainActivity.title, title)
            it.findNavController().navigate(R.id.employees_to_rookie, bundle)
        }

        actionEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:")
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(employee.corporateEmail))
            it.context.startActivity(intent)
        }

        actionShare.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Share employee")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, employee.firstName + " " + employee.lastName + "\n" + employee.phoneNumber + "\n" + employee.corporateEmail)

            it.context.startActivity(Intent.createChooser(sharingIntent, "Share via"))
        }
    }
}