#include "MoneyManager.h"

#include <iomanip>
#include <sstream>

monataryValue::monataryValue() : valueInCents(0) {}
monataryValue::monataryValue(long long value) : valueInCents(value) {}

std::string monataryValue::toString() const {
    std::stringstream valueFormatted;
    valueFormatted << "R$" << (valueInCents) << "." << std::setw(2) << std::setfill('0') << std::abs(valueInCents % 100);
    return valueFormatted.str();
}