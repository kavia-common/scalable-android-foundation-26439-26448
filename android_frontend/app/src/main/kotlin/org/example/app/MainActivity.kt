package org.example.app

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.example.app.ui.dashboard.DashboardFragment
import org.example.app.ui.home.HomeFragment
import org.example.app.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var fragmentManager: FragmentManager

    private var selectedItemId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activity shell layout: DrawerLayout + Toolbar + Fragment container + BottomNav
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        bottomNav = findViewById(R.id.bottom_nav)

        fragmentManager = supportFragmentManager

        // Restore selected tab across configuration changes
        selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_ITEM_ID) ?: R.id.nav_home

        // Classic drawer toggle animation for hamburger icon
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Manual fragment setup: ensure all fragments exist, then show selected.
        ensureFragmentsCreated()
        showDestination(selectedItemId, updateUiSelection = true)

        // BottomNavigationView: no-op on reselection; no back stack usage for tab switches.
        bottomNav.setOnItemReselectedListener {
            // Do nothing: required behavior.
        }
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true
            showDestination(item.itemId, updateUiSelection = true)
            true
        }

        // Drawer NavigationView: same show/hide behavior, then close drawer.
        navigationView.setNavigationItemSelectedListener { item ->
            if (item.itemId != selectedItemId) {
                showDestination(item.itemId, updateUiSelection = true)
            } else {
                // Keep selection in sync even if the same item is tapped.
                syncNavigationSelection(item.itemId)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Back press behavior:
        // - If drawer open => close drawer.
        // - If on non-default tab => return to default (Home).
        // - If on default tab => allow system behavior (exit).
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        return
                    }

                    if (selectedItemId != R.id.nav_home) {
                        showDestination(R.id.nav_home, updateUiSelection = true)
                        return
                    }

                    // Default tab: exit app (let Activity finish/back stack handle it).
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM_ID, selectedItemId)
    }

    override fun onSupportNavigateUp(): Boolean {
        // With manual fragment navigation, "up" just opens the drawer (top-level destinations).
        if (::drawerLayout.isInitialized) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onSupportNavigateUp()
    }

    private fun ensureFragmentsCreated() {
        // Create each tab fragment once (by tag) and add to container, hidden except default.
        val home = fragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment()
        val dashboard = fragmentManager.findFragmentByTag(TAG_DASHBOARD) ?: DashboardFragment()
        val settings = fragmentManager.findFragmentByTag(TAG_SETTINGS) ?: SettingsFragment()

        fragmentManager.beginTransaction().apply {
            // Add if needed; we add all upfront to preserve state and make show/hide predictable.
            if (!home.isAdded) add(R.id.nav_host_fragment, home, TAG_HOME)
            if (!dashboard.isAdded) add(R.id.nav_host_fragment, dashboard, TAG_DASHBOARD)
            if (!settings.isAdded) add(R.id.nav_host_fragment, settings, TAG_SETTINGS)

            // Initially hide all; we'll show the selected in showDestination().
            hide(home)
            hide(dashboard)
            hide(settings)

            commitNowAllowingStateLoss()
        }
    }

    private fun showDestination(itemId: Int, updateUiSelection: Boolean) {
        val home = fragmentManager.findFragmentByTag(TAG_HOME)
        val dashboard = fragmentManager.findFragmentByTag(TAG_DASHBOARD)
        val settings = fragmentManager.findFragmentByTag(TAG_SETTINGS)

        val targetTag = when (itemId) {
            R.id.nav_home -> TAG_HOME
            // Menu IDs must match existing menus; here we map Search/Profile IDs to Dashboard/Settings
            // to satisfy the user instruction to navigate between Home, Dashboard, and Settings.
            R.id.nav_search -> TAG_DASHBOARD
            R.id.nav_profile -> TAG_SETTINGS
            else -> TAG_HOME
        }

        val target = fragmentManager.findFragmentByTag(targetTag)
        if (target == null) return

        fragmentManager.beginTransaction().apply {
            // Hide others, show target (no back stack).
            listOfNotNull(home, dashboard, settings).forEach { fragment ->
                if (fragment == target) show(fragment) else hide(fragment)
            }
            commit()
        }

        selectedItemId = itemId

        if (updateUiSelection) {
            syncNavigationSelection(itemId)
        }

        // Update toolbar title based on shown fragment.
        val titleRes = when (targetTag) {
            TAG_HOME -> R.string.nav_home
            TAG_DASHBOARD -> R.string.nav_dashboard
            TAG_SETTINGS -> R.string.nav_settings
            else -> R.string.app_name
        }
        supportActionBar?.setTitle(titleRes)
    }

    private fun syncNavigationSelection(itemId: Int) {
        // Keep BottomNav and Drawer selection in sync, but avoid infinite loops.
        if (bottomNav.selectedItemId != itemId) {
            bottomNav.menu.findItem(itemId)?.isChecked = true
            bottomNav.selectedItemId = itemId
        } else {
            bottomNav.menu.findItem(itemId)?.isChecked = true
        }

        navigationView.menu.findItem(itemId)?.isChecked = true
    }

    private companion object {
        private const val KEY_SELECTED_ITEM_ID = "selected_item_id"

        // Fragment tags (stable identifiers for show/hide state preservation)
        private const val TAG_HOME = "tab_home"
        private const val TAG_DASHBOARD = "tab_dashboard"
        private const val TAG_SETTINGS = "tab_settings"
    }
}
