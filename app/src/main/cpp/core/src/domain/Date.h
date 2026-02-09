#pragma once

#include <string>

class Date {
public:
    Date(int day, int month, int year);
    
    int getDay() const;
    int getMonth() const;
    int getYear() const;

    std::string toString() const;

    bool operator==(const Date& other) const;
    bool operator<(const Date& other) const;
    
    std::string toISO() const;
    static Date fromISO(const std::string& iso);
    static Date firstDayOfNextMonth(int month, int year);

private:
    int day, month, year;
};