#pragma once

#include <string>

class Money {
public:
    Money(long long cents = 0);

    long long getCents() const;

    Money operator+(const Money& other) const;
    Money operator-(const Money& other) const;

    bool operator==(const Money& other) const;
    bool operator<(const Money& other) const;

    std::string toString() const;
private:
    long long valueInCents;
};