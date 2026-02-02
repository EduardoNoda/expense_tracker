#pragma once

#include <iostream>
#include <string>

class monataryValue {
    long long valueInCents;
public:
    monataryValue();
    monataryValue(long long value);

    std::string toString() const;
};