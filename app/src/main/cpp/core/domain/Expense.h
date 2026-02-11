#pragma once

#include "Money.h"
#include "Date.h"

class Expense {
public:
    Expense(
        Money amount,
        Date date,
        int categoryId,
        int paymentMethodId
    );

    const Money& getAmount() const;
    const Date& getDate() const;

    int getCategoryId() const;
    int getPaymentMethodId() const;

private:
    Money amount;
    Date date;
    int categoryId;
    int paymentMethodId;
};