package br.com.expensetracker.bridge

object CoreBridge {

    init {
        System.loadLibrary("native-lib")
    }

    external fun initDatabase(path: String)
    external fun getMonthSummary(month: Int, year: Int): LongArray

    external fun addRevenueUseCase(amount: Long, day: Int, month: Int, year: Int)

}