package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.tdvorak.nothingmodes.engine.phone.PhoneNumberFormatter
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import java.util.Locale

/**
 * E.164 phone number field with a country-code picker.
 *
 * The consumer sees [value] as the full E.164 string (e.g. `+<countryCode><national>`);
 * the field splits that into a country and a national number for editing.
 */
@Composable
fun PhoneNumberField(
    value: String?,
    onValueChange: (String?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val allCountries = remember { PhoneNumberFormatter.supportedCountryCodes() }
    val defaultRegion = remember { Locale.getDefault().country.ifBlank { "US" } }
    val defaultCountry =
        remember(allCountries, defaultRegion) {
            allCountries.firstOrNull { it.regionCode == defaultRegion }
                ?: allCountries.firstOrNull { it.regionCode == "US" }
                ?: allCountries.first()
        }

    var nationalText by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(defaultCountry) }

    LaunchedEffect(value) {
        val (country, national) = parseForUi(value, allCountries, defaultCountry)
        selectedCountry = country
        nationalText = national
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        CountryCodePicker(
            selected = selectedCountry,
            onSelected = { newCountry ->
                selectedCountry = newCountry
                val newValue =
                    when {
                        newCountry.regionCode == MANUAL_REGION -> value
                        value.isNullOrBlank() ->
                            PhoneNumberFormatter.formatToE164(nationalText, newCountry.regionCode)

                        else -> {
                            val parsed = PhoneNumberFormatter.parseE164(value)
                            if (parsed != null) {
                                PhoneNumberFormatter.formatToE164(parsed.first, newCountry.regionCode)
                            } else {
                                PhoneNumberFormatter.formatToE164(nationalText, newCountry.regionCode)
                            }
                        }
                    }
                onValueChange(newValue)
            },
        )
        Spacer(modifier = Modifier.width(NothingSpacing.sm))
        NothingInput(
            value = nationalText,
            onValueChange = { text ->
                nationalText = text
                onValueChange(
                    when {
                        text.isBlank() -> null
                        selectedCountry.regionCode == MANUAL_REGION -> text
                        else -> PhoneNumberFormatter.formatToE164(text, selectedCountry.regionCode)
                    },
                )
            },
            label = label,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
    }
}

private const val MANUAL_REGION = "ZZ"

private fun parseForUi(
    value: String?,
    allCountries: List<PhoneNumberFormatter.CountryCode>,
    defaultCountry: PhoneNumberFormatter.CountryCode,
): Pair<PhoneNumberFormatter.CountryCode, String> {
    if (value.isNullOrBlank()) return defaultCountry to ""
    val parsed = PhoneNumberFormatter.parseE164(value)
    if (parsed != null) {
        val (national, region) = parsed
        val country = allCountries.firstOrNull { it.regionCode == region } ?: defaultCountry
        return country to national
    }
    return defaultCountry to value
}
