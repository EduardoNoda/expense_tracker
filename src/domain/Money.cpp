#include "Money.h"

#include <iomanip>
#include <sstream>

Money::Money() : valueInCents(0) {}
Money::Money(long long value) : valueInCents(value) {}

std::string Money::toString() const {
    long long reais = valueInCents / 100;
    long long cents = std::llabs(valueInCents % 100);

    std::stringstream valueFormatted;
    valueFormatted << "R$" << reais << "." << std::setw(2) << std::setfill('0') << cents;
    return valueFormatted.str();
}