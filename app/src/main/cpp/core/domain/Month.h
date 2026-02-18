#pragma once

#include "Revenue.h"
#include <vector>
class Month {
public:
    explicit Month(int month, int year);

    void addRevenue(const Revenue&);
    void addExpense(const Expense&);

    Money totalRevenue() const;
    Money totalExpenses() const;
    Money balance() const;

    const std::vector<Revenue>& getRevenues() const;
    const std::vector<Expense>& getExpenses() const;
    Money getTotalRevenue() const;
    Money getTotalExpenses() const;
    Money getBalance() const;
private:
    int month, year;
    std::vector<Revenue> revenues;
    std::vector<Expense> expenses;
};