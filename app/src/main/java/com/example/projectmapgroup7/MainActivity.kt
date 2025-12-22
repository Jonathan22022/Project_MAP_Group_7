package com.example.projectmapgroup7

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.projectmapgroup7.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.example.projectmapgroup7.util.NotificationUtils

/**
 * MainActivity
 *
 * Activity utama aplikasi yang berfungsi sebagai:
 * - Host Navigation Component (FragmentContainerView)
 * - Pengatur Toolbar, Drawer Navigation, dan Bottom Navigation
 * - Pengelola tema (Dark / Light Mode)
 * - Pengelola izin notifikasi dan session user
 */
class MainActivity : AppCompatActivity() {

    // Konfigurasi AppBar untuk Navigation Component
    private lateinit var appBarConfiguration: AppBarConfiguration

    // ViewBinding untuk Activity
    private lateinit var binding: ActivityMainBinding

    // Controller untuk navigasi antar fragment
    private lateinit var navController: NavController

    /**
     * Mengatur tema aplikasi (Dark / Light Mode)
     * berdasarkan preferensi yang tersimpan di SharedPreferences
     */
    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /**
     * Launcher untuk meminta izin notifikasi (Android 13+)
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                android.util.Log.d("Permission", "POST_NOTIFICATIONS diberikan.")
            } else {
                android.util.Log.e("Permission", "POST_NOTIFICATIONS ditolak user.")
            }
        }

    /**
     * Lifecycle utama Activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Terapkan tema sebelum layout di-inflate
        applySavedTheme()
        super.onCreate(savedInstanceState)

        // Inflate layout menggunakan ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Membuat Notification Channel (wajib Android 8+)
        NotificationUtils.createNotificationChannel(this)

        // Meminta izin notifikasi untuk Android 13 ke atas
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Mengatur toolbar sebagai ActionBar
        setSupportActionBar(binding.toolbar)

        // Inisialisasi DrawerLayout & NavigationView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // Mendapatkan NavController dari NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(
                R.id.nav_host_fragment_content_main
            ) as NavHostFragment
        navController = navHostFragment.navController

        // Konfigurasi AppBar (fragment yang dianggap top-level)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_insight),
            drawerLayout
        )

        // Sinkronisasi Toolbar dengan NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Sinkronisasi Drawer Navigation dengan NavController
        navView.setupWithNavController(navController)

        // Sinkronisasi Bottom Navigation dengan NavController
        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)

        // =====================
        // SETUP HEADER DRAWER
        // =====================

        val headerView = navView.getHeaderView(0)
        val headerProfileContainer =
            headerView.findViewById<LinearLayout>(R.id.headerProfileContainer)
        val imageViewProfile =
            headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val tvUserName =
            headerView.findViewById<TextView>(R.id.tvUserName)

        // Ambil data user dari SharedPreferences
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Guest")
        val profileUrl = sharedPref.getString("profile_picture", null)

        // Tampilkan username
        tvUserName.text = username

        // Load foto profil menggunakan Glide
        if (!profileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileUrl)
                .placeholder(R.drawable.ic_account_)
                .circleCrop()
                .into(imageViewProfile)
        }

        // Klik header → buka halaman Account
        headerProfileContainer.setOnClickListener {
            navController.navigate(R.id.nav_account)
            drawerLayout.closeDrawers()
        }

        // =====================
        // HANDLER MENU DRAWER
        // =====================

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                // Menu Logout
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Konfirmasi Logout")
                        .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            // Hapus session user
                            getSharedPreferences("user_session", MODE_PRIVATE)
                                .edit()
                                .clear()
                                .apply()

                            // Navigasi ke Login dan hapus backstack
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

                // Menu lain → default navigation
                else -> {
                    androidx.navigation.ui.NavigationUI
                        .onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }

        // =====================
        // VISIBILITY NAVIGATION
        // =====================

        // Sembunyikan toolbar & navigation pada halaman login/register
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val bottomNavView = binding.bottomNavigation
            val toolbar = binding.toolbar

            when (destination.id) {
                R.id.loginFragment,
                R.id.loginFormFragment,
                R.id.registerFragment -> {
                    supportActionBar?.hide()
                    bottomNavView.visibility = View.GONE
                    toolbar.visibility = View.GONE
                    drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                    )
                }

                else -> {
                    supportActionBar?.show()
                    bottomNavView.visibility = View.VISIBLE
                    toolbar.visibility = View.VISIBLE
                    drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_UNLOCKED
                    )
                }
            }
        }
    }

    /**
     * Mengatur aksi tombol back pada Toolbar
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
