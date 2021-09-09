package ui.permission

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.services.it.feel.R

class PermissionFragment : Fragment(R.layout.fragment_permission) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val checkPermissions = view.findViewById<View>(R.id.check_permissions)
        checkPermissions.setOnClickListener {
            required1()
        }
    }

    private fun required1() {
        val required = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_DENIED) {
            required.add(Manifest.permission.READ_CALL_LOG)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            required.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            required.add(Manifest.permission.CALL_PHONE)
        }

        if (required.isEmpty()) {
            required2()
        } else {
            requestPermissions(required.toTypedArray(), 1)
        }
    }

    private fun required2() {
        val canDrawOverlays = Settings.canDrawOverlays(context)
        if (canDrawOverlays) {
            required3()
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireActivity().packageName}"))
            startActivityForResult(intent, 10)
        }
    }

    private fun required3() {
        if ("xiaomi".equals(Build.MANUFACTURER, true)) {
            val intent = Intent()
            intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            startActivity(intent)
        }

        findNavController().popBackStack()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        required2()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 10) {
            required3()
        }
    }
}