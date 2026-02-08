#include "SQLiteRevenueRepository.h"
#include "../infrastructure/Database.h"
#include <sqlite3.h>
#include <stdexcept>
#include <vector>

SQLiteRevenueRepository::SQLiteRevenueRepository(Database& db)
    : database(db) {}

int SQLiteRevenueRepository::save(const Revenue& revenue) {
    const char* sql = "INSERT INTO revenues (amount_cents, date) VALUES (?, ?);";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    sqlite3_bind_int64(stmt, 1, revenue.getAmount().getCents());
    sqlite3_bind_text(stmt, 2, revenue.getDate().toISO().c_str(), -1, SQLITE_TRANSIENT);

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return sqlite3_last_insert_rowid(database.get());
}

std::vector<Revenue> SQLiteRevenueRepository::findByMonth(int month, int year) {
    std::vector<Revenue> revenues;

    const char* sql = "SELECT id, amount_cents, date"
                    "FROM revenues"
                    "WHERE date >= ? "
                      "AND date < ?";

    std::string start = Date(1, month, year).toISO();
    std::string end = Date::firstDayOfNextMonth(month, year).toISO();

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_text(stmt, 1, start.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, end.c_str(), -1, SQLITE_TRANSIENT);

    while (sqlite3_step(stmt) == SQLITE_ROW) {
        int id = sqlite3_column_int(stmt, 0);
        long long amountCents = sqlite3_column_int64(stmt, 1);
        std::string dateISO = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));

        Revenue revenue(Money(amountCents), Date::fromISO(dateISO));
        revenue.setId(id);
        revenues.push_back(revenue);
    }
    sqlite3_finalize(stmt);
    return revenues;
}

Revenue SQLiteRevenueRepository::findById(int revenueId) {
    const char* sql = "SELECT id, amount_cents, date "
                    "FROM revenues "
                    "WHERE id = ?";
    
    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_int(stmt, 1, revenueId);

    if (sqlite3_step(stmt) != SQLITE_ROW) {
        sqlite3_finalize(stmt);
        throw std::runtime_error("Revenue not found");
    }

    int id = sqlite3_column_int(stmt, 0);
    long long amountCents = sqlite3_column_int64(stmt, 1);
    std::string dateISO = reinterpret_cast<const char*>(sqlite3_column_text(stmt, 2));

    Revenue revenue(Money(amountCents), Date::fromISO(dateISO));
    revenue.setId(id);
    
    sqlite3_finalize(stmt);
    return revenue;
}