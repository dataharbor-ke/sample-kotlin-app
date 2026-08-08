package co.ke.dataharbor.samplekotlinapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.ke.dataharbor.fdis_sdk.FDIS
import co.ke.dataharbor.fdis_sdk.consent.ConsentDialog
import java.util.UUID

/**
 * Sample app demonstrating the FDIS SDK host-app flow.
 *
 * 1. Initialize FDIS to present consent and register the client.
 * 2. Request Android SMS permissions from the consent callback.
 * 3. Notify FDIS after both permissions are granted.
 * 4. Keep permission retry UI in the host app.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_SMS = 1001
        private const val TAG = "FDISKotlinSample"
        private const val HOST_PREFS_NAME = "fdis_sample_prefs"
        private const val KEY_SMS_PERMISSION_REQUESTED = "sms_permission_requested"
    }

    private lateinit var name: String
    private lateinit var email: String
    private lateinit var phone: String
    private lateinit var externalId: String
    private var awaitingPermissionSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        generateTestUser()

        findViewById<Button>(R.id.btn_default_dialog).setOnClickListener {
            startSdkWithDefaultDialog()
        }
        findViewById<Button>(R.id.btn_custom_dialog).setOnClickListener {
            startSdkWithCustomDialog()
        }
        findViewById<Button>(R.id.btn_sms_permission_banner).setOnClickListener {
            checkAndRequestSmsPermission()
        }
        findViewById<Button>(R.id.btn_clear_data).setOnClickListener {
            FDIS.clearData(applicationContext)
            updatePermissionBanner()
        }

        updatePermissionBanner()
    }

    /**
     * Uses temporary data for the sample. Supply the authenticated user's data
     * in a production integration.
     */
    private fun generateTestUser() {
        val id = UUID.randomUUID().toString().take(6)
        name = "User_$id"
        email = "user_$id@test.dev"
        phone = "07${(10000000..99999999).random()}"
        externalId = "EXT_$id"
    }

    override fun onResume() {
        super.onResume()
        if (awaitingPermissionSettings && hasSmsPermission()) {
            awaitingPermissionSettings = false
            notifySdkPermissionGranted()
        } else {
            updatePermissionBanner()
        }
    }

    private fun hasSmsPermission(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val receiveGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && receiveGranted
    }

    /**
     * The host app requests Android permissions only after FDIS has consent.
     */
    private fun checkAndRequestSmsPermission() {
        if (!FDIS.hasConsent(this)) return
        if (hasSmsPermission()) {
            notifySdkPermissionGranted()
            return
        }

        val permissionRequested = getSharedPreferences(HOST_PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_SMS_PERMISSION_REQUESTED, false)
        val canRequestAgain = listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        ).any { permission ->
            ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }

        if (permissionRequested && !canRequestAgain) {
            awaitingPermissionSettings = true
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        getSharedPreferences(HOST_PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SMS_PERMISSION_REQUESTED, true)
            .apply()
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
            REQ_SMS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode != REQ_SMS) return

        if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) {
            notifySdkPermissionGranted()
        } else {
            updatePermissionBanner()
        }
    }

    private fun onConsentGranted() {
        updatePermissionBanner()
        if (!hasSmsPermission()) {
            checkAndRequestSmsPermission()
        } else {
            notifySdkPermissionGranted()
        }
    }

    private fun notifySdkPermissionGranted() {
        FDIS.onSmsPermissionGranted(applicationContext) { success, error ->
            runOnUiThread {
                updatePermissionBanner()
                if (!success) {
                    Log.e(TAG, "FDIS could not resume after permission grant: $error")
                }
            }
        }
    }

    private fun updatePermissionBanner() {
        val show = FDIS.hasConsent(this) && !hasSmsPermission()
        findViewById<TextView>(R.id.tv_sms_permission_banner).visibility =
            if (show) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btn_sms_permission_banner).visibility =
            if (show) View.VISIBLE else View.GONE
    }

    private fun startSdkWithDefaultDialog() {
        FDIS.init(
            context = this,
            orgKey = BuildConfig.FDIS_ORG_KEY,
            name = name,
            email = email,
            phone = phone,
            externalId = externalId,
            syncEnabled = true,
            autoUpload = true,
            consentDialog = null,
            onConsentGranted = ::onConsentGranted
        ) { success, error ->
            if (!success) {
                Log.e(TAG, "FDIS SDK initialization failed: $error")
            }
        }
    }

    private fun startSdkWithCustomDialog() {
        val customDialog = ConsentDialog(
            context = this,
            backgroundColorHex = "#FFFFFF",
            titleColorHex = "#111827",
            messageColorHex = "#374151",
            buttonColorHex = "#1976D2",
            buttonCornerRadius = 24f,
            iconColorHex = "#1976D2",
            closeIconColorHex = "#6B7280",
            titleText = "Empower Your Insights",
            introText = "We'll securely analyze SMS messages to uncover financial patterns.",
            smsTitleText = "SMS Access",
            smsDescText = "We only scan messages needed to detect and process transactions.",
            privacyTitleText = "Privacy First",
            privacyDescText = "Your data is encrypted and used only for FDIS financial insights.",
            consentHtmlText = "I agree to the <b>FDIS Terms</b> and <b>Privacy Policy</b>.",
            buttonText = "Allow FDIS"
        )

        FDIS.init(
            context = this,
            orgKey = BuildConfig.FDIS_ORG_KEY,
            name = name,
            email = email,
            phone = phone,
            externalId = externalId,
            syncEnabled = true,
            autoUpload = true,
            consentDialog = customDialog,
            onConsentGranted = ::onConsentGranted
        ) { success, error ->
            if (!success) {
                Log.e(TAG, "FDIS SDK initialization failed: $error")
            }
        }
    }
}
