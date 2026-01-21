package org.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activity shell layout: DrawerLayout + Toolbar + FragmentContainerView + BottomNav
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // The set of top-level destinations: these won't show an "Up" button, but the drawer icon.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_search,
                R.id.nav_profile
            ),
            drawerLayout
        )

        // Hook up ActionBar/Toolbar title + drawer icon with Navigation component.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Drawer menu -> navigation destinations
        NavigationUI.setupWithNavController(navigationView, navController)

        // Bottom nav -> navigation destinations
        NavigationUI.setupWithNavController(bottomNav, navController)

        // Keep drawer selection in sync when navigating via bottom nav (optional UX polish)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = navigationView.menu
            when (destination.id) {
                R.id.nav_home -> menu.findItem(R.id.nav_home)?.isChecked = true
                R.id.nav_search -> menu.findItem(R.id.nav_search)?.isChecked = true
                R.id.nav_profile -> menu.findItem(R.id.nav_profile)?.isChecked = true
            }
        }

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
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
