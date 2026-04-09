package com.example.inventoryapp.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isFabMenuOpen = false

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

    override fun onBackPressed() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.navProducts.setOnClickListener {
            closeFabMenuIfOpen()
            navigateTo(R.id.productsListFragment)
        }
        binding.bottomNav.navEmployees.setOnClickListener {
            closeFabMenuIfOpen()
            navigateTo(R.id.employeesListFragment)
        }
        binding.bottomNav.navHome.setOnClickListener {
            closeFabMenuIfOpen()
            navigateTo(R.id.homeFragment)
        }
        binding.bottomNav.navWarehouse.setOnClickListener {
            closeFabMenuIfOpen()
            navigateTo(R.id.warehouseFragment)
        }
        binding.bottomNav.navMore.setOnClickListener {
            toggleFabMenu()
        }

        // FAB Menu interactions
        binding.bottomNav.fabMenuOverlay.setOnClickListener {
            closeFabMenu()
        }
        binding.bottomNav.fabMenuClose.setOnClickListener {
            closeFabMenu()
        }
        
        // Menu item clicks
        binding.bottomNav.fabMenuCompanies.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            closeFabMenu()
            navigateTo(R.id.companiesFragment)
        }
        binding.bottomNav.fabMenuPoints.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            closeFabMenu()
            navigateTo(R.id.contractorPointsFragment)
        }
        binding.bottomNav.fabMenuSettings.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            closeFabMenu()
            navigateTo(R.id.settingsHubFragment)
        }

        // Global FAB removed — fragments provide their own FABs now.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            closeFabMenuIfOpen()
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
        val moreIds = setOf(
            R.id.settingsHubFragment,
            R.id.printerSettingsFragment,
            R.id.companiesFragment,
            R.id.addEditCompanyFragment,
            R.id.contractorPointsFragment,
            R.id.addEditContractorPointFragment,
            R.id.contractorPointDetailsFragment
        )
        setNavItemActive(
            moreIds.contains(destinationId),
            binding.bottomNav.navMoreIcon,
            binding.bottomNav.navMoreLabel,
            activeColor,
            inactiveColor
        )
    }

    private fun showMoreMenu() {
        val labels = arrayOf(
            getString(R.string.bottom_nav_menu_companies),
            getString(R.string.bottom_nav_menu_points),
            getString(R.string.bottom_nav_menu_settings),
            getString(R.string.bottom_nav_menu_printer)
        )
        val destinations = intArrayOf(
            R.id.companiesFragment,
            R.id.contractorPointsFragment,
            R.id.settingsHubFragment,
            R.id.printerSettingsFragment
        )
        MaterialAlertDialogBuilder(this)
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                navigateTo(destinations[which])
            }
            .show()
    }

    private fun toggleFabMenu() {
        // Haptic feedback
        binding.bottomNav.navMore.performHapticFeedback(
            android.view.HapticFeedbackConstants.VIRTUAL_KEY,
            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    private fun openFabMenu() {
        isFabMenuOpen = true

        // Show overlay with fade animation
        binding.bottomNav.fabMenuOverlay.visibility = View.VISIBLE
        binding.bottomNav.fabMenuOverlay.alpha = 0f
        binding.bottomNav.fabMenuOverlay.animate()
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Show close button with rotation and scale
        binding.bottomNav.fabMenuClose.visibility = View.VISIBLE
        binding.bottomNav.fabMenuClose.alpha = 0f
        binding.bottomNav.fabMenuClose.scaleX = 0f
        binding.bottomNav.fabMenuClose.scaleY = 0f
        binding.bottomNav.fabMenuClose.rotation = -90f
        binding.bottomNav.fabMenuClose.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(1.5f))
            .setStartDelay(50)
            .start()

        // Animate menu buttons in arc formation (3 buttons)
        animateFabMenuItem(binding.bottomNav.fabMenuCompanies, 0, -75f, 0f)
        animateFabMenuItem(binding.bottomNav.fabMenuPoints, 50, 0f, -10f)
        animateFabMenuItem(binding.bottomNav.fabMenuSettings, 100, 75f, 0f)
    }

    private fun animateFabMenuItem(view: View, startDelay: Long, translationX: Float, additionalY: Float) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.translationX = 0f
        view.translationY = 50f

        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f)
        val transXAnim = ObjectAnimator.ofFloat(view, "translationX", 0f, translationX)
        val transYAnim = ObjectAnimator.ofFloat(view, "translationY", 50f, additionalY)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim, transXAnim, transYAnim)
        animatorSet.duration = 400
        animatorSet.startDelay = startDelay
        animatorSet.interpolator = OvershootInterpolator(1.2f)
        animatorSet.start()
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false

        // Fade out overlay
        binding.bottomNav.fabMenuOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.bottomNav.fabMenuOverlay.visibility = View.GONE
            }
            .start()

        // Animate close button out
        binding.bottomNav.fabMenuClose.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .rotation(90f)
            .setDuration(200)
            .withEndAction {
                binding.bottomNav.fabMenuClose.visibility = View.GONE
            }
            .start()

        // Animate menu items out (3 buttons)
        hideFabMenuItem(binding.bottomNav.fabMenuCompanies, 0)
        hideFabMenuItem(binding.bottomNav.fabMenuPoints, 30)
        hideFabMenuItem(binding.bottomNav.fabMenuSettings, 60)
    }

    private fun hideFabMenuItem(view: View, startDelay: Long) {
        val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleXAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.3f)
        val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.3f)
        val transXAnim = ObjectAnimator.ofFloat(view, "translationX", view.translationX, 0f)
        val transYAnim = ObjectAnimator.ofFloat(view, "translationY", view.translationY, 50f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim, transXAnim, transYAnim)
        animatorSet.duration = 200
        animatorSet.startDelay = startDelay
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()

        view.postDelayed({
            view.visibility = View.GONE
        }, startDelay + 200)
    }

    private fun closeFabMenuIfOpen() {
        if (isFabMenuOpen) {
            closeFabMenu()
        }
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
