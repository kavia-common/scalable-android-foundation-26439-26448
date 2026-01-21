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
     * This is important because setting BottomNavigationView.selectedItemId will trigger its
     * OnItemSelectedListener.
     */
    private var isSyncingSelection: Boolean = false

    private enum class SelectionSource {
        USER_BOTTOM_NAV,
        USER_DRAWER,
        PROGRAMMATIC
    }

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

        // Restore selected tab across configuration changes.
        // NOTE: Fragment instances are restored by FragmentManager automatically.
        selectedItemId = normalizeDestinationId(
            savedInstanceState?.getInt(KEY_SELECTED_ITEM_ID) ?: R.id.nav_home
        )

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

        // IMPORTANT ordering:
        // 1) Ensure fragments exist (safe on restore; won't add duplicates)
        // 2) Setup listeners (so user interactions go through a single entrypoint)
        // 3) Apply restored selection deterministically (show/hide + title + checked states)
        ensureFragmentsCreated()
        setupNavigationListeners()
        selectDestination(selectedItemId, source = SelectionSource.PROGRAMMATIC)

        // Back press behavior:
        // - If drawer open => close drawer.
        // - If on non-home tab => return to home (no back stack entries for tabs).
        // - If already on home => exit.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        return
                    }

                    if (selectedItemId != R.id.nav_home) {
                        selectDestination(R.id.nav_home, source = SelectionSource.PROGRAMMATIC)
                        return
                    }

                    // Home: exit activity.
                    finish()
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

    private fun setupNavigationListeners() {
        // BottomNavigationView: no-op on reselection; no back stack usage for tab switches.
        bottomNav.setOnItemReselectedListener {
            // Do nothing by design.
        }
        bottomNav.setOnItemSelectedListener { item ->
            if (isSyncingSelection) return@setOnItemSelectedListener true

            val normalized = normalizeDestinationId(item.itemId)
            if (normalized == selectedItemId) {
                // Ensure checked state consistency even on re-tap (rare edge cases).
                syncNavigationSelection(normalized)
                return@setOnItemSelectedListener true
            }

            selectDestination(normalized, source = SelectionSource.USER_BOTTOM_NAV)
            true
        }

        // Drawer NavigationView: same show/hide behavior, then close drawer.
        navigationView.setNavigationItemSelectedListener { item ->
            if (isSyncingSelection) {
                // Still close drawer to honor tap; return true to consume.
                drawerLayout.closeDrawer(GravityCompat.START)
                return@setNavigationItemSelectedListener true
            }

            val normalized = normalizeDestinationId(item.itemId)
            if (normalized == selectedItemId) {
                // Keep selection in sync even if the same item is tapped.
                syncNavigationSelection(normalized)
            } else {
                selectDestination(normalized, source = SelectionSource.USER_DRAWER)
            }

            // Always close drawer after a selection.
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun ensureFragmentsCreated() {
        // Create each tab fragment once (by tag) and add to container.
        // FragmentManager will restore existing instances after configuration changes; tags ensure we
        // can always find the same fragment instance and preserve its view/model state.
        val home = fragmentManager.findFragmentByTag(TAG_HOME) ?: HomeFragment()
        val dashboard = fragmentManager.findFragmentByTag(TAG_DASHBOARD) ?: DashboardFragment()
        val settings = fragmentManager.findFragmentByTag(TAG_SETTINGS) ?: SettingsFragment()

        // Only add fragments that are not already added.
        // IMPORTANT: do not blindly hide/show here based on current selection; on restore, FM may
        // already have correct visibility. We'll enforce a single visible fragment in
        // selectDestination(PROGRAMMATIC) below.
        fragmentManager.beginTransaction().apply {
            if (!home.isAdded) add(R.id.nav_host_fragment, home, TAG_HOME)
            if (!dashboard.isAdded) add(R.id.nav_host_fragment, dashboard, TAG_DASHBOARD)
            if (!settings.isAdded) add(R.id.nav_host_fragment, settings, TAG_SETTINGS)

            // If this is a first creation (not restore), start with all hidden to avoid flicker.
            // On restore, these fragments are already added and their hidden state will be restored;
            // calling hide() again is safe but can cause extra transactions. We avoid it.
            commitNow()
        }
    }

    /**
     * Single entrypoint for all destination changes.
     *
     * Responsibilities:
     * - show/hide fragments using stable tags (no Jetpack Navigation, no back stack)
     * - keep BottomNavigationView and NavigationView checked states in sync (both directions)
     * - update AppBar title correctly
     * - close drawer after user drawer selections
     */
    private fun selectDestination(itemId: Int, source: SelectionSource) {
        val normalized = normalizeDestinationId(itemId)

        // Show/hide fragments immediately to avoid transient states.
        val targetTag = when (normalized) {
            R.id.nav_home -> TAG_HOME
            R.id.nav_search -> TAG_DASHBOARD
            R.id.nav_profile -> TAG_SETTINGS
            else -> TAG_HOME
        }

        val target = fragmentManager.findFragmentByTag(targetTag) ?: return
        val allTabs = listOfNotNull(
            fragmentManager.findFragmentByTag(TAG_HOME),
            fragmentManager.findFragmentByTag(TAG_DASHBOARD),
            fragmentManager.findFragmentByTag(TAG_SETTINGS)
        )

        fragmentManager.beginTransaction().apply {
            allTabs.forEach { fragment ->
                if (fragment === target) show(fragment) else hide(fragment)
            }
            commitNow()
        }

        selectedItemId = normalized

        // Sync selection states without triggering recursion.
        syncNavigationSelection(normalized)

        // Update toolbar title based on shown fragment.
        val titleRes = when (targetTag) {
            TAG_HOME -> R.string.nav_home
            TAG_DASHBOARD -> R.string.nav_dashboard
            TAG_SETTINGS -> R.string.nav_settings
            else -> R.string.app_name
        }
        supportActionBar?.setTitle(titleRes)

        // UX: close the drawer only for drawer-originated selections.
        if (source == SelectionSource.USER_DRAWER && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
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
        // Use a guard to avoid triggering listeners recursively.
        isSyncingSelection = true
        try {
            // Bottom navigation: set selectedItemId to update UI (this may call the listener).
            if (bottomNav.selectedItemId != normalized) {
                bottomNav.selectedItemId = normalized
            }

            // Drawer: ensure exactly the one item is checked.
            // Use checked item to get proper single-check behavior.
            if (navigationView.checkedItem?.itemId != normalized) {
                navigationView.setCheckedItem(normalized)
            }

            // Defensive: align check states even if selection is unchanged.
            bottomNav.menu.findItem(normalized)?.isChecked = true
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
