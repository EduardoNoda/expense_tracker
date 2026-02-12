#include <jni.h>
#include "../core/infrastructure/Database.h"
#include "../core/repository/SQLiteExpenseRepository.h"
#include "../core/repository/SQLiteRevenueRepository.h"
#include "../core/application/GetMonthlySummaryUseCase.h"
#include "../core/application/AddRevenueUseCase.h"
#include <android/log.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "EXPENSE_CORE", __VA_ARGS__)


static Database* database = nullptr;
static SQLiteExpenseRepository* expenseRepository = nullptr;
static SQLiteRevenueRepository* revenueRepository = nullptr;
static GetMonthlySummaryUseCase* summaryUseCase = nullptr;
static AddRevenueUseCase* addRevenue = nullptr;

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

    CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE
    );

    CREATE TABLE IF NOT EXISTS payment_methods (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE,
        type TEXT NOT NULL CHECK (type IN ('DEBIT','CREDIT','PIX','CASH')),
        closing_day INTEGER CHECK (closing_day BETWEEN 1 AND 31)
    );

    CREATE TABLE IF NOT EXISTS revenues (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
        date TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS expenses (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        revenue_id INTEGER NOT NULL,
        amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
        date TEXT NOT NULL,
        category_id INTEGER NOT NULL,
        payment_method_id INTEGER NOT NULL,
        created_at TEXT NOT NULL DEFAULT (datetime('now')),

        FOREIGN KEY (revenue_id) REFERENCES revenues(id) ON DELETE CASCADE,
        FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE RESTRICT
    );

    )");
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

        LOGI("Database path: %s", dbPath);

        ensureSchema(*database);

        expenseRepository = new SQLiteExpenseRepository(*database);
        revenueRepository = new SQLiteRevenueRepository(*database);
        summaryUseCase = new GetMonthlySummaryUseCase(*expenseRepository, *revenueRepository);
        addRevenue = new AddRevenueUseCase(*revenueRepository);

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