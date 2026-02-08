#pragma once

#include "../infrastructure/Database.h"
#include "ExpenseRepository.h"
#include <vector>
class SQLiteExpenseRepository : public ExpenseRepository{
public:
    explicit SQLiteExpenseRepository(Database& db);

    int save(int revenueId, const Expense& expense) override;
    std::vector<Expense> findByRevenue(int revenueId) override;
private:
    Database& database;
};