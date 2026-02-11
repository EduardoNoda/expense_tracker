#pragma once

#include "Database.h"

class Schema {
public:
    explicit Schema(Database& db);

    void ensure();

private:
    Database& database;
};
