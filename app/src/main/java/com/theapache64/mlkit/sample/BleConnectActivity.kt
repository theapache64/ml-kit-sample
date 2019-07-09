package com.theapache64.mlkit.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fitpolo.support.MokoSupport
import com.fitpolo.support.callback.MokoConnStateCallback

class BleConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_connect)

        MokoSupport.getInstance().connDevice(
            this,
            "CE:24:FC:49:E7:12",
            object : MokoConnStateCallback {
                override fun onConnectSuccess() {
                    runOnUiThread {
                        Toast.makeText(this@BleConnectActivity, "Connected", Toast.LENGTH_SHORT)
                            .show()

                        startActivity(Intent(this@BleConnectActivity, MainActivity::class.java))
                        finish()
                    }
                }

                override fun onDisConnected() {
                    runOnUiThread {
                        Toast.makeText(this@BleConnectActivity, "Disconnected", Toast.LENGTH_SHORT)
                            .show();
                        finish()
                    }
                }

                override fun onConnTimeout(reConnCount: Int) {
                    runOnUiThread {
                        Toast.makeText(
                            this@BleConnectActivity,
                            "Connection timedout",
                            Toast.LENGTH_SHORT
                        )
                            .show();
                        finish()
                    }
                }

                override fun onFindPhone() {
                }

            }
        )
    }
}
