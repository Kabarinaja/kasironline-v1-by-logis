package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kasirdunia_settings")

data class BusinessSettings(
    val ownerName: String = "Logis Denis Prabowo",
    val businessName: String = "KASIR DUNIA",
    val businessType: String = "Kuliner & Retail",
    val address: String = "Jl. Sudirman No. 45",
    val cityProvince: String = "Bandar Lampung, Lampung",
    val phone: String = "0823-7947-4173",
    val email: String = "kasirdunia.com@gmail.com",
    val currency: String = "Rp",
    val timezone: String = "WIB (GMT+7)",
    val language: String = "id", // id or en
    val receiptFooter: String = "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.",
    val paperSize: String = "58mm", // 58mm, 80mm, 102mm
    val isTaxEnabled: Boolean = false,
    val taxPercentage: Double = 11.0,
    val showHeaderInfo: Boolean = true,
    val showCashierInfo: Boolean = true,
    val showPromoQr: Boolean = false,
    val promoQrText: String = "https://kasirdunia.com",
    val qrisPayload: String = "00020101021126580011ID.CO.QRIS.WWW01189360091100223344550215ID10200300400500303UMI51440014ID.GO.BANKINDONESIA.QRIS01189360091100223344550215ID10200300400505204549953033605802ID5911KASIR DUNIA6014BANDAR LAMPUNG61053514262070703A016304E8A2",
    val preferredPrinterAddress: String = "",
    val preferredPrinterName: String = ""
)

class UserPreferences(private val context: Context) {

    private object PreferencesKeys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val IS_PIN_SET = booleanPreferencesKey("is_pin_set")
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

        // Business
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val BUSINESS_TYPE = stringPreferencesKey("business_type")
        val ADDRESS = stringPreferencesKey("address")
        val CITY_PROVINCE = stringPreferencesKey("city_province")
        val PHONE = stringPreferencesKey("phone")
        val EMAIL = stringPreferencesKey("email")
        val CURRENCY = stringPreferencesKey("currency")
        val TIMEZONE = stringPreferencesKey("timezone")
        val LANGUAGE = stringPreferencesKey("language")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val PAPER_SIZE = stringPreferencesKey("paper_size")
        val IS_TAX_ENABLED = booleanPreferencesKey("is_tax_enabled")
        val TAX_PERCENTAGE = doublePreferencesKey("tax_percentage")
        val SHOW_HEADER_INFO = booleanPreferencesKey("show_header_info")
        val SHOW_CASHIER_INFO = booleanPreferencesKey("show_cashier_info")
        val SHOW_PROMO_QR = booleanPreferencesKey("show_promo_qr")
        val PROMO_QR_TEXT = stringPreferencesKey("promo_qr_text")
        val QRIS_PAYLOAD = stringPreferencesKey("qris_payload")
        val PREFERRED_PRINTER_ADDRESS = stringPreferencesKey("preferred_printer_address")
        val PREFERRED_PRINTER_NAME = stringPreferencesKey("preferred_printer_name")
    }

    val isPinSetFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_PIN_SET] ?: false
    }

    val isOnboardedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.IS_ONBOARDED] ?: false
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LANGUAGE] ?: "id"
    }

    val businessSettingsFlow: Flow<BusinessSettings> = context.dataStore.data.map { prefs ->
        BusinessSettings(
            ownerName = prefs[PreferencesKeys.OWNER_NAME] ?: "Logis Denis Prabowo",
            businessName = prefs[PreferencesKeys.BUSINESS_NAME] ?: "KASIR DUNIA",
            businessType = prefs[PreferencesKeys.BUSINESS_TYPE] ?: "Kuliner & Retail",
            address = prefs[PreferencesKeys.ADDRESS] ?: "Jl. Sudirman No. 45",
            cityProvince = prefs[PreferencesKeys.CITY_PROVINCE] ?: "Bandar Lampung, Lampung",
            phone = prefs[PreferencesKeys.PHONE] ?: "0823-7947-4173",
            email = prefs[PreferencesKeys.EMAIL] ?: "kasirdunia.com@gmail.com",
            currency = prefs[PreferencesKeys.CURRENCY] ?: "Rp",
            timezone = prefs[PreferencesKeys.TIMEZONE] ?: "WIB (GMT+7)",
            language = prefs[PreferencesKeys.LANGUAGE] ?: "id",
            receiptFooter = prefs[PreferencesKeys.RECEIPT_FOOTER] ?: "Terima Kasih Atas Kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.",
            paperSize = prefs[PreferencesKeys.PAPER_SIZE] ?: "58mm",
            isTaxEnabled = prefs[PreferencesKeys.IS_TAX_ENABLED] ?: false,
            taxPercentage = prefs[PreferencesKeys.TAX_PERCENTAGE] ?: 11.0,
            showHeaderInfo = prefs[PreferencesKeys.SHOW_HEADER_INFO] ?: true,
            showCashierInfo = prefs[PreferencesKeys.SHOW_CASHIER_INFO] ?: true,
            showPromoQr = prefs[PreferencesKeys.SHOW_PROMO_QR] ?: false,
            promoQrText = prefs[PreferencesKeys.PROMO_QR_TEXT] ?: "https://kasirdunia.com",
            qrisPayload = prefs[PreferencesKeys.QRIS_PAYLOAD] ?: BusinessSettings().qrisPayload,
            preferredPrinterAddress = prefs[PreferencesKeys.PREFERRED_PRINTER_ADDRESS] ?: "",
            preferredPrinterName = prefs[PreferencesKeys.PREFERRED_PRINTER_NAME] ?: ""
        )
    }

    suspend fun savePin(pin: String) {
        val salt = UUID.randomUUID().toString().take(8)
        val hash = hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_HASH] = hash
            prefs[PreferencesKeys.PIN_SALT] = salt
            prefs[PreferencesKeys.IS_PIN_SET] = true
            prefs[PreferencesKeys.FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(inputPin: String): Boolean {
        var isValid = false
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val lockout = prefs[PreferencesKeys.LOCKOUT_UNTIL] ?: 0L
            if (now < lockout) {
                isValid = false
                return@edit
            }

            val savedHash = prefs[PreferencesKeys.PIN_HASH] ?: ""
            val salt = prefs[PreferencesKeys.PIN_SALT] ?: ""
            if (savedHash.isEmpty()) {
                isValid = true
                return@edit
            }

            val testHash = hashPin(inputPin, salt)
            if (testHash == savedHash) {
                isValid = true
                prefs[PreferencesKeys.FAILED_ATTEMPTS] = 0
                prefs[PreferencesKeys.LOCKOUT_UNTIL] = 0L
            } else {
                isValid = false
                val fails = (prefs[PreferencesKeys.FAILED_ATTEMPTS] ?: 0) + 1
                prefs[PreferencesKeys.FAILED_ATTEMPTS] = fails
                if (fails >= 5) {
                    prefs[PreferencesKeys.LOCKOUT_UNTIL] = now + (30 * 1000L) // 30 sec cooldown
                }
            }
        }
        return isValid
    }

    suspend fun getLockoutSeconds(): Int {
        var secondsRemaining = 0
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val lockout = prefs[PreferencesKeys.LOCKOUT_UNTIL] ?: 0L
            if (lockout > now) {
                secondsRemaining = ((lockout - now) / 1000).toInt()
            }
        }
        return secondsRemaining
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_ONBOARDED] = onboarded
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LANGUAGE] = lang
        }
    }

    suspend fun updateBusinessSettings(settings: BusinessSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.OWNER_NAME] = settings.ownerName
            prefs[PreferencesKeys.BUSINESS_NAME] = settings.businessName
            prefs[PreferencesKeys.BUSINESS_TYPE] = settings.businessType
            prefs[PreferencesKeys.ADDRESS] = settings.address
            prefs[PreferencesKeys.CITY_PROVINCE] = settings.cityProvince
            prefs[PreferencesKeys.PHONE] = settings.phone
            prefs[PreferencesKeys.EMAIL] = settings.email
            prefs[PreferencesKeys.CURRENCY] = settings.currency
            prefs[PreferencesKeys.TIMEZONE] = settings.timezone
            prefs[PreferencesKeys.LANGUAGE] = settings.language
            prefs[PreferencesKeys.RECEIPT_FOOTER] = settings.receiptFooter
            prefs[PreferencesKeys.PAPER_SIZE] = settings.paperSize
            prefs[PreferencesKeys.IS_TAX_ENABLED] = settings.isTaxEnabled
            prefs[PreferencesKeys.TAX_PERCENTAGE] = settings.taxPercentage
            prefs[PreferencesKeys.SHOW_HEADER_INFO] = settings.showHeaderInfo
            prefs[PreferencesKeys.SHOW_CASHIER_INFO] = settings.showCashierInfo
            prefs[PreferencesKeys.SHOW_PROMO_QR] = settings.showPromoQr
            prefs[PreferencesKeys.PROMO_QR_TEXT] = settings.promoQrText
            prefs[PreferencesKeys.QRIS_PAYLOAD] = settings.qrisPayload
            prefs[PreferencesKeys.PREFERRED_PRINTER_ADDRESS] = settings.preferredPrinterAddress
            prefs[PreferencesKeys.PREFERRED_PRINTER_NAME] = settings.preferredPrinterName
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = "$pin::$salt"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
