#include "Expense.h"
#include <stdexcept>
Expense::Expense(
        int revenueId,
        Money amount,
        Date date,
        Date impactDate,
        int categoryId,
        int paymentMethodId
)
        :
        id(0),
        revenueId(revenueId),
        amount(amount),
        date(date),
        impactDate(impactDate),
        categoryId(categoryId),
        paymentMethodId(paymentMethodId)
{
    if (categoryId <= 0)
        throw std::invalid_argument("Invalid category id");

    if (paymentMethodId <= 0)
        throw std::invalid_argument("Invalid payment method id");
}
Expense::Expense(
    int id,
    int revenueId,
    Money amount,
    Date date,
    Date impactDate,
    int categoryId,
    int paymentMethodId
)
    :
      id(id),
      revenueId(revenueId),
      amount(amount),
      date(date),
      impactDate(impactDate),
      categoryId(categoryId),
      paymentMethodId(paymentMethodId)
{
    if (categoryId <= 0)
        throw std::invalid_argument("Invalid category id");

    if (paymentMethodId <= 0)
        throw std::invalid_argument("Invalid payment method id");
}

const int& Expense::getId() const { return id; }
const int& Expense::getRevenueId() const { return revenueId; }
const Money& Expense::getAmount() const { return amount; }
const Date& Expense::getDate() const { return date; }
const Date& Expense::getImpactDate() const { return impactDate; }
int Expense::getCategoryId() const { return categoryId; }
int Expense::getPaymentMethodId() const { return paymentMethodId; }