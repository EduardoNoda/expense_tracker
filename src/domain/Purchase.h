#pragma once

#include <string>

class Purchase {
public:
    static Purchase createSummary(std::string location, double total);

    const std::string& location() const;
    double total() const;

private:
    Purchase(std::string location, double total);

    std::string location_;
    double total_;
};
