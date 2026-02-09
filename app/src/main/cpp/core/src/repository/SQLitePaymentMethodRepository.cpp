#include "SQLitePaymentMethodRepository.h"
#include <stdexcept>

SQLitePaymentMethodRepository::SQLitePaymentMethodRepository(Database& db) : database(db) {}

PaymentType parseType(const std::string& type) {
    if (type == "IMMEDIATE") return PaymentType::IMMEDIATE;
    if (type == "CREDIT") return PaymentType::CREDIT;

    throw std::runtime_error("Invalid payment method type in database");
}


PaymentMethod SQLitePaymentMethodRepository::findById (int methodId) {
    const char* sql = "SELECT id, type, closing_day "
                    "FROM payment_methods "
                    "WHERE id = ?";

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);
    sqlite3_bind_int(stmt, 1, methodId);

    if (sqlite3_step(stmt) != SQLITE_ROW) {
        sqlite3_finalize(stmt);
        throw std::runtime_error("Payment Method not found");
    }

    int id = sqlite3_column_int(stmt, 0);
    const unsigned char* rawType = sqlite3_column_text(stmt, 1);
    std::string typeStr = rawType ? reinterpret_cast<const char*>(rawType) : "";
    int closingDay = sqlite3_column_int(stmt, 2);

    PaymentType type = parseType(typeStr);

    PaymentMethod paymentMethod(id, type, closingDay);

    sqlite3_finalize(stmt);
    return paymentMethod;
}