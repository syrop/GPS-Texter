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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.texter.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.webkit.WebView
import android.widget.Toast
import androidx.viewpager.widget.ViewPager

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import java.util.ArrayList
import java.util.Locale

import pl.org.seva.texter.R
import pl.org.seva.texter.ui.TitledPagerAdapter
import pl.org.seva.texter.history.HistoryFragment
import pl.org.seva.texter.main.extension.readString
import pl.org.seva.texter.movement.location
import pl.org.seva.texter.stats.StatsFragment
import pl.org.seva.texter.navigation.NavigationFragment
import pl.org.seva.texter.settings.SettingsActivity
import pl.org.seva.texter.sms.smsSender
import pl.org.seva.texter.ui.SlidingTabLayout

class MainActivity : AppCompatActivity() {

    /** Used when counting a double click.  */
    private var clickTime: Long = 0
    private var exitToast: Toast? = null
    /** Obtained from intent, may be null.  */
    private var action: String? = null
    private var shuttingDown: Boolean = false
    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        action = intent.action
        action?: finish()

        setContentView(R.layout.activity_main)

        val titles = arrayOf<CharSequence>(
                getString(R.string.stats_tab_name),
                getString(R.string.map_tab_name),
                getString(R.string.history_tab_name))

        setSupportActionBar(findViewById(R.id.toolbar))
        val fragments = ArrayList<Fragment>()
        val pager = findViewById<ViewPager>(R.id.pager)
        val tabs = findViewById<SlidingTabLayout>(R.id.tabs)
        fragments.add(StatsFragment.newInstance())
        fragments.add(NavigationFragment.newInstance())
        fragments.add(HistoryFragment.newInstance())

        val adapter = TitledPagerAdapter(supportFragmentManager, titles).setItems(fragments)

        pager.adapter = adapter

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

        smsSender.init(getString(R.string.speed_unit))

        val googlePlay = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (googlePlay != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, googlePlay, GOOGLE_REQUEST_CODE)?.show()
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
    @SuppressLint("CheckResult")
    private fun processPermissions(): Boolean {
        val permissionsToRequest = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions
                    .permissionGrantedListener()
                    .filter { it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
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
                Permissions.LOCATION_PERMISSION_REQUEST_ID)

        return false
    }

    private fun initGps() = location.initGpsOnLocationGranted(applicationContext)

    private fun showStartupDialog(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean(PREF_STARTUP_SHOWN, false)) {
            return false
        }
        dialog = Dialog(this)
        checkNotNull(dialog).setCancelable(false)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_startup)
        val web = dialog.findViewById<WebView>(R.id.web)
        val language = Locale.getDefault().language
        web.settings.defaultTextEncodingName = "utf-8"
        val content =
                assets.open(if (language == "pl") "startup_pl.html" else "startup_en.html")
                        .readString()
                        .replace(APP_VERSION_PLACEHOLDER, versionName)
        web.loadDataWithBaseURL("file:///android_asset/", content, "text/html", "UTF-8", null)

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
        checkNotNull(dialog).setCancelable(false)
        checkNotNull(dialog).setContentView(R.layout.dialog_help)
        val web = checkNotNull(dialog).findViewById<WebView>(R.id.web)
        web.settings.defaultTextEncodingName = "utf-8"
        val language = Locale.getDefault().language
        val content =
                assets.open(if (language == "pl") "help_pl.html" else "help_en.html")
                        .readString()
                        .replace(APP_VERSION_PLACEHOLDER, versionName)
        web.loadDataWithBaseURL(
                "file:///android_asset/",
                content,
                "text/html",
                "UTF-8",
                null)

        checkNotNull(dialog).findViewById<View>(R.id.ok).setOnClickListener {
            checkNotNull(dialog).dismiss()
        }
        checkNotNull(dialog).show()
    }

    private val versionName: String
        get() =  packageManager.getPackageInfo(packageName, 0).versionName

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissionsArray: Array<String>,
            grantResults: IntArray) =
            // If request is cancelled, the result arrays are empty.
            permissions.onRequestPermissionsResult(requestCode, permissionsArray, grantResults)

    public override fun onDestroy() {
        // Also called when the screen is rotated.
        dialog?.dismiss()
        if (action == Intent.ACTION_MAIN && shuttingDown) {
            // action != Intent.ACTION_MAIN when activity has been launched from a notification.
            stopService()
        }
        super.onDestroy()
    }

    private fun stopService() = (application as TexterApplication).stopService()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onBackPressed() = if (System.currentTimeMillis() - clickTime < DOUBLE_CLICK_MS) {
        shuttingDown = true
        exitToast?.cancel()
        super.onBackPressed()
    } else {
        exitToast?.cancel()
        exitToast = Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT)
        checkNotNull(exitToast).show()
        clickTime = System.currentTimeMillis()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_help -> {
                    showHelpDialog()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun onLocationPermissionGranted() = initGps()

    companion object {

        private const val APP_VERSION_PLACEHOLDER = "[app_version]"

        private const val PREF_STARTUP_SHOWN = "pref_startup_shown"

        private const val GOOGLE_REQUEST_CODE = 0

        /** Number of milliseconds that will be taken for a double click.  */
        private const val DOUBLE_CLICK_MS = 1000L
    }
}
