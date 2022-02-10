package com.shong.phonecontentresolver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.shong.phonecontentresolver.databinding.ActivityMainBinding

// 1. 즐겨찾기 여부 포함 모든번호
// 2. 연락처 그룹 및 속해있는 번호
class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName + "_sHong"

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS,
//        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }
            if (granted) {
                Log.d(TAG, "granted")
            } else {
                Log.d(TAG, "not granted")
                if (Build.VERSION.SDK_INT >= 30) {
                    Snackbar.make(
                        binding.root,
                        "권한이 거부되어 있습니다.\n'확인'을 누르면 설정창으로 이동합니다.",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("확인") {
                            val settingIntent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(settingIntent)
                        }
                        .show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.getPhoneButton.setOnClickListener { checkPermission() }
    }

    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                Log.d(TAG, "주소록 권한 요청")
            }
            permReqLauncher.launch(PERMISSIONS)
        } else {
            Log.d(TAG, "already granted")
            getGroup()
            getAllPhoneWithFavorite()
        }
    }

    private fun getGroup() {
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.ACCOUNT_NAME,
            ContactsContract.Groups.ACCOUNT_TYPE,
            ContactsContract.Groups.DELETED,
            ContactsContract.Groups.GROUP_VISIBLE
        )
        val cursor: Cursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            projection,
            null,
            null,
            null
        ) ?: return
        while (cursor.moveToNext()) {
            Log.d(TAG, "===== Group Id : ${cursor.getString(0)} =====")
            Log.d(TAG, "Group Title : " + cursor.getString(1))
            Log.d(TAG, "Group Account Name : " + cursor.getString(2))
            Log.d(TAG, "Group Account Type : " + cursor.getString(3))
            Log.d(TAG, "Group Deleted : " + cursor.getString(4))
            Log.d(TAG, "Group Visible : " + cursor.getString(5))
            Log.d(TAG, "========================")

            getGroupPhone(cursor.getString(0))
        }
        cursor.close()
    }

    private fun getGroupPhone(groupId: String) {
        val cProjection = arrayOf<String>(
            ContactsContract.Contacts.DISPLAY_NAME,
            CommonDataKinds.GroupMembership.CONTACT_ID
        )

        val groupCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            cProjection,
            CommonDataKinds.GroupMembership.GROUP_ROW_ID + "= ?" + " AND "
                    + CommonDataKinds.GroupMembership.MIMETYPE + "='"
                    + CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'",
            arrayOf(groupId),
            null
        )
        if (groupCursor != null && groupCursor.moveToFirst()) {
            do {
                val nameCoumnIndex = groupCursor.getColumnIndex(Phone.DISPLAY_NAME)
                val name = groupCursor.getString(nameCoumnIndex)
                val gi = groupCursor.getColumnIndex(CommonDataKinds.GroupMembership.CONTACT_ID)
                val contactId = groupCursor.getLong(gi)
                val numberCursor = contentResolver.query(
                    Phone.CONTENT_URI,
                    arrayOf(Phone.NUMBER),
                    Phone.CONTACT_ID + "=" + contactId,
                    null,
                    null
                )
                if (numberCursor!!.moveToFirst()) {
                    val numberColumnIndex = numberCursor.getColumnIndex(Phone.NUMBER)
                    do {
                        val phoneNumber = numberCursor.getString(numberColumnIndex)
                        Log.d(TAG, "*** Phone $name:$phoneNumber ***")
                    } while (numberCursor.moveToNext())
                }
                numberCursor.close()
            } while (groupCursor.moveToNext())
        }

        groupCursor?.close()
    }

    private fun getAllPhoneWithFavorite() {
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.NUMBER,
            Phone.NORMALIZED_NUMBER,
            Phone.TYPE,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED
        )
        val sort = Phone.DISPLAY_NAME_PRIMARY + " ASC"
        val phones = contentResolver.query(Phone.CONTENT_URI, projection, null, null, sort)

        if (phones != null && phones.count > 0) {
            while (phones.moveToNext()) {
                val normalIndex = phones.getColumnIndex(Phone.DISPLAY_NAME)
                val normalizedNumber = phones.getString(normalIndex)
                val hasPhoneNumberIndex = phones.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (phones.getString(hasPhoneNumberIndex).toInt() > 0) {

                    val idIndex = phones.getColumnIndex(Phone.CONTACT_ID)
                    val id = phones.getInt(idIndex)

                    val nameIndex = phones.getColumnIndex(Phone.DISPLAY_NAME)
                    val name = phones.getString(nameIndex)

//                    val phoneNumIndex = phones.getColumnIndex(Phone.NUMBER)
//                    val phoneNumber = phones.getString(phoneNumIndex)

                    val favIndex = phones.getColumnIndex(Phone.STARRED)
                    val fav = phones.getInt(favIndex)

                    val isFavorite = (fav == 1)

                    val uriIndex = phones.getColumnIndex(Phone.PHOTO_URI)
                    val uri = phones.getString(uriIndex)
                    if (uri != null) {
                        Log.d(TAG, "$id $isFavorite $name $uri")
                    } else {
                        Log.d(TAG, "$id $isFavorite $name")
                    }

                    try {
                        val phone_type_index = phones.getColumnIndex(Phone.TYPE)
                        val phone_type = phones.getInt(phone_type_index)
                        Log.d(TAG, "phone type : ${phone_type}")
                        when (phone_type) {
                            Phone.TYPE_MOBILE -> {
                                val phone_index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                Log.d(TAG, "mobile : ${phones.getString(phone_index)}")
                            }
                            Phone.TYPE_WORK -> {
                                val work_index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                Log.d(TAG, "work : ${phones.getString(work_index)}")
                            }
                            Phone.TYPE_HOME -> {
                                val home_index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                Log.d(TAG, "home : ${phones.getString(home_index)}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "error : ${e.localizedMessage}")
                    }

                }
            }
            phones.close()
        }
    }

}