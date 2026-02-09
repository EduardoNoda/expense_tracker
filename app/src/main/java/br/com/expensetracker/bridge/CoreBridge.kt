package br.com.expensetracker.bridge

object CoreBridge {

    init {
        System.loadLibrary("native_lib")
    }

    external fun getMonthSummary(month: Int, year: Int): LongArray

}