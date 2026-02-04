@file:Suppress("DEPRECATION")

package com.ayamq.kiosk.ui
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import android.graphics.Color
import android.graphics.Typeface
import com.ayamq.kiosk.R
import com.ayamq.kiosk.broadcast.OrderStatusReceiver
import com.ayamq.kiosk.data.database.entity.MenuEntity
import com.ayamq.kiosk.data.database.entity.OrderEntity
import com.ayamq.kiosk.model.Category
import com.ayamq.kiosk.scheduler.BackupScheduler
import com.ayamq.kiosk.scheduler.OrderCheckAlarmReceiver
import com.ayamq.kiosk.service.OrderMonitorService
import com.ayamq.kiosk.ui.adapter.CartAdapter
import com.ayamq.kiosk.ui.adapter.MenuAdapter
import com.ayamq.kiosk.ui.viewmodel.MenuViewModel
import com.ayamq.kiosk.ui.viewmodel.OrderCreationResult
import com.ayamq.kiosk.ui.viewmodel.OrderViewModel
import com.ayamq.kiosk.util.FormatUtils
import com.ayamq.kiosk.util.NetworkUtils
import com.ayamq.kiosk.util.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Main Activity for AyamQ Kiosk
 * Handles:
 * - Customer menu browsing and ordering
 * - Admin access with PIN authentication
 * - Cart management
 * - Background service integration
 */
class MainActivity : AppCompatActivity() {

    // ViewModels
    private lateinit var menuViewModel: MenuViewModel
    private lateinit var orderViewModel: OrderViewModel

    // Adapters
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var cartAdapter: CartAdapter

    // UI Components
    private lateinit var categoryButtons: List<Button>
    private lateinit var tvCategoryTitle: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var btnOrderNow: Button
    private lateinit var btnAdmin: ImageButton

    // Utils
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var orderStatusReceiver: OrderStatusReceiver

    // Admin mode flag
    private var isAdminMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize utilities
        preferenceManager = PreferenceManager(this)

        // Initialize ViewModels
        menuViewModel = ViewModelProvider(this)[MenuViewModel::class.java]
        orderViewModel = ViewModelProvider(this)[OrderViewModel::class.java]

        // Setup UI
        initializeViews()
        setupCategoryButtons()
        setupMenuRecyclerView()
        setupCartRecyclerView()
        setupOrderButton()
        setupAdminButton()

        // Setup observers
        setupObservers()

        // Start background services
        startBackgroundServices()

        // Register broadcast receiver
        registerOrderStatusReceiver()

        // Check network status
        checkNetworkStatus()
    }

    /**
     * Initialize all view references
     */
    private fun initializeViews() {
        // Category buttons
        val btnPaket = findViewById<Button>(R.id.btn_category_paket)
        val btnAlaCarte = findViewById<Button>(R.id.btn_category_ala_carte)
        val btnSauce = findViewById<Button>(R.id.btn_category_sauce)
        val btnDrink = findViewById<Button>(R.id.btn_category_drink)

        categoryButtons = listOf(btnPaket, btnAlaCarte, btnSauce, btnDrink)

        // Other UI elements
        tvCategoryTitle = findViewById(R.id.tv_category_title)
        tvTotalPrice = findViewById(R.id.tv_total_price)
        btnOrderNow = findViewById(R.id.btn_order_now)
        btnAdmin = findViewById(R.id.btn_admin)
    }

    /**
     * Setup category button click listeners
     */
    private fun setupCategoryButtons() {
        categoryButtons[0].setOnClickListener {
            selectCategory(Category.PAKET, 0)
        }
        categoryButtons[1].setOnClickListener {
            selectCategory(Category.ALA_CARTE, 1)
        }
        categoryButtons[2].setOnClickListener {
            selectCategory(Category.SAUCE, 2)
        }
        categoryButtons[3].setOnClickListener {
            selectCategory(Category.DRINK, 3)
        }

        // Select first category by default
        selectCategory(Category.PAKET, 0)
    }

    /**
     * Select a category and update UI
     */
    private fun selectCategory(category: Category, buttonIndex: Int) {
        // Update ViewModel
        menuViewModel.selectCategory(category)

        // Update category title
        tvCategoryTitle.text = category.displayName

        // Update button backgrounds
        categoryButtons.forEachIndexed { index, button ->
            if (index == buttonIndex) {
                button.setBackgroundColor(resources.getColor(R.color.accent_green, theme))
            } else {
                button.setBackgroundColor(resources.getColor(R.color.secondary, theme))
            }
        }
    }

    /**
     * Setup menu RecyclerView with GridLayoutManager
     */
    private fun setupMenuRecyclerView() {
        val rvMenu = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_menu)

        // Create adapter with click handler
        menuAdapter = MenuAdapter(
            onItemClick = { menuItem ->
                // Add to cart when "Tambah" button is clicked
                menuViewModel.addToCart(menuItem)
                Toast.makeText(this, "${menuItem.name} ditambahkan", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = if (isAdminMode) {
                { menuItem ->
                    // Long click in admin mode deletes item
                    showDeleteMenuDialog(menuItem)
                    true
                }
            } else null
        )

        // Setup RecyclerView
        rvMenu.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = menuAdapter
        }
    }

    /**
     * Setup cart RecyclerView
     */
    private fun setupCartRecyclerView() {
        val rvCart = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_cart)

        // Create adapter with quantity control handlers
        cartAdapter = CartAdapter(
            onIncrease = { cartItem ->
                menuViewModel.increaseQuantity(cartItem)
            },
            onDecrease = { cartItem ->
                menuViewModel.decreaseQuantity(cartItem)
            }
        )

        // Setup RecyclerView
        rvCart.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = cartAdapter
        }
    }

    /**
     * Setup order button click listener
     */
    private fun setupOrderButton() {
        btnOrderNow.setOnClickListener {
            val cartItems = menuViewModel.getCartItemsList()
            val totalPrice = menuViewModel.totalPrice.value ?: 0

            if (cartItems.isEmpty()) {
                Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create order
            orderViewModel.createOrder(cartItems, totalPrice)
        }
    }

    /**
     * Setup admin button click listener
     */
    private fun setupAdminButton() {
        btnAdmin.setOnClickListener {
            showAdminPinDialog()
        }
    }

    /**
     * Setup LiveData observers
     */
    private fun setupObservers() {
        // Observe filtered menu items
        menuViewModel.filteredMenuItems.observe(this) { menuItems ->
            menuAdapter.submitList(menuItems)
        }

        // Observe cart items
        menuViewModel.cartItems.observe(this) { cartItems ->
            cartAdapter.submitList(cartItems)
        }

        // Observe total price
        menuViewModel.totalPrice.observe(this) { totalPrice ->
            tvTotalPrice.text = FormatUtils.formatPrice(totalPrice)
        }

        // Observe order creation status
        orderViewModel.orderCreationStatus.observe(this) { result ->
            when (result) {
                is OrderCreationResult.Success -> {
                    Toast.makeText(this, R.string.order_success, Toast.LENGTH_LONG).show()
                    // Clear cart after successful order
                    menuViewModel.clearCart()
                    orderViewModel.resetOrderCreationStatus()
                }
                is OrderCreationResult.Error -> {
                    Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                    orderViewModel.resetOrderCreationStatus()
                }
                null -> {
                    // No action needed
                }
            }
        }
    }

    /**
     * Start background services for order monitoring
     */
    private fun startBackgroundServices() {
        try {
            // Start OrderMonitorService
            val serviceIntent = Intent(this, OrderMonitorService::class.java)
            startService(serviceIntent)

            // Schedule AlarmManager for periodic checks
            OrderCheckAlarmReceiver.scheduleAlarm(this)

            // Schedule JobScheduler for backup
            BackupScheduler.scheduleBackupJob(this)

            Log.d("MainActivity", "Background services started successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting background services", e)
            Toast.makeText(this, "Some background features may not work", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Register BroadcastReceiver for order status updates
     */
    private fun registerOrderStatusReceiver() {
        orderStatusReceiver = OrderStatusReceiver()
        orderStatusReceiver.setListener(object : OrderStatusReceiver.OrderStatusListener {
            override fun onOrderStatusChanged(orderCount: Int) {
                runOnUiThread {
                    // Update UI based on order status change
                    // For example, show notification or update order list
                    // This is already handled by LiveData, but you can add additional logic here
                }
            }
        })

        val filter = IntentFilter("com.ayamq.kiosk.ORDER_STATUS_CHANGED")

        // Register receiver with appropriate flags based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit flag
            registerReceiver(orderStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            // Older Android versions
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(orderStatusReceiver, filter)
        }
    }

    /**
     * Check network status and log
     */
    private fun checkNetworkStatus() {
        val isConnected = NetworkUtils.isNetworkAvailable(this)
        val networkType = NetworkUtils.getNetworkType(this)
        Log.d("MainActivity", "Network: $networkType, Connected: $isConnected")
    }

    // ==================== ADMIN FUNCTIONS ====================

    /**
     * Show admin PIN entry dialog
     */
    private fun showAdminPinDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_pin, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // PIN entry handling
        var enteredPin = ""
        val pinIndicators = listOf(
            dialogView.findViewById<View>(R.id.pin_indicator_1),
            dialogView.findViewById<View>(R.id.pin_indicator_2),
            dialogView.findViewById<View>(R.id.pin_indicator_3),
            dialogView.findViewById<View>(R.id.pin_indicator_4)
        )

        // Number button click listeners
        val numberButtons = listOf(
            dialogView.findViewById<Button>(R.id.btn_num_0),
            dialogView.findViewById<Button>(R.id.btn_num_1),
            dialogView.findViewById<Button>(R.id.btn_num_2),
            dialogView.findViewById<Button>(R.id.btn_num_3),
            dialogView.findViewById<Button>(R.id.btn_num_4),
            dialogView.findViewById<Button>(R.id.btn_num_5),
            dialogView.findViewById<Button>(R.id.btn_num_6),
            dialogView.findViewById<Button>(R.id.btn_num_7),
            dialogView.findViewById<Button>(R.id.btn_num_8),
            dialogView.findViewById<Button>(R.id.btn_num_9)
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (enteredPin.length < 4) {
                    enteredPin += index.toString()
                    updatePinIndicators(pinIndicators, enteredPin.length)

                    // Check PIN when 4 digits entered
                    if (enteredPin.length == 4) {
                        if (preferenceManager.verifyAdminPin(enteredPin)) {
                            dialog.dismiss()
                            enterAdminMode()
                        } else {
                            Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
                            enteredPin = ""
                            updatePinIndicators(pinIndicators, 0)
                        }
                    }
                }
            }
        }

        // Backspace button
        dialogView.findViewById<Button>(R.id.btn_backspace).setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
                updatePinIndicators(pinIndicators, enteredPin.length)
            }
        }

        // Clear button
        dialogView.findViewById<Button>(R.id.btn_clear).setOnClickListener {
            enteredPin = ""
            updatePinIndicators(pinIndicators, 0)
        }

        // Cancel button
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Update PIN indicator UI
     */
    private fun updatePinIndicators(indicators: List<View>, filledCount: Int) {
        indicators.forEachIndexed { index, view ->
            if (index < filledCount) {
                view.setBackgroundResource(R.drawable.pin_indicator_filled)
            } else {
                view.setBackgroundResource(R.drawable.pin_indicator_empty)
            }
        }
    }

    /**
     * Enter admin mode
     */
    private fun enterAdminMode() {
        isAdminMode = true
        Toast.makeText(this, "Admin Mode Activated", Toast.LENGTH_SHORT).show()

        // Show admin options dialog
        showAdminOptionsDialog()
    }

    /**
     * Show admin options menu
     */
    private fun showAdminOptionsDialog() {
        val options = arrayOf(
            "Add Menu Item",
            "Remove Menu Item",
            "Restore Menu Item",
            "Delete All Menu Items",
            "Backup Data",
            "View Orders",
            "Exit Admin Mode"
        )

        AlertDialog.Builder(this)
            .setTitle("Admin Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddMenuDialog()
                    1 -> showDeleteMenuCategoryDialog()
                    2 -> showRestoreMenuCategoryDialog()
                    3 -> showDeleteAllMenuItemsDialog()
                    4 -> showBackupDataDialog()
                    5 -> showOrdersDialog()
                    6 -> exitAdminMode()
                }
            }
            .setOnCancelListener {
                exitAdminMode()
            }
            .show()
    }

    /**
     * Show category selection dialog for deleting menu items
     */
    private fun showDeleteMenuCategoryDialog() {
        val categories = Category.entries.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                val selectedCategory = Category.entries[which]
                showDeleteMenuItemsDialog(selectedCategory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show confirmation dialog for deleting all menu items
     */
    private fun showDeleteAllMenuItemsDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Delete All Menu Items")
            .setMessage(
                "Are you absolutely sure?\n\n" +
                        "This will PERMANENTLY delete ALL menu items from the database.\n\n" +
                        "• All categories will be empty\n" +
                        "• This action CANNOT be undone\n" +
                        "• You will need to add menu items again\n\n" +
                        "Type DELETE to confirm:"
            )
            .setView(EditText(this).apply {
                hint = "Type DELETE here"
                id = android.R.id.edit
            })
            .setPositiveButton("Delete All") { dialog, _ ->
                val input = (dialog as AlertDialog).findViewById<EditText>(android.R.id.edit)
                if (input?.text.toString() == "DELETE") {
                    performDeleteAllMenuItems()
                } else {
                    Toast.makeText(
                        this,
                        "Confirmation text incorrect. Deletion cancelled.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform the delete all operation
     */
    private fun performDeleteAllMenuItems() {
        menuViewModel.deleteAllMenuItems()

        Toast.makeText(
            this,
            "All menu items deleted. Please add new items in the Admin Menu.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Show backup data dialog with options
     */
    private fun showBackupDataDialog() {
        val options = arrayOf(
            "Backup Now (Local + Upload)",
            "Backup to Local Only",
        )

        AlertDialog.Builder(this)
            .setTitle("Backup Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> performBackupWithUpload()
                    1 -> performLocalBackupOnly()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform backup and upload to cPanel
     */
    private fun performBackupWithUpload() {
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("Backing up data and uploading...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch {
            try {
                // 1️⃣ BACKUP (boleh di main, isinya IO coroutine)
                com.ayamq.kiosk.util.BackupEngine
                    .performBackup(this@MainActivity)

                // 2️⃣ UPLOAD (PINDAHKAN KE IO THREAD)
                val uploadResult = withContext(Dispatchers.IO) {
                    com.ayamq.kiosk.util.BackupUploader
                        .uploadAllBackups(this@MainActivity)
                }

                progressDialog.dismiss()

                // 3️⃣ TAMPILKAN HASIL
                when {
                    uploadResult.allSuccess -> {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("✅ Backup Berhasil")
                            .setMessage(
                                "Data berhasil dibackup dan diupload ke server!\n\n" +
                                        "✅ Menu items\n" +
                                        "✅ Orders\n\n" +
                                        "Data Anda aman tersimpan di cPanel hosting."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }

                    uploadResult.anySuccess -> {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠️ Upload Sebagian")
                            .setMessage(
                                "Backup lokal berhasil, tapi upload sebagian:\n\n" +
                                        "${if (uploadResult.menuSuccess) "✅" else "❌"} Menu items\n" +
                                        "${if (uploadResult.ordersSuccess) "✅" else "❌"} Orders\n\n" +
                                        "Data tetap tersimpan di perangkat."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }

                    else -> {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("⚠️ Upload Gagal")
                            .setMessage(
                                "Backup lokal berhasil, tapi gagal upload ke server.\n\n" +
                                        "Periksa koneksi internet atau konfigurasi server.\n\n" +
                                        "Data tetap tersimpan di perangkat."
                            )
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

            } catch (e: Exception) {
                progressDialog.dismiss()

                Log.e("BACKUP_UI", "Backup/upload error", e)

                Toast.makeText(
                    this@MainActivity,
                    "Backup selesai, tapi terjadi kesalahan saat upload",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }



    /**
     * Perform local backup only (no upload)
     */
    private fun performLocalBackupOnly() {
        Toast.makeText(this, "Backing up locally...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                BackupScheduler.triggerBackupNow(this@MainActivity)

                kotlinx.coroutines.delay(1500)

                val hasBackup = BackupScheduler.hasBackupFiles(this@MainActivity)

                if (hasBackup) {
                    Toast.makeText(
                        this@MainActivity,
                        "✅ Backup lokal berhasil!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "⚠️ Backup mungkin belum selesai",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show menu items for deletion by category
     */
    @SuppressLint("SetTextI18n")
    private fun showDeleteMenuItemsDialog(category: Category) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val titleTextView = TextView(this).apply {
            text = "Delete Items from ${category.displayName}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleTextView)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800
            )
        }

        val itemsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Observe menu items for selected category
        menuViewModel.getMenuItemsByCategory(category.dbValue).observe(this) { menuItems ->
            itemsLayout.removeAllViews()

            if (menuItems.isEmpty()) {
                itemsLayout.addView(TextView(this).apply {
                    text = "No items in this category"
                    textSize = 16f
                    setPadding(16, 32, 16, 32)
                    gravity = Gravity.CENTER
                })
            } else {
                menuItems.forEach { menuItem ->
                    val itemLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(16, 12, 16, 12)
                        gravity = Gravity.CENTER_VERTICAL
                        setBackgroundColor(resources.getColor(R.color.background, theme))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 8)
                        }
                    }

                    // Menu item info
                    val infoLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    infoLayout.addView(TextView(this).apply {
                        text = menuItem.name
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(resources.getColor(R.color.text_primary, theme))
                    })

                    infoLayout.addView(TextView(this).apply {
                        text = FormatUtils.formatPrice(menuItem.price)
                        textSize = 14f
                        setTextColor(resources.getColor(R.color.accent_red, theme))
                    })

                    itemLayout.addView(infoLayout)

                    // Delete button
                    val deleteButton = Button(this).apply {
                        text = "Delete"
                        setBackgroundColor(resources.getColor(R.color.accent_red, theme))
                        setTextColor(resources.getColor(R.color.white, theme))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener {
                            confirmDeleteMenuItem(menuItem)
                        }
                    }

                    itemLayout.addView(deleteButton)
                    itemsLayout.addView(itemLayout)
                }
            }
        }

        scrollView.addView(itemsLayout)
        layout.addView(scrollView)

        AlertDialog.Builder(this)
            .setView(layout)
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Confirm deletion of menu item
     */
    private fun confirmDeleteMenuItem(menuItem: MenuEntity) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Remove")
            .setMessage("Remove \"${menuItem.name}\"?\n\nThis item will be hidden from the menu but can be restored later.")
            .setPositiveButton("Delete") { _, _ ->
                // ViewModel handles coroutine internally
                menuViewModel.deleteMenuItem(menuItem)
                Toast.makeText(
                    this,
                    "\"${menuItem.name}\" Remove (can be restored)",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show category selection dialog for restoring menu items
     */
    private fun showRestoreMenuCategoryDialog() {
        val categories = Category.entries.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Category to Restore")
            .setItems(categories) { _, which ->
                val selectedCategory = Category.entries[which]
                showRestoreMenuItemsDialog(selectedCategory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show deleted menu items for restoration by category
     */
    @SuppressLint("SetTextI18n")
    private fun showRestoreMenuItemsDialog(category: Category) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val titleTextView = TextView(this).apply {
            text = "Restore Items from ${category.displayName}"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleTextView)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800
            )
        }

        val itemsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Observe unavailable menu items for selected category
        menuViewModel.getUnavailableMenuItemsByCategory(category.dbValue).observe(this) { menuItems ->
            itemsLayout.removeAllViews()

            if (menuItems.isEmpty()) {
                itemsLayout.addView(TextView(this).apply {
                    text = "No deleted items in this category"
                    textSize = 16f
                    setPadding(16, 32, 16, 32)
                    gravity = Gravity.CENTER
                    setTextColor(resources.getColor(R.color.text_secondary, theme))
                })
            } else {
                menuItems.forEach { menuItem ->
                    val itemLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(16, 12, 16, 12)
                        gravity = Gravity.CENTER_VERTICAL
                        setBackgroundColor(resources.getColor(R.color.background, theme))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, 8)
                        }
                    }

                    // Menu item info
                    val infoLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    infoLayout.addView(TextView(this).apply {
                        text = menuItem.name
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(resources.getColor(R.color.text_primary, theme))
                    })

                    infoLayout.addView(TextView(this).apply {
                        text = FormatUtils.formatPrice(menuItem.price)
                        textSize = 14f
                        setTextColor(resources.getColor(R.color.accent_red, theme))
                    })

                    infoLayout.addView(TextView(this).apply {
                        text = "Removed"
                        textSize = 12f
                        setTextColor(resources.getColor(R.color.text_secondary, theme))
                        setTypeface(null, Typeface.ITALIC)
                    })

                    itemLayout.addView(infoLayout)

                    // Restore button
                    val restoreButton = Button(this).apply {
                        text = "Restore"
                        setBackgroundColor(resources.getColor(R.color.accent_green, theme))
                        setTextColor(resources.getColor(R.color.white, theme))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener {
                            confirmRestoreMenuItem(menuItem)
                        }
                    }

                    itemLayout.addView(restoreButton)
                    itemsLayout.addView(itemLayout)
                }
            }
        }

        scrollView.addView(itemsLayout)
        layout.addView(scrollView)

        AlertDialog.Builder(this)
            .setView(layout)
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Confirm restoration of menu item
     */
    private fun confirmRestoreMenuItem(menuItem: MenuEntity) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Restore")
            .setMessage("Restore \"${menuItem.name}\"?\n\nThis item will be available in the menu again.")
            .setPositiveButton("Restore") { _, _ ->
                // ViewModel handles coroutine internally
                menuViewModel.restoreMenuItem(menuItem.id)
                Toast.makeText(
                    this,
                    "\"${menuItem.name}\" restored successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Checks for the backup file and shows a confirmation dialog before importing the menu.
     */
    private fun showImportMenuFromJSON() {
        val backupFile = File(filesDir, "menu_backup.json")

        // First, check if the backup file actually exists.
        if (!backupFile.exists()) {
            Toast.makeText(
                this,
                "'menu_backup.json' tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()
            return // Stop if no file is found.
        }

        // If the file exists, show the confirmation dialog.
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Import Menu")
            .setMessage(
                "Anda yakin ingin import menu dari 'menu_backup.json'?\n\n" +
                        "⚠️ SEMUA menu yang ada saat ini akan DIHAPUS dan digantikan dengan data dari backup."
            )
            .setPositiveButton("Import") { _, _ ->
                // If the user confirms, call the function that does the actual work.
                importMenuFromJson()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Import Menu Items from JSON
     */
    private fun importMenuFromJson() {
        lifecycleScope.launch {
            try {
                val file = File(filesDir, "menu_backup.json")

                val json = withContext(Dispatchers.IO) {
                    file.readText()
                }

                val type = object : TypeToken<List<MenuEntity>>() {}.type
                val menus: List<MenuEntity> = Gson().fromJson(json, type)

                if (menus.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "File backup kosong",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // 🔥 INI INTI NYA
                menuViewModel.insertMenuItems(menus)

                Toast.makeText(
                    this@MainActivity,
                    "Berhasil import ${menus.size} menu",
                    Toast.LENGTH_LONG
                ).show()

            } catch (_: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Import gagal",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show dialog to add new menu item
     */
    @SuppressLint("SetTextI18n")
    private fun showAddMenuDialog() {
        // Create custom layout programmatically for simplicity
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val nameEditText = EditText(this).apply {
            hint = "Menu Name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        val priceEditText = EditText(this).apply {
            hint = "Price (Rupiah)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val categorySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                Category.entries.map { it.displayName }
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Add New Menu Item"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val importButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_upload)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Import Menu from JSON"
            setOnClickListener {
                showImportMenuFromJSON()
            }
        }

        header.addView(title)
        header.addView(importButton)
        layout.addView(header)

        layout.addView(nameEditText)
        layout.addView(priceEditText)
        layout.addView(TextView(this).apply {
            text = "Category:"
            setPadding(0, 24, 0, 8)
        })
        layout.addView(categorySpinner)

        AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val priceStr = priceEditText.text.toString()
                val category = Category.entries[categorySpinner.selectedItemPosition]

                if (name.isNotBlank() && priceStr.isNotBlank()) {
                    val price = priceStr.toIntOrNull() ?: 0
                    val newMenuItem = MenuEntity(
                        name = name,
                        price = price,
                        category = category.dbValue
                    )
                    menuViewModel.insertMenuItem(newMenuItem)
                    Toast.makeText(this, "Menu item added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show orders list dialog
     */
    @SuppressLint("SetTextI18n")
    private fun showOrdersDialog() {
        val options = arrayOf(
            "View Pending Orders",
            "View Orders History"
        )

        AlertDialog.Builder(this)
            .setTitle("Orders")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPendingOrdersDialog()
                    1 -> showOrdersHistoryDialog()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showPendingOrdersDialog() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val scrollView = ScrollView(this)
        val ordersLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        orderViewModel.pendingOrders.observe(this) { orders ->
            ordersLayout.removeAllViews()

            if (orders.isEmpty()) {
                ordersLayout.addView(TextView(this).apply {
                    text = "No pending orders"
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                })
            } else {
                orders.forEach { order ->
                    val orderView = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        setBackgroundColor(resources.getColor(R.color.background, theme))
                    }

                    orderView.addView(TextView(this).apply {
                        text = "Order #${order.queueNumber}"
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                    })

                    orderView.addView(TextView(this).apply {
                        text = FormatUtils.formatDateTime(order.timestamp)
                        textSize = 12f
                    })

                    orderViewModel.getOrderItems(order.orderId)
                        .observe(this) { items ->
                            items.forEach { item ->
                                orderView.addView(TextView(this).apply {
                                    text = "${item.quantity}x ${item.itemName} - ${
                                        FormatUtils.formatPrice(item.quantity * item.pricePerUnit)
                                    }"
                                })
                            }
                        }

                    orderView.addView(TextView(this).apply {
                        text = "Total: ${FormatUtils.formatPrice(order.totalPrice)}"
                        setTypeface(null, Typeface.BOLD)
                    })

                    orderView.addView(Button(this).apply {
                        text = "Complete Order"
                        setOnClickListener {
                            orderViewModel.completeOrder(order.orderId)
                            Toast.makeText(
                                this@MainActivity,
                                "Order #${order.queueNumber} completed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })

                    ordersLayout.addView(orderView)
                }
            }
        }

        scrollView.addView(ordersLayout)
        layout.addView(scrollView)

        AlertDialog.Builder(this)
            .setTitle("Pending Orders")
            .setView(layout)
            .setPositiveButton("Close", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showOrdersHistoryDialog() {

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 24, 24, 16)
        }

        val title = TextView(this).apply {
            text = "Orders History"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val restoreButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_rotate) // ikon restore bawaan
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Restore orders history"


            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 24
            }

            setOnClickListener {
                showRestoreOrdersDialog()
            }
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Delete orders history"
            setOnClickListener {
                showDeleteAllOrdersDialog()
            }
        }

        header.addView(title)
        header.addView(restoreButton)
        header.addView(deleteButton)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val revenueText = TextView(this).apply {
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.accent_green, theme))
            setPadding(0, 0, 0, 16)
        }

        orderViewModel.totalRevenue.observe(this) { revenue ->
            revenueText.text = "Total Revenue: ${FormatUtils.formatPrice(revenue)}"
        }

        val scrollView = ScrollView(this)
        val ordersLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        orderViewModel.completedOrders.observe(this) { orders ->
            ordersLayout.removeAllViews()

            if (orders.isEmpty()) {
                ordersLayout.addView(TextView(this).apply {
                    text = "No order history"
                    textSize = 16f
                })
            } else {
                orders.forEach { order ->
                    ordersLayout.addView(TextView(this).apply {
                        text =
                            "Order ID: ${order.orderId} • ${
                                FormatUtils.formatDateTime(order.timestamp)
                            }\nTotal: ${FormatUtils.formatPrice(order.totalPrice)}"
                        setPadding(0, 8, 0, 8)
                    })
                }
            }
        }

        scrollView.addView(ordersLayout)

        layout.addView(revenueText)
        layout.addView(scrollView)

        AlertDialog.Builder(this)
            .setCustomTitle(header)
            .setView(layout)
            .setPositiveButton("Close", null)
            .show()
    }

    /**
     * Restore Order History from JSON
     */
    private fun showRestoreOrdersDialog() {
        val filesDir = this.filesDir
        // Assume a single, default backup file name.
        val backupFile = File(filesDir, "orders_backup.json")

        // If the backup file is not found, show a message and stop.
        if (!backupFile.exists()) {
            Toast.makeText(
                this,
                "File backup 'orders_backup.json' tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // If the file exists, go directly to the confirmation dialog.
        confirmRestoreOrders(backupFile)
    }

    private fun confirmRestoreOrders(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Restore")
            .setMessage(
                "Restore order history dari file:\n\n${file.name}\n\n" +
                        "Semua data order saat ini akan diganti."
            )
            .setPositiveButton("Restore") { _, _ ->
                restoreOrdersFromJson(file)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun restoreOrdersFromJson(backupFile: File) {
        lifecycleScope.launch {
            try {
                // 2. Check if the PASSED file exists, instead of the hardcoded one.
                if (!backupFile.exists()) {
                    Toast.makeText(
                        this@MainActivity,
                        "${backupFile.name} tidak ditemukan", // Use the actual filename in the message
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // 3. Read the text from the PASSED file.
                val json = withContext(Dispatchers.IO) {
                    backupFile.readText()
                }

                val type = object : TypeToken<List<OrderEntity>>() {}.type
                val orders: List<OrderEntity> =
                    Gson().fromJson(json, type)

                if (orders.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "File backup kosong",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // 🔥 RESET + RESTORE
                orderViewModel.deleteAllOrders()

                orders.forEach { order ->
                    orderViewModel.insertRestoredOrder(order)
                }

                Toast.makeText(
                    this@MainActivity,
                    "Berhasil restore ${orders.size} order dari ${backupFile.name}", // Improved success message
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Restore gagal: ${e.message}", // Show exception message for better debugging
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show confirmation dialog for deleting all orders
     */
    private fun showDeleteAllOrdersDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Delete All Orders")
            .setMessage(
                "Are you absolutely sure?\n\n" +
                        "This will PERMANENTLY delete ALL orders history.\n\n" +
                        "• Order history will be cleared\n" +
                        "• This action CANNOT be undone\n\n" +
                        "Type DELETE to confirm:"
            )
            .setView(EditText(this).apply {
                hint = "Type DELETE here"
                id = android.R.id.edit
            })
            .setPositiveButton("Delete All") { dialog, _ ->
                val input =
                    (dialog as AlertDialog).findViewById<EditText>(android.R.id.edit)

                if (input?.text.toString() == "DELETE") {
                    performDeleteAllOrders()
                } else {
                    Toast.makeText(
                        this,
                        "Confirmation text incorrect. Deletion cancelled.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Perform delete all orders
     */
    private fun performDeleteAllOrders() {
        orderViewModel.deleteAllOrders()

        Toast.makeText(
            this,
            "All orders have been deleted.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Show dialog to confirm menu item deletion
     */
    private fun showDeleteMenuDialog(menuItem: MenuEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Menu Item")
            .setMessage("Delete ${menuItem.name}?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    menuViewModel.deleteMenuItem(menuItem)
                    Toast.makeText(this, "Menu item deleted", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "Cannot delete: Item in orders", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Exit admin mode
     */
    private fun exitAdminMode() {
        isAdminMode = false
        Toast.makeText(this, "Admin Mode Deactivated", Toast.LENGTH_SHORT).show()

        // Recreate adapter without long-click handler
        setupMenuRecyclerView()

        // Re-observe menu items
        menuViewModel.filteredMenuItems.observe(this) { menuItems ->
            menuAdapter.submitList(menuItems)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        try {
            unregisterReceiver(orderStatusReceiver)
        } catch (_: Exception) {
            // Receiver may not be registered
        }
    }
}