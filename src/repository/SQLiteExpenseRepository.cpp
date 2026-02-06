#include "SQLiteExpenseRepository.h"
#include "Database.h"
#include <sqlite3.h>
#include <vector>

SQLiteExpenseRepository::SQLiteExpenseRepository(Database& db)
    : database(db) {}

int SQLiteExpenseRepository::save(int revenueId, const Expense& expense) {
    const char* sql = "INSERT INTO expenses (revenue_id, amount_cents, date, category_id, payment_method_id) VALUES (?, ?, ?, ?, ?);";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    sqlite3_bind_int(stmt, 1, revenueId);
    sqlite3_bind_int64(stmt, 2, expense.getAmount().getCents());
    sqlite3_bind_text(stmt, 3, expense.getDate().toISO().c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 4, expense.getCategoryId());
    sqlite3_bind_int(stmt, 5, expense.getPaymentMethodId());

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return sqlite3_last_insert_rowid(database.get());
}

std::vector<Expense> SQLiteExpenseRepository::findByRevenue(int revenueId) {
    std::vector<Expense> expenses;

    const char* sql = "SELECT amount_cents, date, category_id, payment_method_id "
                    "FROM expenses "
                    "WHERE revenue_id = ?;";
                
    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_int(stmt, 1, revenueId);

    while(sqlite3_step(stmt) == SQLITE_ROW) {
        long long amountCents = sqlite3_column_int64(stmt, 0);
        std::string dateISO = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 1));
        int categoryId = sqlite3_column_int(stmt, 2);
        int paymentMethodId = sqlite3_column_int(stmt, 3);
        
        Expense expense (Money(amountCents), Date::fromISO(dateISO), categoryId, paymentMethodId);

        expenses.push_back(expense);
    }
    sqlite3_finalize(stmt);
    return expenses;
}