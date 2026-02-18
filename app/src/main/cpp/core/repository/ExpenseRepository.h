#pragma once

#include "../domain/Expense.h"
#include <vector>

class ExpenseRepository{
public:
    virtual ~ExpenseRepository() = default;

    virtual int save(int revenueId, const Expense& expense) = 0;
    virtual std::vector<Expense> findByRevenue(int revenueId) = 0;
    virtual std::vector<Expense> findByImpactMonth(int month, int year) = 0;
    virtual void beginTransaction() = 0;
    virtual void commitTransaction() = 0;
};