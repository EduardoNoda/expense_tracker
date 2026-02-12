#include "Month.h"
#include "Money.h"
#include "Revenue.h"
#include <stdexcept>

Month::Month(int month, int year) 
    : month(month), year(year){
    if (month < 1 || month > 12)
        throw std::invalid_argument("Invalid month");

    if (year < 0)
        throw std::invalid_argument("Invalid year");
}

void Month::addRevenue(const Revenue& revenue) {
    revenues.push_back(revenue);
}

Money Month::totalRevenue() const {
    Money total(0);
    for(const auto& r : revenues) {
        total = total + r.getAmount();
    }
    return total;
}

Money Month::totalExpenses() const {
    Money total(0);
    for(const auto& e : revenues) {
        total = total + e.totalExpenses();
    }
    return total;
}

Money Month::balance() const {
    return totalRevenue() - totalExpenses();
}

Money Month::getTotalRevenue() const {
    return totalRevenue();
}

Money Month::getTotalExpenses() const {
    return totalExpenses();
}

Money Month::getBalance() const {
    return balance();
}