package com.example

import com.example.printer.EscPosBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCashDrawerCalculation() {
        // Rumus uang di laci: modal awal + penjualan tunai + kas masuk - kas keluar
        val modalAwal = 100000.0
        val penjualanTunai = 250000.0
        val kasMasuk = 50000.0
        val kasKeluar = 20000.0

        val perkiraanLaci = modalAwal + penjualanTunai + kasMasuk - kasKeluar
        assertEquals(380000.0, perkiraanLaci, 0.001)

        val uangAktual = 380000.0
        val selisih = uangAktual - perkiraanLaci
        assertEquals(0.0, selisih, 0.001)
    }

    @Test
    fun testPaymentChangeCalculation() {
        val totalBelanja = 78000.0
        val uangDiterima = 100000.0

        val kembalian = (uangDiterima - totalBelanja).coerceAtLeast(0.0)
        assertEquals(22000.0, kembalian, 0.001)
    }

    @Test
    fun testDiscountCalculation() {
        val subtotal = 150000.0
        val discount = 15000.0
        val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
        val taxRate = 11.0 // 11% PPN
        val tax = afterDiscount * (taxRate / 100.0)
        val grandTotal = afterDiscount + tax

        assertEquals(135000.0, afterDiscount, 0.001)
        assertEquals(14850.0, tax, 0.001)
        assertEquals(149850.0, grandTotal, 0.001)
    }

    @Test
    fun testEscPosReceiptFormatting() {
        val builder = EscPosBuilder("58mm")
        builder.init()
            .textLine("KASIR DUNIA")
            .doubleRow("Item 1", "Rp 15.000")
            .divider('-')
        val bytes = builder.build()
        assertTrue(bytes.isNotEmpty())
    }
}
