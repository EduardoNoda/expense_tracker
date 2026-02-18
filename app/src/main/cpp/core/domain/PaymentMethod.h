#pragma once

enum class PaymentType {
    IMMEDIATE, CREDIT
};

class PaymentMethod {
public:
    PaymentMethod(int id, PaymentType type, int closingDay);

    bool isCredit() const;
    int getClosingDay() const;
    int getDueDay() const;
private:
    int id;
    PaymentType type;
    int closingDay;
    int dueDay;
};