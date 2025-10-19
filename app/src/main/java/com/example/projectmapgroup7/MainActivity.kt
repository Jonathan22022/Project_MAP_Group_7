package com.example.projectmapgroup7

// Import berbagai komponen Android dan Jetpack yang dibutuhkan
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

/**
 * MainActivity berfungsi sebagai activity utama aplikasi
 * yang mengatur navigasi (drawer, bottom nav, toolbar, FAB)
 * serta mengelola tampilan utama berdasarkan fragment aktif.
 */
class MainActivity : AppCompatActivity() {

    // Deklarasi variabel global
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menghubungkan layout XML dengan binding agar lebih mudah mengakses view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengatur toolbar sebagai ActionBar utama
        setSupportActionBar(binding.toolbar)

        // Ambil elemen drawer layout dan navigation view dari layout
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // ===== 🔹 Setup Navigation Component =====
        // Mengambil NavHostFragment untuk mengontrol perpindahan antar fragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // Mengatur AppBarConfiguration untuk menentukan destinasi utama (top-level)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_insight), // fragment utama
            drawerLayout
        )

        // Menghubungkan ActionBar dan Drawer dengan NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // ===== 🔹 Setup Bottom Navigation =====
        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)

        // ===== 🔹 Setup Floating Action Button =====
        val fab: FloatingActionButton = binding.fab
        fab.bringToFront() // memastikan FAB berada di atas layer lainnya
        fab.setOnClickListener {
            // Navigasi ke halaman tambah tugas (Add Task)
            navController.navigate(R.id.nav_addtask)
        }

        // ===== 🔹 Drawer Header Setup (Profil Pengguna) =====
        val headerView = navView.getHeaderView(0)
        val headerProfileContainer = headerView.findViewById<LinearLayout>(R.id.headerProfileContainer)
        val imageViewProfile = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)

        // Mengambil data pengguna dari SharedPreferences (session user)
        val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = sharedPref.getString("username", "Guest")
        val profileUrl = sharedPref.getString("profile_picture", null)

        // Menampilkan nama pengguna di header drawer
        tvUserName.text = username

        // Jika ada URL foto profil, tampilkan dengan Glide
        if (!profileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileUrl)
                .placeholder(R.drawable.ic_account_) // placeholder jika gambar belum dimuat
                .circleCrop() // membuat gambar berbentuk lingkaran
                .into(imageViewProfile)
        }

        // Klik header profile untuk menuju halaman akun pengguna
        headerProfileContainer.setOnClickListener {
            navController.navigate(R.id.nav_account)
            drawerLayout.closeDrawers()
        }

        // ===== 🔹 Drawer Menu Item Listener (Logout & Navigasi Menu) =====
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                // Jika pengguna memilih "Logout"
                R.id.nav_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Konfirmasi Logout")
                        .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
                        .setPositiveButton("Ya") { _, _ ->
                            // Hapus data session pengguna
                            val sharedPref = getSharedPreferences("user_session", MODE_PRIVATE)
                            sharedPref.edit().clear().apply()

                            // Arahkan kembali ke halaman login
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

                // Untuk menu lain, gunakan navigasi otomatis dari NavigationUI
                else -> {
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(menuItem, navController)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }

        // ===== 🔹 Kontrol Tampilan (Hide BottomNav & FAB di Halaman Login/Register) =====
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fab = binding.fab
            val bottomNav = binding.bottomNavigation
            val toolbar = binding.toolbar

            when (destination.id) {
                // Sembunyikan komponen navigasi di halaman login & register
                R.id.loginFragment, R.id.loginFormFragment, R.id.registerFragment -> {
                    supportActionBar?.hide()
                    fab.isVisible = false
                    bottomNav.visibility = View.GONE
                    toolbar.visibility = View.GONE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }

                // Tampilkan kembali komponen navigasi di halaman lain
                else -> {
                    supportActionBar?.show()
                    fab.isVisible = true
                    bottomNav.visibility = View.VISIBLE
                    toolbar.visibility = View.VISIBLE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }
    }

    /**
     * Fungsi ini menangani aksi saat tombol "Up" ditekan (navigasi ke atas/back)
     * sesuai konfigurasi AppBarConfiguration.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}