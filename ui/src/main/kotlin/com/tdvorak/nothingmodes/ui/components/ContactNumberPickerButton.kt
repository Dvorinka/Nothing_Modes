package com.tdvorak.nothingmodes.ui.components

import android.Manifest
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton

/**
 * Button that picks a contact and returns the first phone number.
 * Gated on READ_CONTACTS, requested in context.
 */
@Composable
fun ContactNumberPickerButton(
    onNumber: (String) -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Pick contact",
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val hasNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (idIndex == -1 || hasNumberIndex == -1) return@use
                val id = cursor.getString(idIndex)
                if (cursor.getInt(hasNumberIndex) == 0) return@use
                context.contentResolver
                    .query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null,
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numberIndex != -1) {
                                phoneCursor
                                    .getString(numberIndex)
                                    ?.filter { it.isDigit() || it == '+' }
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let(onNumber)
                            }
                        }
                    }
            }
        }

    PermissionGate(
        permissions = listOf(Manifest.permission.READ_CONTACTS),
        rationale = "Choose a contact to pre-fill the phone number.",
        disclosure =
            "Nothing Modes reads your contacts only when you press 'Pick contact'. The selected phone number is used to pre-fill a trigger or action. " +
                "Contact data is processed on your device and is never uploaded, sold, or shared.",
        modifier = modifier,
    ) {
        NothingPillButton(
            text = text,
            onClick = { launcher.launch(null) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
