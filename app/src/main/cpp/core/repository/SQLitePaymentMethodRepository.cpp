#include "SQLitePaymentMethodRepository.h"
#include <stdexcept>
#include <android/log.h>

SQLitePaymentMethodRepository::SQLitePaymentMethodRepository(Database& db) : database(db) {}

PaymentType parseType(const std::string& type) {
    if (type == "IMMEDIATE") return PaymentType::IMMEDIATE;
    if (type == "CREDIT") return PaymentType::CREDIT;

    return PaymentType::IMMEDIATE;
}

void SQLitePaymentMethodRepository::save(const std::string& name, const std::string& type, int closingDay, int dueDay) {
    const char* sql = "INSERT INTO payment_methods (name, type, closing_day, due_day) VALUES (?, ?, ?, ?);";
    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database.get(), sql, -1, &stmt, nullptr);

    sqlite3_bind_text(stmt, 1, name.c_str(), -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, type.c_str(), -1, SQLITE_TRANSIENT);

    if (type == "CREDIT") {
        sqlite3_bind_int(stmt, 3, closingDay);
        sqlite3_bind_int(stmt, 4, dueDay);
    } else {
        sqlite3_bind_null(stmt, 3);
        sqlite3_bind_null(stmt, 4);
    }

    sqlite3_step(stmt);
    sqlite3_finalize(stmt);
}

PaymentMethod SQLitePaymentMethodRepository::findById (int methodId) {
    const char* sql = "SELECT id, name, type, closing_day, due_day "
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

    const unsigned char* rawName = sqlite3_column_text(stmt, 1);
    std::string nameStr = rawName ? reinterpret_cast<const char*>(rawName) : "Desconhecido";

    const unsigned char* rawType = sqlite3_column_text(stmt, 2);
    std::string typeStr = rawType ? reinterpret_cast<const char*>(rawType) : "IMMEDIATE";

    int closingDay = sqlite3_column_int(stmt, 3);
    int dueDay = sqlite3_column_int(stmt, 4);
    __android_log_print(ANDROID_LOG_ERROR, "CoreBridge", "Type from DB: %s", typeStr.c_str());

    PaymentType type = parseType(typeStr);

    PaymentMethod paymentMethod(id, nameStr, type, closingDay, dueDay);

    sqlite3_finalize(stmt);
    return paymentMethod;
}