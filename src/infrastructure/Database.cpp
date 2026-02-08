#include "Database.h"
#include <sqlite3.h>
#include <stdexcept>

Database::Database (const std::string& path) 
    : db(nullptr) {
        if(sqlite3_open(path.c_str(), &db) != SQLITE_OK)
            throw new std::invalid_argument("Falha ao tentar abrir o banco de dados\n");
}

Database::~Database() {
    if(db) sqlite3_close(db);
}

sqlite3* Database::get() {
    return db;
}

void Database::exec(const std::string& sql) {
    char* errMsg = nullptr;

    if (sqlite3_exec(db, sql.c_str(), nullptr, nullptr, &errMsg) != SQLITE_OK) {
        std::string error = errMsg ? errMsg : "Unknown SQL error";
        sqlite3_free(errMsg);
        throw std::runtime_error(error);
    }
}