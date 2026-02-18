#include "PaymentMethod.h"

PaymentMethod::PaymentMethod(int id, std::string name, PaymentType type, int closingDay, int dueDay)
        : id(id), name(name), type(type), closingDay(closingDay), dueDay(dueDay) {}

std::string PaymentMethod::getName() const { return name; }

bool PaymentMethod::isCredit() const { return type == PaymentType::CREDIT; }

int PaymentMethod::getClosingDay() const { return closingDay; }

int PaymentMethod::getDueDay() const { return dueDay; }