#pragma once

#include <iostream>
#include <string>

class Money {
    long long valueInCents;
public:
    Money();
    Money(long long value);

    std::string toString() const;
};