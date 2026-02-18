#pragma once

#include "Money.h"
#include "Date.h"

class Expense {
public:
    Expense(
        int revenueId,
        Money amount,
        Date date,
        Date impactDate,
        int categoryId,
        int paymentMethodId
    );

    const int& getRevenueId() const;
    const Money& getAmount() const;
    const Date& getDate() const;
    const Date& getImpactDate() const;

    int getCategoryId() const;
    int getPaymentMethodId() const;

private:
    int revenueId;
    Money amount;
    Date date;
    Date impactDate;
    int categoryId;
    int paymentMethodId;
};