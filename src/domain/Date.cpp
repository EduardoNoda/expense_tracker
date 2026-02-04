#include "Date.h"

#include <iomanip>
#include <iostream>
#include <sstream>
#include <stdexcept>



int Date::getDay() const {return day;}

int Date::getMonth() const {return month;}

int Date::getYear() const {return year;}

std::string Date::toString () const {
    std::stringstream dateFormatted;
    dateFormatted << std::setw(2) << std::setfill('0') << day << "/" 
                << std::setw(2) << std::setfill('0') << month << "/" 
                << year;
    return dateFormatted.str();
}

void validateDate (int getDay, int getMonth, int getYear) {
    int daysInMonth;

    bool isLeapYear = (getYear % 4 == 0 && getYear % 100 != 0) || (getYear % 400 == 0);

    switch (getMonth) {
        case 1: case 3: case 5: case 7: case 8: case 10: case 12:
            daysInMonth = 31;
            break;
        case 4: case 6: case 9: case 11:
            daysInMonth = 30;
            break;
        case 2:
            daysInMonth = isLeapYear ? 29 : 28;
            break;
        default:
            throw std::invalid_argument("Invalid Month\n");
    }

    if (getDay < 1 || getDay > daysInMonth) 
        throw std::invalid_argument("Invalid Day\n");
    
    if (getYear < 1900 || getYear > 3000)
        throw std::invalid_argument("Invalid Year");
}

bool Date::operator<(const Date& other) const {
    if (year != other.year) return year < other.year;
    if (month != other.month) return month < other.month;
    return day < other.day;
}

bool Date::operator==(const Date& other) const {
    if (day != other.day || month != other.month || year != other.year) return false;
    return true;
}

std::string Date::toISO() const {
    std::stringstream ss;
    ss << year << "-"
       << std::setw(2) << std::setfill('0') << month << "-"
       << std::setw(2) << std::setfill('0') << day;
    return ss.str();
}

Date::Date(int d, int m, int y) {
    validateDate(d, m, y);
    day = d;
    month = m;
    year = y;
};