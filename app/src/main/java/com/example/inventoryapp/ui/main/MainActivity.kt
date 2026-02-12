package com.example.inventoryapp.ui.main

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val window = window
        val backgroundColor = ContextCompat.getColor(this, R.color.background)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNav()
        hideSystemNavigation()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemNavigation()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navProducts.setOnClickListener {
            navigateTo(R.id.productsListFragment)
        }
        binding.bottomNav.navEmployees.setOnClickListener {
            navigateTo(R.id.employeesListFragment)
        }
        binding.bottomNav.navHome.setOnClickListener {
            navigateTo(R.id.homeFragment)
        }
        binding.bottomNav.navWarehouse.setOnClickListener {
            navigateTo(R.id.warehouseFragment)
        }
        binding.bottomNav.navSettings.setOnClickListener {
            navigateTo(R.id.printerSettingsFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            updateBottomNavSelection(destination.id)
            updateAppBarVisibility(destination.id)
        }
    }

    private fun navigateTo(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, false)
            .build()
        navController.navigate(destinationId, null, navOptions)
    }

    private fun updateBottomNavSelection(destinationId: Int) {
        val productsIds = setOf(
            R.id.productsListFragment,
            R.id.productDetailsFragment,
            R.id.addProductFragment,
            R.id.templatesListFragment,
            R.id.templateDetailsFragment,
            R.id.bulkAddFragment
        )
        val employeesIds = setOf(
            R.id.employeesListFragment,
            R.id.employeeDetailsFragment,
            R.id.addEmployeeFragment,
            R.id.assignByScanFragment
        )
        val warehouseIds = setOf(
            R.id.warehouseFragment,
            R.id.warehouseLocationDetailsFragment,
            R.id.addLocationFragment
        )

        val activeColor = ContextCompat.getColor(this, R.color.home_nav_icon_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.home_nav_icon)

        setNavItemActive(
            productsIds.contains(destinationId),
            binding.bottomNav.navProductsIcon,
            binding.bottomNav.navProductsLabel,
            activeColor,
            inactiveColor
        )
        setNavItemActive(
            employeesIds.contains(destinationId),
            binding.bottomNav.navEmployeesIcon,
            binding.bottomNav.navEmployeesLabel,
            activeColor,
            inactiveColor
        )
        binding.bottomNav.navHomeIcon.setColorFilter(
            ContextCompat.getColor(this, R.color.white)
        )
        setNavItemActive(
            warehouseIds.contains(destinationId),
            binding.bottomNav.navWarehouseIcon,
            binding.bottomNav.navWarehouseLabel,
            activeColor,
            inactiveColor
        )
        setNavItemActive(
            destinationId == R.id.printerSettingsFragment,
            binding.bottomNav.navSettingsIcon,
            binding.bottomNav.navSettingsLabel,
            activeColor,
            inactiveColor
        )
    }

    private fun setNavItemActive(
        isActive: Boolean,
        iconView: ImageView,
        labelView: TextView?,
        activeColor: Int,
        inactiveColor: Int
    ) {
        val color = if (isActive) activeColor else inactiveColor
        iconView.setColorFilter(color)
        labelView?.setTextColor(color)
    }

    private fun updateAppBarVisibility(destinationId: Int) {
        binding.appBarLayout.visibility = android.view.View.GONE
    }

    private fun hideSystemNavigation() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
