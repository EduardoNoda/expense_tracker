#include "SQLiteExpenseRepository.h"
#include "../infrastructure/Database.h"
#include <sqlite3.h>
#include <vector>

SQLiteExpenseRepository::SQLiteExpenseRepository(Database& db)
    : database(db) {}

int SQLiteExpenseRepository::save(int revenueId, const Expense& expense) {
    const char* sql = "INSERT INTO expenses (revenue_id, amount_cents, date, impact_date,category_id, payment_method_id) VALUES (?, ?, ?, ?, ?, ?);";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    if (revenueId <= 0) {
        sqlite3_bind_null(stmt, 1);
    } else {
        sqlite3_bind_int(stmt, 1, revenueId);
    }
    sqlite3_bind_int64(stmt, 2, expense.getAmount().getCents());
    sqlite3_bind_text(stmt, 3, expense.getDate().toISO().c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 4, expense.getImpactDate().toISO().c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_int(stmt, 5, expense.getCategoryId());
    sqlite3_bind_int(stmt, 6, expense.getPaymentMethodId());

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return sqlite3_last_insert_rowid(database.get());
}

std::vector<Expense> SQLiteExpenseRepository::findByRevenue(int revenueId) {
    std::vector<Expense> expenses;

    const char* sql = "SELECT id, revenue_id, amount_cents, date, impact_date, category_id, payment_method_id "
                        "FROM expenses "
                        "WHERE revenue_id = ?;";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_int(stmt, 1, revenueId);

    while(sqlite3_step(stmt) == SQLITE_ROW) {
        int id = sqlite3_column_int(stmt, 0);
        long long amountCents = sqlite3_column_int64(stmt, 1);
        std::string dateISO = reinterpret_cast<const char*>(sqlite3_column_text(stmt,2));
        std::string impactDate = reinterpret_cast<const char*>(sqlite3_column_text(stmt,3));
        int categoryId = sqlite3_column_int(stmt,4);
        int paymentMethodId = sqlite3_column_int(stmt,5);

        Expense expense (id, revenueId, Money(amountCents), Date::fromISO(dateISO), Date::fromISO(impactDate),categoryId, paymentMethodId);

        expenses.push_back(expense);
    }
    sqlite3_finalize(stmt);
    return expenses;
}

std::vector<Expense> SQLiteExpenseRepository::findByImpactMonth(int month, int year) {
    std::vector<Expense> expenses;

    const char* sql = "SELECT id, revenue_id, amount_cents, date, impact_date, category_id, payment_method_id "
                      "FROM expenses "
                      "WHERE impact_date >= ? "
                      "AND impact_date < ?;";

    std::string start = Date(1, month, year).toISO();
    std::string end = Date::firstDayOfNextMonth(month, year).toISO();

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_text(stmt, 1, start.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, end.c_str(), -1, SQLITE_TRANSIENT);

    while(sqlite3_step(stmt) == SQLITE_ROW) {
        int id = sqlite3_column_int(stmt, 0);
        int revenueId = 0;
        if (sqlite3_column_type(stmt, 1) != SQLITE_NULL) {
            revenueId = sqlite3_column_int(stmt, 1);
        }
        long long amountCents = sqlite3_column_int64(stmt, 2);
        std::string dateISO = reinterpret_cast<const char*>(sqlite3_column_text(stmt,3));
        std::string impactDate = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 4));
        int categoryId = sqlite3_column_int(stmt, 5);
        int paymentMethodId = sqlite3_column_int(stmt, 6);

        Expense expense (id, revenueId, Money(amountCents), Date::fromISO(dateISO), Date::fromISO(impactDate),categoryId, paymentMethodId);

        expenses.push_back(expense);
    }
    sqlite3_finalize(stmt);
    return expenses;
}
void SQLiteExpenseRepository::deleteById(int expenseId) {
    const char* sql = "DELETE FROM expenses WHERE id = ?;";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    sqlite3_bind_int(stmt, 1, expenseId);

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);
}
void SQLiteExpenseRepository::payCreditCardBill(int month, int year, int targetRevenueId) {
    // Atualiza os gastos órfãos do mês para pertencerem à Receita escolhida!
    const char* sql = "UPDATE expenses SET revenue_id = ? WHERE revenue_id IS NULL AND impact_date >= ? AND impact_date < ?;";

    std::string start = Date(1, month, year).toISO();
    std::string end = Date::firstDayOfNextMonth(month, year).toISO();

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    sqlite3_bind_int(stmt, 1, targetRevenueId);
    sqlite3_bind_text(stmt, 2, start.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 3, end.c_str(), -1, SQLITE_TRANSIENT);

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);
}
void SQLiteExpenseRepository::beginTransaction() {
    database.beginTransaction();
}

void SQLiteExpenseRepository::commitTransaction() {
    database.commitTransaction();
}