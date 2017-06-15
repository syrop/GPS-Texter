/*
 * Copyright (C) 2017 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.org.seva.texter.view.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.webkit.WebView
import android.widget.Toast

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import org.apache.commons.io.IOUtils

import java.io.IOException
import java.util.ArrayList
import java.util.Locale

import javax.inject.Inject

import pl.org.seva.texter.R
import pl.org.seva.texter.presenter.utils.PermissionsUtils
import pl.org.seva.texter.presenter.utils.SmsSender
import pl.org.seva.texter.view.adapter.TitledPagerAdapter
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.view.fragment.HistoryFragment
import pl.org.seva.texter.view.fragment.StatsFragment
import pl.org.seva.texter.view.fragment.NavigationFragment
import pl.org.seva.texter.view.layout.SlidingTabLayout
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.Timer

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var smsSender: SmsSender
    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var permissionsUtils: PermissionsUtils
    @Inject
    lateinit var timer: Timer

    /** Used when counting a double click.  */
    private var clickTime: Long = 0
    /** Obtained from intent, may be null.  */
    private var action: String? = null
    private var shuttingDown: Boolean = false
    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        action = intent.action
        action?: finish()

        val graph = (application as TexterApplication).component
        graph.inject(this)

        setContentView(R.layout.activity_main)

        val titles = arrayOf<CharSequence>(getString(R.string.stats_tab_name),
                getString(R.string.map_tab_name),
                getString(R.string.history_tab_name))

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fragments = ArrayList<Fragment>()
        fragments.add(StatsFragment.newInstance())
        fragments.add(NavigationFragment.newInstance())
        fragments.add(HistoryFragment.newInstance())

        val adapter = TitledPagerAdapter(supportFragmentManager, titles).setItems(fragments)

        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = adapter

        val tabs = findViewById<SlidingTabLayout>(R.id.tabs)

        val tabColor: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tabColor = resources.getColor(R.color.tabsScrollColor, theme)
        } else {
            @Suppress("DEPRECATION")
            tabColor = resources.getColor(R.color.tabsScrollColor)
        }
        tabs.setDistributeEvenly()
        tabs.setCustomTabColorizer { tabColor }
        tabs.setViewPager(pager)

        smsSender.init(this, getString(R.string.speed_unit))

        val googlePlay = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (googlePlay != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, googlePlay, GOOGLE_REQUEST_CODE).show()
        }

        if (!showStartupDialog()) {
            processPermissions()
        }
        (application as TexterApplication).startService()
    }

    /**
     * All actions that require permissions must be placed here. The method performs them or
     * asks for permissions if they haven't been granted already.

     * @return true if all permissions had been granted before calling the method
     */
    private fun processPermissions(): Boolean {
        val permissionsToRequest = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsUtils
                    .permissionGrantedListener()
                    .filter { it.first == PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() }
        } else {
            initGps()
        }
        if (smsSender.isTextingEnabled) {
            permissionsToRequest.addAll(smsSender.permissionsToRequest())
        }
        if (permissionsToRequest.isEmpty()) {
            return true
        }
        val arr = permissionsToRequest.toTypedArray()
        ActivityCompat.requestPermissions(
                this,
                arr,
                PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID)

        return false
    }

    private fun initGps() {
        locationSource.initGpsOnLocationGranted(applicationContext)
    }

    private fun showStartupDialog(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(PREF_STARTUP_SHOWN, false)) {
            return false
        }
        dialog = Dialog(this)
        dialog!!.setCancelable(false)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_startup)

        val web = dialog.findViewById<WebView>(R.id.web)

        val language = Locale.getDefault().language
        web.settings.defaultTextEncodingName = "utf-8"

        try {
            val content = IOUtils.toString(
                    assets.open(if (language == "pl") "startup_pl.html" else "startup_en.html"),
                    "UTF-8")
                    .replace("[APP_VERSION]", versionName)
            web.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "UTF-8", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        dialog.findViewById<View>(R.id.dismiss).setOnClickListener {
            processPermissions()
            dialog.dismiss()
            prefs.edit().putBoolean(PREF_STARTUP_SHOWN, true).apply()  // asynchronously
        }
        dialog.findViewById<View>(R.id.settings).setOnClickListener {
            dialog.dismiss()
            prefs.edit().putBoolean(PREF_STARTUP_SHOWN, true).apply()
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        dialog.show()
        return true
    }

    private fun showHelpDialog() {
        dialog = Dialog(this)
        dialog!!.setCancelable(false)
        dialog!!.setContentView(R.layout.dialog_help)
        val web = dialog!!.findViewById<WebView>(R.id.web)
        web.settings.defaultTextEncodingName = "utf-8"

        val language = Locale.getDefault().language

        try {
            val content = IOUtils.toString(
                    assets.open(if (language == "pl") "help_pl.html" else "help_en.html"),
                    "UTF-8")
                    .replace("[APP_VERSION]", versionName)
            web.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "UTF-8", null)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        dialog!!.findViewById<View>(R.id.ok).setOnClickListener { dialog!!.dismiss() }
        dialog!!.show()
    }

    private val versionName: String
        get() {
            try {
                return packageManager.getPackageInfo(packageName, 0).versionName
            } catch (ex: PackageManager.NameNotFoundException) {
                return getString(R.string.app_version_unknown)
            }

        }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        // If request is cancelled, the result arrays are empty.
        permissionsUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    public override fun onDestroy() {
        // Also called when the screen is rotated.
        dialog?.dismiss()
        if (action == Intent.ACTION_MAIN && shuttingDown) {
            // action != Intent.ACTION_MAIN when activity has been launched from a notification.
            stopService()
        }
        super.onDestroy()
    }

    private fun stopService() {
        (application as TexterApplication).stopService()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - clickTime < DOUBLE_CLICK_MILLIS) {
            shuttingDown = true
            super.onBackPressed()
        } else {
            Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT).show()
            clickTime = System.currentTimeMillis()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_help -> {
                showHelpDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun onLocationPermissionGranted() {
        initGps()
    }

    companion object {

        private val PREF_STARTUP_SHOWN = "pref_startup_shown"

        private val GOOGLE_REQUEST_CODE = 0

        /** Number of milliseconds that will be taken for a double click.  */
        private val DOUBLE_CLICK_MILLIS: Long = 5000
    }
}
