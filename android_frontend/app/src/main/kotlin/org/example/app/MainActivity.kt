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

    /**
     * The "logical" currently selected destination. We treat Home/Dashboard/Settings as the only
     * destinations, but map them onto the existing menu IDs:
     * - nav_home -> Home
     * - nav_search -> Dashboard
     * - nav_profile -> Settings
     */
    private var selectedItemId: Int = R.id.nav_home

    /**
     * Guards against recursive selection changes when we programmatically sync drawer/bottom-nav.
     */
    private var isSyncingSelection: Boolean = false

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

        // Restore selected tab across configuration changes (fragment instances are restored by FM).
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

        // Manual fragment setup: ensure all fragments exist (by stable tags), then show selection.
        ensureFragmentsCreated()
        showDestination(selectedItemId, updateUiSelection = true)

        // BottomNavigationView: no-op on reselection; no back stack usage for tab switches.
        bottomNav.setOnItemReselectedListener {
            // Do nothing by design.
        }
        bottomNav.setOnItemSelectedListener { item ->
            if (isSyncingSelection) return@setOnItemSelectedListener true

            val normalized = normalizeDestinationId(item.itemId)
            if (normalized == selectedItemId) return@setOnItemSelectedListener true

            showDestination(normalized, updateUiSelection = true)
            true
        }

        // Drawer NavigationView: same show/hide behavior, then close drawer.
        navigationView.setNavigationItemSelectedListener { item ->
            val normalized = normalizeDestinationId(item.itemId)
            if (normalized != selectedItemId) {
                showDestination(normalized, updateUiSelection = true)
            } else {
                // Keep selection in sync even if the same item is tapped.
                syncNavigationSelection(normalized)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Back press behavior:
        // - If drawer open => close drawer.
        // - If on non-home tab => return to home (no back stack entries for tabs).
        // - If already on home => exit (finish activity).
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
        // Create each tab fragment once (by tag) and add to container.
        // FragmentManager will restore existing instances after configuration changes; tags ensure we
        // can always find the same fragment instance and preserve its view/model state.
        val home = fragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment()
        val dashboard = fragmentManager.findFragmentByTag(TAG_DASHBOARD) ?: DashboardFragment()
        val settings = fragmentManager.findFragmentByTag(TAG_SETTINGS) ?: SettingsFragment()

        fragmentManager.beginTransaction().apply {
            // Add if needed; we add all upfront to preserve state and make show/hide predictable.
            if (!home.isAdded) add(R.id.nav_host_fragment, home, TAG_HOME)
            if (!dashboard.isAdded) add(R.id.nav_host_fragment, dashboard, TAG_DASHBOARD)
            if (!settings.isAdded) add(R.id.nav_host_fragment, settings, TAG_SETTINGS)

            // Hide all; we'll show the selected in showDestination().
            hide(home)
            hide(dashboard)
            hide(settings)

            // Use commitNow so the initial state is applied before we sync UI selections/titles.
            // Not using back stack keeps tab switching "flat" and makes back behavior deterministic.
            commitNowAllowingStateLoss()
        }
    }

    private fun showDestination(itemId: Int, updateUiSelection: Boolean) {
        val normalized = normalizeDestinationId(itemId)

        val home = fragmentManager.findFragmentByTag(TAG_HOME)
        val dashboard = fragmentManager.findFragmentByTag(TAG_DASHBOARD)
        val settings = fragmentManager.findFragmentByTag(TAG_SETTINGS)

        val targetTag = when (normalized) {
            R.id.nav_home -> TAG_HOME
            R.id.nav_search -> TAG_DASHBOARD
            R.id.nav_profile -> TAG_SETTINGS
            else -> TAG_HOME
        }

        val target = fragmentManager.findFragmentByTag(targetTag) ?: return

        fragmentManager.beginTransaction().apply {
            // Hide others, show target (no back stack).
            listOfNotNull(home, dashboard, settings).forEach { fragment ->
                if (fragment == target) show(fragment) else hide(fragment)
            }
            commit()
        }

        selectedItemId = normalized

        if (updateUiSelection) {
            syncNavigationSelection(normalized)
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

    /**
     * Maps any incoming menu item ID to a valid destination ID.
     * This makes the rest of the navigation code robust if menus ever diverge.
     */
    private fun normalizeDestinationId(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_home -> R.id.nav_home
            R.id.nav_search -> R.id.nav_search
            R.id.nav_profile -> R.id.nav_profile
            else -> R.id.nav_home
        }
    }

    private fun syncNavigationSelection(itemId: Int) {
        val normalized = normalizeDestinationId(itemId)

        // Keep BottomNav and Drawer selection in sync.
        // Use a guard to avoid triggering BottomNav listeners recursively.
        isSyncingSelection = true
        try {
            // Bottom navigation: set checked state + selectedItemId to update UI.
            if (bottomNav.selectedItemId != normalized) {
                bottomNav.selectedItemId = normalized
            }
            bottomNav.menu.findItem(normalized)?.isChecked = true

            // Drawer: mark checked item (single-check group in menu_drawer.xml).
            navigationView.menu.findItem(normalized)?.isChecked = true
        } finally {
            isSyncingSelection = false
        }
    }

    private companion object {
        private const val KEY_SELECTED_ITEM_ID = "selected_item_id"

        // Fragment tags (stable identifiers for show/hide state preservation)
        private const val TAG_HOME = "tab_home"
        private const val TAG_DASHBOARD = "tab_dashboard"
        private const val TAG_SETTINGS = "tab_settings"
    }
}
