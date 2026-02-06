#pragma once

#include "Money.h"
#include "Date.h"
#include "Expense.h"
#include <vector>

class Revenue {
public:
    Revenue (Money amaout, Date date);

    int getId() const;
    void setId(int id);
    const Money& getAmount() const;
    const Date& getDate() const;

    void addExpense(const Expense&);

    Money totalExpenses() const;
    Money remaining() const;

private:
    int id;
    Money amount;
    Date date;

    std::vector<Expense> expenses;
};