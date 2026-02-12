#pragma once

#include "Revenue.h"
#include <vector>
class Month {
public:
    explicit Month(int month, int year);

    void addRevenue(const Revenue&);

    Money totalRevenue() const;
    Money totalExpenses() const;
    Money balance() const;

    Money getTotalRevenue() const;
    Money getTotalExpenses() const;
    Money getBalance() const;
private:
    int month, year;
    std::vector<Revenue> revenues;
};