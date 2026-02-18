#pragma once

#include "Money.h"
#include "Date.h"
#include "Expense.h"
#include <vector>

class Revenue {
public:
    Revenue (std::string name, Money amaout, Date date);
    std::string getName() const;

    int getId() const;
    void setId(int id);
    const Money& getAmount() const;
    const Date& getDate() const;
    const std::vector<Expense> & getExpenses() const;
    void addExpense(const Expense&);

    Money totalExpenses() const;
    Money remaining() const;

private:
    int id;
    std::string name;
    Money amount;
    Date date;
    std::vector<Expense> expenses;
};