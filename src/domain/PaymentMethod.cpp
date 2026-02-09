#include "PaymentMethod.h"

PaymentMethod::PaymentMethod(int id, PaymentType type, int closingDay) 
    : id(id), type(type), closingDay(closingDay) {}
    
bool PaymentMethod::isCredit() const {
    return type == PaymentType::CREDIT;
}

int PaymentMethod::getClosingDay() const {
    return closingDay;
}