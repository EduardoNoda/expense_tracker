#include "AddExpenseUseCase.h"
#include <stdexcept>

AddExpenseUseCase::AddExpenseUseCase (
    ExpenseRepository& expenseRepository,
    RevenueRepository& revenueRepository,
    PaymentMethodRepository& paymentMethodRepository)
    : expenseRepository(expenseRepository), revenueRepository(revenueRepository), paymentMethodRepository(paymentMethodRepository){}

int AddExpenseUseCase::execute(int revenueId, Money money, Date date, int categoryId, int paymentMethodId) {
    Revenue revenue = revenueRepository.findById(revenueId);

    if (revenue.getDate().getMonth() != date.getMonth() ||
        revenue.getDate().getYear() != date.getYear()) {
        throw std::invalid_argument("Expense date must belong to revenue month");
    }

    Expense expense(money, date, categoryId, paymentMethodId);

    PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId);
    Date impactDate = date;

    if(paymentMethod.isCredit()) {
        int closingDay = paymentMethod.getClosingDay();

        int year = date.getYear();
        int month = date.getMonth() + 1;

        if (month == 13) {
            month = 1;
            year++;
        }

        impactDate = Date(closingDay, month, year);
    }

    return expenseRepository.save(revenueId, expense);
}

