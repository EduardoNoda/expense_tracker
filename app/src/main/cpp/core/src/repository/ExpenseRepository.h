#pragma once

#include "../domain/Expense.h"
#include <vector>

class ExpenseRepository{
public:
    virtual ~ExpenseRepository() = default;

    virtual int save(int revenueId, const Expense& expense) = 0;
    virtual std::vector<Expense> findByRevenue(int revenueId) = 0;
};