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