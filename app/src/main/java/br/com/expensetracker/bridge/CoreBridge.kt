package br.com.expensetracker.bridge

object CoreBridge {

    init {
        System.loadLibrary("native-lib")
    }
    external fun initDatabase(path: String)
    external fun getMonthSummary(month: Int, year: Int): LongArray
    external fun addRevenueUseCase(name: String, amount: Long, day: Int, month: Int, year: Int)
    external fun getRevenuesForMonth(month: Int, year: Int): String
    external fun addExpenseToRevenue(
        revenueId: Int,
        amount: Long,
        day: Int,
        month: Int,
        year: Int,
        categoryId: Int,
        paymentMethodId: Int,
        installments: Int
    ): Int
    external fun deleteExpenseById(expenseId: Int)
    external fun deleteRevenueById(revenueId: Int)
    @JvmStatic
    external fun payCreditCardBill(month: Int, year: Int, revenueId: Int)
    @JvmStatic
    external fun checkDueInvoices(todayDay: Int, todayMonth: Int, todayYear: Int): String
    external fun getAllCategories(): String
    external fun getPaymentMethods(): String
    external fun addPaymentMethod(name: String, closingDay: Int, dueDay: Int)
}