#include <jni.h>
#include "../core/infrastructure/Database.h"
#include "../core/repository/SQLiteExpenseRepository.h"
#include "../core/repository/SQLiteRevenueRepository.h"
#include "../core/repository/SQLitePaymentMethodRepository.h"
#include "../core/application/GetMonthlySummaryUseCase.h"
#include "../core/application/AddRevenueUseCase.h"
#include "../core/application/AddExpenseUseCase.h"
#include <android/log.h>
#include <android/log.h>
#include <sstream>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EXPENSE_CORE", __VA_ARGS__)


static Database* database = nullptr;
static SQLiteExpenseRepository* expenseRepository = nullptr;
static SQLiteRevenueRepository* revenueRepository = nullptr;
static SQLitePaymentMethodRepository* paymentMethodRepository = nullptr;
static GetMonthlySummaryUseCase* summaryUseCase = nullptr;
static AddRevenueUseCase* addRevenue = nullptr;
static AddExpenseUseCase* addExpenseUseCase = nullptr;

extern "C"
JNIEXPORT jlongArray JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getMonthSummary(JNIEnv *env, jobject thiz, jint month, jint year) {
    if(summaryUseCase == nullptr)
        return env->NewLongArray(0);

    Month result = summaryUseCase->execute(month, year);

    jlongArray array = env->NewLongArray(3);
    jlong temp[3];

    temp[0] = result.getTotalRevenue().getCents();
    temp[1] = result.getTotalExpenses().getCents();
    temp[2] = result.getBalance().getCents();

    env->SetLongArrayRegion(array, 0, 3, temp);
    LOGI("Fetching month summary");
    LOGI("Revenue cents: %lld", result.getTotalRevenue().getCents());
    return array;
}
static void ensureSchema(Database& db) {
    db.exec("PRAGMA foreign_keys = ON;");

    db.exec(R"(
    DROP TABLE expenses;
    DROP TABLE payment_methods;
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE
    );
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS payment_methods (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL CHECK (type IN ('IMMEDIATE','CREDIT')),
        closing_day INTEGER CHECK (closing_day IS NULL OR closing_day BETWEEN 1 AND 31)
        due_day INTEGER CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31)
    );
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS revenues (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
        date TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS expenses (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        revenue_id INTEGER NOT NULL,
        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
        date TEXT NOT NULL,
        impact_date TEXT NOT NULL,
        category_id INTEGER NOT NULL,
        payment_method_id INTEGER NOT NULL,
        created_at TEXT NOT NULL DEFAULT (datetime('now')),

        FOREIGN KEY (revenue_id) REFERENCES revenues(id) ON DELETE CASCADE,
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE RESTRICT
    );
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (1, 'Geral');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO payment_methods(name, type) VALUES ('Dinheiro','IMMEDIATE')
    )");

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(db.get(), "SELECT COUNT(*) FROM payment_methods;", -1, &stmt, nullptr);
    sqlite3_step(stmt);
    int count = sqlite3_column_int(stmt, 0);
    sqlite3_finalize(stmt);

    LOGI("Payment methods count after ensureSchema: %d", count);

    sqlite3_prepare_v2(db.get(),
                       "SELECT sql FROM sqlite_master WHERE name='payment_methods';",
                       -1, &stmt, nullptr);

    if (sqlite3_step(stmt) == SQLITE_ROW) {
        const char* schema = (const char*)sqlite3_column_text(stmt, 0);
        LOGI("PAYMENT_METHODS SCHEMA: %s", schema);
    }
    sqlite3_finalize(stmt);

}

extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_initDatabase(
        JNIEnv *env,
        jobject,
        jstring path) {

    try {

        const char* dbPath = env->GetStringUTFChars(path, nullptr);

        database = new Database(dbPath);

        LOGI("DB PATH: %s", dbPath);
        ensureSchema(*database);

        expenseRepository = new SQLiteExpenseRepository(*database);
        revenueRepository = new SQLiteRevenueRepository(*database);
        paymentMethodRepository = new SQLitePaymentMethodRepository(*database);
        summaryUseCase = new GetMonthlySummaryUseCase(*expenseRepository, *revenueRepository);
        addRevenue = new AddRevenueUseCase(*revenueRepository);
        addExpenseUseCase = new AddExpenseUseCase(*expenseRepository, *revenueRepository, *paymentMethodRepository);

        env->ReleaseStringUTFChars(path, dbPath);

    } catch (const std::exception& e) {
        __android_log_print(
                ANDROID_LOG_ERROR,
                "EXPENSE_CORE",
                "InitDatabase error: %s",
                e.what()
        );
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_addRevenueUseCase(JNIEnv *env, jobject thiz,
                                                               jlong amount, jint day, jint month,
                                                               jint year) {
    if(addRevenue == nullptr)
        return;

    Date date(day, month, year);
    Money money(amount);

    addRevenue->execute(money, date);
    LOGI("Revenue inserted successfully");
}
std::string getRevenuesForMonth(int month, int year) {
    Month m = summaryUseCase->execute(month, year);
    std::basic_stringstream<char> ss;

    for(const auto& r : m.getRevenues()){
        ss << r.getId() << ";"
            << r.getAmount().getCents() << ";"
            << r.getDate().toISO() << ";"
            << "\n";
    }
    return ss.str();
}
extern "C"
JNIEXPORT jstring JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getRevenuesForMonth(JNIEnv *env, jobject thiz,
                                                                 jint month, jint year) {
    std::string result = getRevenuesForMonth(month, year);
    return env->NewStringUTF(result.c_str());
}
void addExpenseToRevenue(int revenueId, Money amount, int day, int month, int year){
    Money money(amount);
    Date date(day, month, year);

    int categoryId = 1;      // fixo por enquanto
    int paymentMethodId = 1; // fixo por enquanto

    addExpenseUseCase->execute(
            revenueId,
            money,
            date,
            categoryId,
            paymentMethodId
    );
}
extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_addExpenseToRevenue(JNIEnv *env, jobject thiz,
                                                                 jint revenue_id, jlong amount,
                                                                 jint day, jint month, jint year) {
    try {
        addExpenseToRevenue(revenue_id, amount, day, month, year);
    }catch (const std::exception& e){
        __android_log_print(ANDROID_LOG_ERROR, "CoreBridge", "Error: %s", e.what());
    }
}