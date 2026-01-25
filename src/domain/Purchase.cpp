#include "domain/Purchase.h"
#include <stdexcept>

Purchase::Purchase(std::string location, double total)
    : location_(std::move(location)), total_(total)
{
    if (location_.empty()) {
        throw std::invalid_argument("Location cannot be empty");
    }
    if (total_ < 0.0) {
        throw std::invalid_argument("Total cannot be negative");
    }
}

Purchase Purchase::createSummary(std::string location, double total) {
    return Purchase(std::move(location), total);
}

const std::string& Purchase::location() const {
    return location_;
}

double Purchase::total() const {
    return total_;
}
