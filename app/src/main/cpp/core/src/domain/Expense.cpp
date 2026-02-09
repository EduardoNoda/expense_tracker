#include "Expense.h"
#include <stdexcept>

Expense::Expense(
    Money amount,
    Date date,
    int categoryId,
    int paymentMethodId
)
    : amount(amount),
      date(date),
      categoryId(categoryId),
      paymentMethodId(paymentMethodId)
{
    if (categoryId <= 0)
        throw std::invalid_argument("Invalid category id");

    if (paymentMethodId <= 0)
        throw std::invalid_argument("Invalid payment method id");
}

const Money& Expense::getAmount() const { return amount; }
const Date& Expense::getDate() const { return date; }
int Expense::getCategoryId() const { return categoryId; }
int Expense::getPaymentMethodId() const { return paymentMethodId; }