#pragma once

#include <sqlite3.h>
#include <string.h>
#include <string>

class Database{
public:
    explicit Database (const std::string& path);
    ~Database();

    sqlite3* get();
private:
    sqlite3* db;
};