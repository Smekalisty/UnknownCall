package ui.employees

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.services.it.feel.R
import ui.employees.pojo.EmployeeEntity
import ui.employees.pojo.EmployeeEntityExtended
import utils.Utils

open class EmployeeViewHolder(view: View) : ViewHolder(view) {
    private val logo: ShapeableImageView = view.findViewById(R.id.logo)
    private val initials: MaterialTextView = view.findViewById(R.id.initials)
    private val primary: MaterialTextView = view.findViewById(R.id.primary)
    private val secondary: MaterialTextView = view.findViewById(R.id.secondary)
    private val call: MaterialButton = view.findViewById(R.id.call)

    override fun bind(employeeEntity: EmployeeEntity, onSelected: (Int) -> Unit) {
        val employeeHolderExtended = employeeEntity as EmployeeEntityExtended
        val employee = employeeHolderExtended.payload

        val view = this.itemView
        this.primary.text = view.context.getString(R.string.combine_two_words, employee.firstName.toString().trim(), employee.lastName.toString().trim())

        val secondary = employee.specialization

        if (secondary.isNullOrEmpty()) {
            this.secondary.visibility = View.GONE
        } else {
            this.secondary.text = secondary
            this.secondary.visibility = View.VISIBLE
        }

        val initials = if (employee.firstName.isNullOrEmpty()) {
            if (employee.lastName.isNullOrEmpty()) {
                "XY"
            } else {
                employee.lastName[0].toString()
            }
        } else {
            var initials = employee.firstName[0].toString()
            if (!employee.lastName.isNullOrEmpty()) {
                initials += employee.lastName[0].toString()
            }
            initials
        }

        this.initials.text = initials

        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                this@EmployeeViewHolder.initials.visibility = View.VISIBLE
                return false
            }

            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                this@EmployeeViewHolder.initials.visibility = View.INVISIBLE
                return false
            }
        }

        val requestOptions = RequestOptions.circleCropTransform()
            .placeholder(Utils.generateOvalDrawable(this.primary.text.toString()))
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(view)
            .load(employee.photo)
            .listener(listener)
            .apply(requestOptions)
            .into(this.logo)

        this.call.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${employee.phoneNumber}")
            ContextCompat.startActivity(view.context, intent, null)
        }

        this.itemView.setOnClickListener {
            onSelected(adapterPosition)
        }
    }
}