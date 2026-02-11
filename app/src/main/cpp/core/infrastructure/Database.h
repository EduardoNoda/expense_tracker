#pragma once

#include <sqlite3.h>
#include <string>

class Database{
public:
    explicit Database (const std::string& path);
    ~Database();

    sqlite3* get();

    void exec(const std::string& sql);
private:
    sqlite3* db;
};