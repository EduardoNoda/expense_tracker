#pragma once

#include "Money.h"
#include "Date.h"

class Revenue {
public:
    Revenue (
        Money amount,
        Date date
    );

    const Money& getAmount() const;
    const Date& getDate() const;

private:
    Money amount;
    Date date;
};