#include "MoneyManager.h"

#include <iomanip>
#include <sstream>

monataryValue::monataryValue() : value(0) {}
monataryValue::monataryValue(long long value) : value(value) {}

std::string monataryValue::toString() const {
    std::stringstream valueFormatted;
    valueFormatted << "R$" << (value) << "." << std::setw(2) << std::setfill('0') << std::abs(value % 100);
    return valueFormatted.str();
}