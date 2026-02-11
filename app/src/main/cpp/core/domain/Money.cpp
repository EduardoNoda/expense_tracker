#include "Money.h"

#include <iomanip>
#include <sstream>

Money::Money(long long cents) 
    : valueInCents(cents) {}

long long Money::getCents() const {
    return valueInCents;
}

Money Money::operator+(const Money& other) const {
    return Money(valueInCents + other.valueInCents);
}

Money Money::operator-(const Money& other) const {
    return Money(valueInCents - other.valueInCents);
}

bool Money::operator<(const Money& other) const{
    return valueInCents < other.valueInCents;
}

bool Money::operator==(const Money& other) const{
    return valueInCents == other.valueInCents;
}

std::string Money::toString() const {
    long long reais = valueInCents / 100;
    long long cents = std::llabs(valueInCents % 100);

    std::stringstream valueFormatted;
    valueFormatted << "R$" << reais << "." << std::setw(2) << std::setfill('0') << cents;
    return valueFormatted.str();
}