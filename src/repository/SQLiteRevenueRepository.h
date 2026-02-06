#pragma once

#include "RevenueRepository.h"
#include "Database.h"
#include <vector>

class SQLiteRevenueRepository : public RevenueRepository{
public:
    explicit SQLiteRevenueRepository(Database& db);

    int save(const Revenue& revenue) override;
    std::vector<Revenue> findByMonth(int month, int year) override;
    Revenue findById(int revenueId) override;
private:
    Database& database;
};