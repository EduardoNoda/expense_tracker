#pragma once

#include <string>
#include "../../sqlite3/sqlite3.h"

class Database{
public:
    explicit Database (const std::string& path);
    ~Database();

    sqlite3* get();

    void exec(const std::string& sql);
    void beginTransaction();
    void commitTransaction();
private:
    sqlite3* db;
};