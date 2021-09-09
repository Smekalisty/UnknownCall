package utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import kotlin.random.Random

object Utils {
    fun generateOvalDrawable(text: String): GradientDrawable {
        return generateDrawable(text, GradientDrawable.OVAL)
    }

    private fun generateDrawable(text: String, shape: Int): GradientDrawable {
        val random = Random(text.hashCode())

        val red = (32 + random.nextInt(256)) / 2
        val green = (128  + random.nextInt(256)) / 2
        val blue = (64 + random.nextInt(256)) / 2

        return GradientDrawable().apply {
            this.shape = shape
            this.setColor(Color.rgb(red, green, blue))
        }
    }
}