#pragma once

#include <string>

enum class PaymentType {
    IMMEDIATE, CREDIT
};

class PaymentMethod {
public:
    PaymentMethod(int id, std::string name,PaymentType type, int closingDay, int dueDay);

    std::string getName() const;
    bool isCredit() const;
    int getClosingDay() const;
    int getDueDay() const;
private:
    int id;
    std::string name;
    PaymentType type;
    int closingDay;
    int dueDay;
};