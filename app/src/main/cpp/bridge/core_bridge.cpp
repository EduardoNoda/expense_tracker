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

// --- FUNÇÃO AUXILIAR PARA SERIALIZAR LISTAS (NOVO) ---
// Evita duplicar código para enviar listas ao Kotlin
std::string serializeCursor(sqlite3_stmt* stmt, int colCount) {
    std::stringstream ss;
    while(sqlite3_step(stmt) == SQLITE_ROW) {
        for(int i = 0; i < colCount; i++) {
            const char* val = (const char*)sqlite3_column_text(stmt, i);
            ss << (val ? val : "") << ";";
        }
        ss << "\n";
    }
    return ss.str();
}
static void ensureSchema(Database& db) {
    db.exec("PRAGMA foreign_keys = ON;");

    /*db.exec(R"(
    DROP TABLE IF EXISTS expenses;
    DROP TABLE IF EXISTS revenues;
    DROP TABLE IF EXISTS payment_methods;
    DROP TABLE IF EXISTS categories;
    )");*/

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL UNIQUE
    );
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS payment_methods (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        type TEXT NOT NULL CHECK (type IN ('IMMEDIATE', 'CREDIT')),
        closing_day INTEGER CHECK (closing_day IS NULL OR closing_day BETWEEN 1 AND 31),
        due_day INTEGER CHECK (due_day IS NULL OR due_day BETWEEN 1 AND 31)
    );
    )");

    db.exec(R"(
    CREATE TABLE IF NOT EXISTS revenues (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
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
    INSERT OR IGNORE INTO categories (id, name) VALUES (1, 'Academia');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (2, 'Barbeiro');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (3, 'Bares e Restaurantes');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (4, 'Cartão de Crédito');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (5, 'Combustível');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (6, 'Condomínio');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (7, 'Diarista');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (8, 'Energia');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (9, 'Entretenimento');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (10, 'Farmácia');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (11, 'IPTU');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (12, 'IPVA');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (13, 'Manutenção da Casa');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (14, 'Manutenção do Veículo');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (15, 'Mercado');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (16, 'Padaria');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (17, 'Parcela do Veículo');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (18, 'Pet');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (19, 'Plano de Saúde');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (20, 'Prestação da Casa');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (21, 'Salão');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (22, 'Saneamento');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (23, 'Seguro');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO categories (id, name) VALUES (24, 'Outros');
    )");

    db.exec(R"(
    INSERT OR IGNORE INTO payment_methods(id, name, type) VALUES (1, 'À Vista', 'IMMEDIATE')
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
JNIEXPORT jlongArray JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getMonthSummary(JNIEnv *env, jobject thiz, jint month, jint year) {
    if(summaryUseCase == nullptr)
        return env->NewLongArray(0);
    try {
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
    } catch (const std::exception& e) {
        LOGI("Error getting summary: %s", e.what());
        return env->NewLongArray(0);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_addRevenueUseCase(JNIEnv *env, jobject thiz, jstring name,
                                                               jlong amount, jint day, jint month,
                                                               jint year) {
    if(addRevenue == nullptr) return;

    const char *nameCStr = env->GetStringUTFChars(name, nullptr);
    try {
        std::string nameStr(nameCStr);

        Date date(day, month, year);
        Money money(amount);

        addRevenue->execute(nameCStr, money, date);
        LOGI("Revenue inserted successfully");
    } catch (const std::exception& e) {
        LOGI("Error adding revenue: %s", e.what());
    }
    env->ReleaseStringUTFChars(name, nameCStr);
}
extern "C"
JNIEXPORT jstring JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getRevenuesForMonth(JNIEnv *env, jobject thiz, jint month, jint year) {
    if(summaryUseCase == nullptr) return env->NewStringUTF("");

    try {
        Month m = summaryUseCase->execute(month, year);
        std::basic_stringstream<char> ss;

        for (const auto &r: m.getRevenues()) {
            // 1. Dados da Receita
            ss << r.getId() << ";"
               << r.getAmount().getCents() << ";"
               << r.getDate().toISO() << ";"
               << r.getName() << "|"; // Pipe separador

            // 2. Dados das Despesas (Isso estava faltando no seu código enviado)
            // 2. Dados das Despesas (Agora enviando Valor, Categoria E Forma de Pagamento)
            for (const auto &e : r.getExpenses()) {
                ss << e.getId() << ";"
                   << e.getAmount().getCents() << ";"
                   << e.getCategoryId() << ";"
                   << e.getPaymentMethodId() << "#";
            }

            ss << "\n";
        }
        return env->NewStringUTF(ss.str().c_str());
    } catch (const std::exception& e) {
        LOGI("Error listing revenues: %s", e.what());
        return env->NewStringUTF("");
    }
}
extern "C"
JNIEXPORT jstring JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getAllCategories(JNIEnv *env, jobject thiz) {
    if(database == nullptr) return env->NewStringUTF("");

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database->get(), "SELECT id, name FROM categories ORDER BY name", -1, &stmt, nullptr);
    std::string res = serializeCursor(stmt, 2); // id;name;
    sqlite3_finalize(stmt);
    return env->NewStringUTF(res.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getPaymentMethods(JNIEnv *env, jobject thiz) {
    if(database == nullptr) return env->NewStringUTF("");

    sqlite3_stmt* stmt;
    sqlite3_prepare_v2(database->get(), "SELECT id, name FROM payment_methods ORDER BY id", -1, &stmt, nullptr);
    std::string res = serializeCursor(stmt, 2);
    sqlite3_finalize(stmt);
    return env->NewStringUTF(res.c_str());
}
// REFATORADO: Agora aceita categoryId e paymentMethodId vindo da UI
// ... includes ...

extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_addExpenseToRevenue(JNIEnv *env, jobject thiz,
                                                                 jint revenue_id,
                                                                 jlong amount,
                                                                 jint day, jint month, jint year,
                                                                 jint categoryId,
                                                                 jint paymentMethodId,
                                                                 jint installments // <--- NOVO
) {
    if(addExpenseUseCase == nullptr) return;

    try {
        Money money(amount);
        Date date(day, month, year);
        Date impactDate = date;

        addExpenseUseCase->execute(
                revenue_id,
                money,
                date,
                impactDate,
                categoryId,
                paymentMethodId,
                installments // <--- Passando pro Core
        );
    } catch (const std::exception& e){
        LOGI("Error adding expense: %s", e.what());
    }
}
// Adicione essa função no final do arquivo
extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_addPaymentMethod(JNIEnv *env, jobject thiz,
                                                              jstring name,
                                                              jint closingDay, jint dueDay) {
    if(paymentMethodRepository == nullptr) return;

    const char *nameCStr = env->GetStringUTFChars(name, nullptr);
    try {
        // Assume sempre CREDIT para cartões adicionados pelo usuário no MVP
        paymentMethodRepository->save(std::string(nameCStr), "CREDIT", closingDay, dueDay);
        LOGI("New credit card added: %s", nameCStr);
    } catch (const std::exception& e) {
        LOGI("Error adding card: %s", e.what());
    }
    env->ReleaseStringUTFChars(name, nameCStr);
}
extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_deleteExpenseById(JNIEnv *env, jobject thiz,
                                                               jint expense_id) {
    if(expenseRepository == nullptr) return;
    int id = static_cast<int>(expense_id);
    expenseRepository->deleteById(id);
}

extern "C"
JNIEXPORT void JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_deleteRevenueById(JNIEnv *env, jobject thiz,
                                                               jint revenue_id) {
    if(revenueRepository == nullptr) return;

    int id = static_cast<int>(revenue_id);
    revenueRepository->deleteById(id);
}