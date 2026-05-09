package com.duuberian.ghostfour

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class AppDrawerActivity : AppCompatActivity() {

    private lateinit var allApps: List<Pair<String, String>>
    private lateinit var filteredApps: MutableList<Pair<String, String>>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_drawer)

        val appsList = findViewById<ListView>(R.id.appsList)
        val searchInput = findViewById<EditText>(R.id.searchInput)

        allApps = loadLaunchableApps()
        filteredApps = allApps.toMutableList()

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filteredApps.map { it.first }
        )
        appsList.adapter = adapter

        appsList.setOnItemClickListener { _, _, position, _ ->
            val pkg = filteredApps[position].second
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                filteredApps.clear()
                filteredApps.addAll(
                    if (q.isEmpty()) allApps else allApps.filter { it.first.lowercase().contains(q) }
                )
                adapter.clear()
                adapter.addAll(filteredApps.map { it.first })
                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun loadLaunchableApps(): List<Pair<String, String>> {
        val pm = packageManager
        val installed = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))

        return installed.mapNotNull { appInfo ->
            val pkg = appInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                val label = pm.getApplicationLabel(appInfo).toString()
                label to pkg
            } else null
        }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
}
