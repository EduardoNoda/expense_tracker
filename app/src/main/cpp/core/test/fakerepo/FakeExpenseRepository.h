#pragma once

#include <map>
#include <vector>

#include "repository/ExpenseRepository.h"

class FakeExpenseRepository : public ExpenseRepository {
public:
    int save(int revenueId, const Expense& expense) override {
        expenses[revenueId].push_back(expense);
        return ++counter;
    }

    std::vector<Expense> findByRevenue(int revenueId) override {
        return expenses[revenueId];
    }

private:
    int counter = 0;
    std::map<int, std::vector<Expense>> expenses;
};