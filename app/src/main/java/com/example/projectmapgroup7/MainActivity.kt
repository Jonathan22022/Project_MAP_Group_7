package com.example.projectmapgroup7

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_insight),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Floating Action Button (FAB)
        binding.fab.bringToFront()
        binding.fab.setOnClickListener {
            navController.navigate(R.id.nav_addtask)
        }

        // --- Akses Header Drawer ---
        val headerView = navView.getHeaderView(0)
        val headerProfileContainer = headerView.findViewById<LinearLayout>(R.id.headerProfileContainer)
        val imageViewProfile = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)

        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Guest")
        val profileUrl = sharedPref.getString("profile_picture", null)

        tvUserName.text = username

        if (!profileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileUrl)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageViewProfile)
        }

        headerProfileContainer.setOnClickListener {
            navController.navigate(R.id.nav_account)
            drawerLayout.closeDrawers()
        }

        // ✅ Drawer Navigation item listener
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Konfirmasi Logout")
                        .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
                            sharedPref.edit().clear().apply()

                            navController.navigate(
                                R.id.loginFragment,
                                null,
                                androidx.navigation.NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_home, true)
                                    .build()
                            )
                            drawerLayout.closeDrawers()
                        }
                        .setNegativeButton("Batal") { dialog, _ ->
                            dialog.dismiss()
                            drawerLayout.closeDrawers()
                        }
                        .show()
                    true
                }
                else -> {
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }

        // ✅ Sembunyikan BottomAppBar & FAB saat di login/register
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fab = binding.fab
            val bottomAppBar = binding.bottomAppBar
            val toolbar = binding.toolbar

            when (destination.id) {
                R.id.loginFragment, R.id.loginFormFragment, R.id.registerFragment -> {
                    supportActionBar?.hide()
                    fab.isVisible = false
                    bottomAppBar.isVisible = false
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    supportActionBar?.show()
                    fab.isVisible = true
                    bottomAppBar.isVisible = true
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
