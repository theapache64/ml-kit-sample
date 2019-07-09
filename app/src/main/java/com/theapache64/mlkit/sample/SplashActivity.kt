package com.theapache64.mlkit.sample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.fitpolo.support.MokoSupport
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import com.theapache64.mlkit.sample.databinding.ActivitySplashBinding
import com.theapache64.twinkill.logger.info

class SplashActivity : AppCompatActivity() {


    companion object {
        const val RQ_ENABLE_BT = 423
    }

    private var isPermissionReCheckNeeded: Boolean = false
    private var binding: ActivitySplashBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding =
            DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash)

        info("Splash invoked")

        confirmPermission()
    }

    private fun confirmPermission() {

        if (!MokoSupport.getInstance().isBluetoothOpen) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RQ_ENABLE_BT)
            return
        }



        MokoSupport.getInstance().disConnectBle()

        confirmPermission(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) {
            goToBleConnect()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isPermissionReCheckNeeded) {
            isPermissionReCheckNeeded = false
            confirmPermission()
        }
    }

    private fun goToBleConnect() {
        Handler().postDelayed({
            startActivity(Intent(this, BleConnectActivity::class.java))
            finish()
        }, 2000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RQ_ENABLE_BT) {
            confirmPermission()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Checks for given permission and executes onPermissionGranted when all requester
     * permissions are allowed.
     */
    @Suppress("SameParameterValue")
    private fun confirmPermission(vararg permissions: String, onPermissionGranted: () -> Unit) {

        // Deny listener
        val deniedPermissionListener =
            SnackbarOnAnyDeniedMultiplePermissionsListener.Builder.with(
                binding!!.root,
                R.string.enable_permissions
            )
                .withDuration(Snackbar.LENGTH_INDEFINITE)
                .withOpenSettingsButton(R.string.action_settings)
                .withCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        isPermissionReCheckNeeded = true
                    }
                })
                .build()

        // Default listener
        val defaultPermissionsListener = object : BaseMultiplePermissionsListener() {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    onPermissionGranted()
                }
            }
        }


        // Combining both
        val permissionListener = CompositeMultiplePermissionsListener(
            deniedPermissionListener,
            defaultPermissionsListener
        )

        Dexter.withActivity(this)
            .withPermissions(permissions.toList())
            .withListener(permissionListener)
            .check()
    }

}
