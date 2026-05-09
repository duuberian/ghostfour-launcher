package com.duuberian.ghostfour

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var slots: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("ghostfour", MODE_PRIVATE)

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
    }

    override fun onResume() {
        super.onResume()
        refreshLabels()
    }

    private fun refreshLabels() {
        slots.forEachIndexed { index, textView ->
            val pkg = prefs.getString("slot_$index", null)
            textView.text = if (pkg == null) {
                "Set App ${index + 1}"
            } else {
                appLabel(pkg)
            }
        }
    }

    private fun launchSlot(index: Int) {
        val pkg = prefs.getString("slot_$index", null) ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
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
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

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
