package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.phone.PhoneNumberFormatter
import java.util.Locale

/**
 * Country/dial-code picker backed by libphonenumber.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePicker(
    selected: PhoneNumberFormatter.CountryCode,
    onSelected: (PhoneNumberFormatter.CountryCode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val countries = remember { PhoneNumberFormatter.supportedCountryCodes() }

    TextButton(
        onClick = { open = true },
        modifier = modifier,
    ) {
        Text(text = "${selected.flag} ${selected.dialCode}")
    }

    if (open) {
        CountryCodeSheet(
            countries = countries,
            selected = selected,
            onSelected = {
                onSelected(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodeSheet(
    countries: List<PhoneNumberFormatter.CountryCode>,
    selected: PhoneNumberFormatter.CountryCode,
    onSelected: (PhoneNumberFormatter.CountryCode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(countries, query) {
            if (query.isBlank()) {
                countries
            } else {
                val q = query.lowercase(Locale.getDefault())
                countries.filter {
                    it.name.lowercase().contains(q) ||
                        it.dialCode.contains(q) ||
                        it.regionCode.lowercase().contains(q)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Choose country",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(360.dp),
            ) {
                items(
                    items = filtered,
                    key = { it.regionCode },
                ) { country ->
                    CountryItem(
                        country = country,
                        selected = country.regionCode == selected.regionCode,
                        onClick = { onSelected(country) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: PhoneNumberFormatter.CountryCode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(text = country.flag)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = country.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = country.dialCode,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
