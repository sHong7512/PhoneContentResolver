package com.shong.phonecontentresolver

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

class PhoneResolver(val contentResolver: ContentResolver) {
    val TAG = this::class.java.simpleName + "_sHong"

    fun getGroup() {
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

    fun getGroupPhone(groupId: String) {
        val cProjection = arrayOf<String>(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
        )

        val groupCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            cProjection,
            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "= ?" + " AND "
                    + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'",
            arrayOf(groupId),
            null
        )
        if (groupCursor != null && groupCursor.moveToFirst()) {
            do {
                val nameCoumnIndex = groupCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val name = groupCursor.getString(nameCoumnIndex)
                val gi = groupCursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)
                val contactId = groupCursor.getLong(gi)
                val numberCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId,
                    null,
                    null
                )
                if (numberCursor!!.moveToFirst()) {
                    val numberColumnIndex = numberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
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

    fun getAllPhoneWithFavorite() {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.STARRED
        )
        val sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " ASC"
        val phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, sort)

        if (phones != null && phones.count > 0) {
            while (phones.moveToNext()) {
                val normalIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val normalizedNumber = phones.getString(normalIndex)
                val hasPhoneNumberIndex =
                    phones.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (phones.getString(hasPhoneNumberIndex).toInt() > 0) {

                    val idIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val id = phones.getInt(idIndex)

                    val nameIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val name = phones.getString(nameIndex)

//                    val phoneNumIndex = phones.getColumnIndex(Phone.NUMBER)
//                    val phoneNumber = phones.getString(phoneNumIndex)

                    val favIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    val fav = phones.getInt(favIndex)

                    val isFavorite = (fav == 1)

                    val uriIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val uri = phones.getString(uriIndex)
                    if (uri != null) {
                        Log.d(TAG, "$id $isFavorite $name $uri")
                    } else {
                        Log.d(TAG, "$id $isFavorite $name")
                    }

                    try {
                        val phone_type_index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                        val phone_type = phones.getInt(phone_type_index)
                        Log.d(TAG, "phone type : ${phone_type}")
                        when (phone_type) {
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> {
                                val phone_index =
                                    phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                Log.d(TAG, "mobile : ${phones.getString(phone_index)}")
                            }
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> {
                                val work_index =
                                    phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                Log.d(TAG, "work : ${phones.getString(work_index)}")
                            }
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> {
                                val home_index =
                                    phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
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