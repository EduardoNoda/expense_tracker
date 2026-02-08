#include "Schema.h"

#include <fstream>
#include <sstream>
#include <stdexcept>

Schema::Schema(Database& db)
    : database(db) {}

void Schema::ensure() {
    // 1. Ativar foreign keys (SQLite não ativa por padrão)
    database.exec("PRAGMA foreign_keys = ON;");

    // 2. Ler schema.sql
    std::ifstream file("schema.sql");
    if (!file.is_open()) {
        throw std::runtime_error("schema.sql not found");
    }

    std::stringstream buffer;
    buffer << file.rdbuf();

    // 3. Executar schema inteiro
    database.exec(buffer.str());
}
