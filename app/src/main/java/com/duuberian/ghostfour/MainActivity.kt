package com.duuberian.ghostfour

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.Menu
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var slots: List<TextView>
    private lateinit var appsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("ghostfour", MODE_PRIVATE)
        appsContainer = findViewById(R.id.appsContainer)

        slots = listOf(
            findViewById(R.id.app1),
            findViewById(R.id.app2),
            findViewById(R.id.app3),
            findViewById(R.id.app4)
        )

        slots.forEachIndexed { index, textView ->
            textView.setOnClickListener { launchSlot(index) }
            textView.setOnLongClickListener {
                chooseAppForSlot(index)
                true
            }
        }

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val start = e1 ?: return false
                val dy = e2.y - start.y
                val dx = e2.x - start.x

                // Swipe down opens app search drawer
                if (dy > 120 && kotlin.math.abs(dy) > kotlin.math.abs(dx) && kotlin.math.abs(velocityY) > 400) {
                    startActivity(Intent(this@MainActivity, AppDrawerActivity::class.java).putExtra("focus_search", true))
                    return true
                }
                return false
            }
        })

        findViewById<android.view.View>(R.id.root).setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLabels()
        applyPlacement()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1001, 0, "Layout")
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == 1001) {
            showPlacementMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPlacementMenu() {
        val positions = arrayOf("Center", "Top Left", "Top Right", "Bottom Left", "Bottom Right")
        AlertDialog.Builder(this)
            .setTitle("App placement")
            .setItems(positions) { _, which ->
                prefs.edit().putInt("placement", which).apply()
                applyPlacement()
            }
            .setNeutralButton("Alignment") { _, _ -> showAlignmentMenu() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAlignmentMenu() {
        val alignments = arrayOf("Center", "Start", "End")
        AlertDialog.Builder(this)
            .setTitle("Text alignment")
            .setItems(alignments) { _, which ->
                prefs.edit().putInt("alignment", which).apply()
                applyPlacement()
            }
            .show()
    }

    private fun applyPlacement() {
        val placement = prefs.getInt("placement", 0)
        val alignment = prefs.getInt("alignment", 0)

        val gravity = when (placement) {
            1 -> Gravity.TOP or Gravity.START
            2 -> Gravity.TOP or Gravity.END
            3 -> Gravity.BOTTOM or Gravity.START
            4 -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.CENTER
        }
        appsContainer.gravity = gravity

        val textGravity = when (alignment) {
            1 -> Gravity.START
            2 -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        slots.forEach { it.gravity = textGravity }
    }

    private fun refreshLabels() {
        slots.forEachIndexed { index, textView ->
            val pkg = prefs.getString("slot_$index", null)
            textView.text = if (pkg == null) "Set App ${index + 1}" else appLabel(pkg)
        }
    }

    private fun launchSlot(index: Int) {
        val pkg = prefs.getString("slot_$index", null) ?: return
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    private fun chooseAppForSlot(index: Int) {
        val apps = loadLaunchableApps()
        val labels = apps.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Pick app for slot ${index + 1}")
            .setItems(labels) { _, which ->
                prefs.edit().putString("slot_$index", apps[which].second).apply()
                refreshLabels()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadLaunchableApps(): List<Pair<String, String>> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        return pm.queryIntentActivities(intent, 0)
            .map {
                val label = it.loadLabel(pm).toString()
                val pkg = it.activityInfo.packageName
                label to pkg
            }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }

    private fun appLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            "Missing app"
        }
    }
}
