package ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.services.it.feel.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import utils.BaseFragment
import utils.Preference

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    companion object {
        const val title = "title"
    }

    private val viewModel by viewModels<StartupViewModel>()

    private lateinit var appBarConfiguration: AppBarConfiguration

    //TODO binding
    private lateinit var drawer: DrawerLayout
    private lateinit var navigation: NavigationView
    private lateinit var logo: ShapeableImageView
    private lateinit var primaryText: MaterialTextView
    private lateinit var secondaryText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        drawer = findViewById(R.id.drawer)
        navigation = findViewById(R.id.navigation)
        logo = navigation.getHeaderView(0).findViewById(R.id.logo)
        primaryText = navigation.getHeaderView(0).findViewById(R.id.primaryText)
        secondaryText = navigation.getHeaderView(0).findViewById(R.id.secondaryText)

        val navigationController = findNavigationController()

        setSupportActionBar(toolbar)

        appBarConfiguration = AppBarConfiguration(navigationController.graph, drawer)

        setupActionBarWithNavController(navigationController, appBarConfiguration)
        navigation.setupWithNavController(navigationController)
        navigation.setNavigationItemSelectedListener(onNavigation)

        navigationController.addOnDestinationChangedListener { _, _, arguments ->
            val title = arguments?.getString(MainActivity.title)
            if (title != null) {
                supportActionBar?.title = title
            }
        }

        if (Firebase.auth.currentUser == null) {
            if (savedInstanceState == null) {
                navigationController.navigate(R.id.employees_to_welcome)
            }
            onAuth(false)
        } else {
            onAuth(true)
        }

        changeMenuRookieVisibility(Preference.getWritePermission(this))

        viewModel.checkWritePermission()

        lifecycleScope.launchWhenCreated {
            viewModel.signInMutableFlow
                .onEach(::onAuth)
                .collect()
        }

        lifecycleScope.launchWhenCreated {
            viewModel.writePermissionFlow
                .onEach(::onCheckWritePermission)
                .collect()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (!canFinish()) {
            return false
        }

        return findNavigationController().navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val canFinish = closeDrawer()
        if (!canFinish) {
            return
        }

        if (canFinish()) {
            super.onBackPressed()
        }
    }

    private fun closeDrawer(): Boolean {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return false
        }
        return true
    }

    private fun canFinish(): Boolean {
        val navigationHostFragment = supportFragmentManager.findFragmentById(R.id.navigationHostFragment) as NavHostFragment
        val primaryNavigationFragment = navigationHostFragment.childFragmentManager.primaryNavigationFragment
        if (primaryNavigationFragment is BaseFragment) {
            return primaryNavigationFragment.canFinish()
        }

        return true
    }

    private fun findNavigationController(): NavController {
        val navigationHostFragment = supportFragmentManager.findFragmentById(R.id.navigationHostFragment) as NavHostFragment
        return navigationHostFragment.navController
    }

    private val onNavigation = NavigationView.OnNavigationItemSelectedListener {
        closeDrawer()

        when (it.itemId) {
            R.id.menu_item_rookie -> {
                findNavigationController().navigate(R.id.rookieFragment)
            }

            R.id.menu_item_permission -> {
                findNavigationController().navigate(R.id.permissionFragment)
            }

            R.id.menu_item_sign_out -> {
                viewModel.signOut(this)
                findNavigationController().navigate(R.id.employees_to_welcome)

                changeMenuRookieVisibility(false)
                onAuth(false)
            }
        }

        true
    }

    private fun onCheckWritePermission(result: Boolean) {
        Preference.setWritePermission(this, result)
        changeMenuRookieVisibility(result)
    }

    private fun changeMenuRookieVisibility(result: Boolean) {
        val menuItem = navigation.menu.findItem(R.id.menu_item_rookie)
        menuItem.isVisible = result
    }

    private fun onAuth(isAuth: Boolean) {
        val menuItem = navigation.menu.findItem(R.id.menu_item_sign_out)
        menuItem.isVisible = isAuth

        val requestOptions = RequestOptions.circleCropTransform()
            .error(R.drawable.application_logo)
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(this)
            .load(Firebase.auth.currentUser?.photoUrl)
            .apply(requestOptions)
            .into(logo)

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            primaryText.setText(R.string.label_welcome)
            secondaryText.setText(R.string.application_name)
        } else {
            primaryText.text = currentUser.displayName
            secondaryText.text = currentUser.email
        }
    }

    // TODO theme from settings
    // TODO message - action teams
}