package com.example.assistivetouch

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class FloatingService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var menuView: View
    private lateinit var favoriteMenuView: View
    private lateinit var params: WindowManager.LayoutParams

    private var isMenuOpen = false
    private var isFavoriteMenuOpen = false
    private var screenWidth = 0

    // SharedPreferences untuk penyimpanan data favorit secara permanen
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("assistive_touch_prefs", Context.MODE_PRIVATE)
    }

    // Menyimpan package name aplikasi favorit (8 slot)
    private val favoriteApps = Array<String?>(8) { null }

    private val slotIds = arrayOf(
        R.id.btn_fav_1, R.id.btn_fav_2, R.id.btn_fav_3, 
        R.id.btn_fav_4, R.id.btn_fav_5, R.id.btn_fav_6, 
        R.id.btn_fav_7, R.id.btn_fav_8
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenWidth = size.x

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)
        menuView = LayoutInflater.from(this).inflate(R.layout.layout_floating_menu, null)
        favoriteMenuView = LayoutInflater.from(this).inflate(R.layout.layout_favorite_menu, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 150
            y = 500
        }

        windowManager.addView(floatingView, params)

        setupTouchAndClickListener()
        setupMenuActions()
        setupFavoriteMenuActions()

        // Load data favorit dari SharedPreferences dan perbarui tampilan UI
        loadFavoriteApps()
    }

    // --- LOGIKA SHAREDPREFERENCES ---

    private fun loadFavoriteApps() {
        val pm = packageManager
        for (i in 0 until 8) {
            val savedPkg = prefs.getString("fav_slot_$i", null)
            favoriteApps[i] = savedPkg

            if (savedPkg != null) {
                val slotView = favoriteMenuView.findViewById<View>(slotIds[i])
                val imgIcon = slotView.findViewById<ImageView>(R.id.img_fav_icon)
                val txtLabel = slotView.findViewById<TextView>(R.id.txt_fav_label)

                try {
                    val appInfo = pm.getApplicationInfo(savedPkg, 0)
                    val iconDrawable = pm.getApplicationIcon(appInfo)
                    val appName = pm.getApplicationLabel(appInfo).toString()

                    imgIcon?.setImageDrawable(iconDrawable)
                    txtLabel?.text = appName
                    txtLabel?.setTextColor(android.graphics.Color.WHITE)
                } catch (_: Exception) {
                    // Jika aplikasi sudah di-uninstall, reset slot tersebut
                    saveFavoriteApp(i, null)
                }
            }
        }
    }

    private fun saveFavoriteApp(slotIndex: Int, packageName: String?) {
        favoriteApps[slotIndex] = packageName
        if (packageName != null) {
            prefs.edit().putString("fav_slot_$slotIndex", packageName).apply()
        } else {
            prefs.edit().remove("fav_slot_$slotIndex").apply()
        }
    }

    // --- TOUCH & MENU LISTENERS ---

    private fun setupTouchAndClickListener() {
        val btnFloating = floatingView.findViewById<View>(R.id.btn_floating)

        btnFloating.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private val CLICK_THRESHOLD = 15

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = abs(event.rawX - initialTouchX)
                        val diffY = abs(event.rawY - initialTouchY)

                        if (diffX < CLICK_THRESHOLD && diffY < CLICK_THRESHOLD) {
                            toggleMenu()
                        } else {
                            val middle = screenWidth / 2
                            if (params.x < middle) {
                                params.x = 10
                            } else {
                                params.x = screenWidth - floatingView.width - 10
                            }
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun toggleMenu() {
        if (isFavoriteMenuOpen) closeFavoriteMenu()

        if (isMenuOpen) {
            windowManager.removeView(menuView)
            isMenuOpen = false
        } else {
            val menuParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                params.type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }
            windowManager.addView(menuView, menuParams)
            isMenuOpen = true
        }
    }

    private fun showFavoriteMenu() {
        if (isMenuOpen) {
            windowManager.removeView(menuView)
            isMenuOpen = false
        }
        val favParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            params.type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        windowManager.addView(favoriteMenuView, favParams)
        isFavoriteMenuOpen = true
    }

    private fun closeFavoriteMenu() {
        if (isFavoriteMenuOpen) {
            windowManager.removeView(favoriteMenuView)
            isFavoriteMenuOpen = false
        }
    }

    private fun setupMenuActions() {
        menuView.findViewById<View>(R.id.menu_outside_touch).setOnClickListener {
            if (isMenuOpen) toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_home).setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_HOME)
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_notification).setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_back).setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_BACK)
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_device).setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_control_center).setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_assistant).setOnClickListener {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { startActivity(intent) } catch (_: Exception) {}
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_screenshot).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_lock).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            toggleMenu()
        }

        menuView.findViewById<View>(R.id.btn_star).setOnClickListener {
            showFavoriteMenu()
        }
    }

    private fun setupFavoriteMenuActions() {
        favoriteMenuView.findViewById<View>(R.id.fav_outside_touch).setOnClickListener {
            closeFavoriteMenu()
        }

        favoriteMenuView.findViewById<View>(R.id.btn_fav_back_to_main).setOnClickListener {
            closeFavoriteMenu()
            toggleMenu()
        }

        for (i in slotIds.indices) {
            val slotView = favoriteMenuView.findViewById<View>(slotIds[i])

            slotView?.setOnClickListener {
                val pkg = favoriteApps[i]
                if (pkg == null) {
                    openAppPicker(i, slotIds[i])
                } else {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        closeFavoriteMenu()
                    }
                }
            }

            slotView?.setOnLongClickListener {
                if (favoriteApps[i] != null) {
                    showManageSlotDialog(i, slotIds[i])
                }
                true
            }
        }
    }

    private fun showManageSlotDialog(slotIndex: Int, slotId: Int) {
        val options = arrayOf("Ganti Aplikasi", "Hapus dari Favorit")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Kelola Aplikasi Favorit")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openAppPicker(slotIndex, slotId)
                1 -> removeFavoriteApp(slotIndex, slotId)
            }
        }

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        dialog.show()
    }

    private fun removeFavoriteApp(slotIndex: Int, slotId: Int) {
        saveFavoriteApp(slotIndex, null)

        val slotView = favoriteMenuView.findViewById<View>(slotId)
        val imgIcon = slotView?.findViewById<ImageView>(R.id.img_fav_icon)
        val txtLabel = slotView?.findViewById<TextView>(R.id.txt_fav_label)

        imgIcon?.setImageResource(R.drawable.ic_star)
        txtLabel?.text = "Tambah"
        txtLabel?.setTextColor(android.graphics.Color.parseColor("#8E8E93"))

        Toast.makeText(this, "Aplikasi dihapus dari Favorit", Toast.LENGTH_SHORT).show()
    }

    private fun openAppPicker(slotIndex: Int, slotId: Int) {
        val pm = packageManager

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { app -> app.loadLabel(pm).toString().lowercase() }

        val appNames = installedApps.map { it.loadLabel(pm).toString() }
        val packageNames = installedApps.map { it.packageName }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Aplikasi Favorit")
        builder.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, appNames)
        ) { _, which ->
            val selectedPackage = packageNames[which]
            val selectedName = appNames[which]

            saveFavoriteApp(slotIndex, selectedPackage)

            val slotView = favoriteMenuView.findViewById<View>(slotId)
            val imgIcon = slotView?.findViewById<ImageView>(R.id.img_fav_icon)
            val txtLabel = slotView?.findViewById<TextView>(R.id.txt_fav_label)

            try {
                val iconDrawable = pm.getApplicationIcon(selectedPackage)
                imgIcon?.setImageDrawable(iconDrawable)
            } catch (_: Exception) {}

            txtLabel?.text = selectedName
            txtLabel?.setTextColor(android.graphics.Color.WHITE)

            Toast.makeText(this, "$selectedName ditambahkan ke Favorit!", Toast.LENGTH_SHORT).show()
        }

        val dialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            @Suppress("DEPRECATION")
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_PHONE)
        }
        dialog.show()
    }

    // --- PEMBERSIHAN OVERLAY UNTUK ONDESTROY & ONUNBIND ---

    private fun removeAllViews() {
        try {
            if (::floatingView.isInitialized && floatingView.windowToken != null) {
                windowManager.removeView(floatingView)
            }
            if (isMenuOpen && ::menuView.isInitialized && menuView.windowToken != null) {
                windowManager.removeView(menuView)
                isMenuOpen = false
            }
            if (isFavoriteMenuOpen && ::favoriteMenuView.isInitialized && favoriteMenuView.windowToken != null) {
                windowManager.removeView(favoriteMenuView)
                isFavoriteMenuOpen = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        removeAllViews()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAllViews()
    }
}