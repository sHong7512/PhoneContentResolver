package com.shong.phonecontentresolver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.shong.phonecontentresolver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName + "_sHong"

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value == true }
            if (granted) {
                Log.d(TAG, "granted")
            } else {
                Log.d(TAG, "not granted")
                if (Build.VERSION.SDK_INT >= 30) {
                    Snackbar.make(
                        binding.root,
                        "권한이 거부되어 있습니다.\n'확인'을 누르면 설정창으로 이동합니다.",
                        Snackbar.LENGTH_LONG
                    ).setAction("확인") {
                        val settingIntent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", packageName, null)
                        }
                        startActivity(settingIntent)
                    }.show()
                }
            }
        }

    private val phoneResolver by lazy { PhoneResolver(contentResolver) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // true : Call History   false : Contact Phone Number
        binding.getPhoneButton.setOnClickListener { checkPermission(false) }
        binding.getCallHistoryButton.setOnClickListener { checkPermission(true) }
    }

    private fun checkPermission(isHP: Boolean) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                Log.d(TAG, "주소록 권한 요청")
            }
            permReqLauncher.launch(PERMISSIONS)
        } else {
            Log.d(TAG, "already granted")
            if(isHP){
                binding.callHistoryTextView.text = phoneResolver.getCallHistory()
            }else{
//                phoneResolver.getGroup()
                phoneResolver.getAllPhoneWithFavorite()
            }

        }
    }

}