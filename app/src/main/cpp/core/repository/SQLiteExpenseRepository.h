#pragma once

#include "../infrastructure/Database.h"
#include "ExpenseRepository.h"
#include <vector>
class SQLiteExpenseRepository : public ExpenseRepository{
public:
    explicit SQLiteExpenseRepository(Database& db);

    int save(int revenueId, const Expense& expense) override;
    std::vector<Expense> findByRevenue(int revenueId) override;
    std::vector<Expense> findByImpactMonth(int month, int year) override;
    void deleteById(int expenseId) override;
    void payCreditCardBill(int month, int year, int targetRevenueId) override;
    std::string checkDueInvoices(int todayDay, int todayMonth, int todayYear) override;
    void beginTransaction() override;
    void commitTransaction() override;
private:
    Database& database;
};