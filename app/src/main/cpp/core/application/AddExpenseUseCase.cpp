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
        int dueDay = paymentMethod.getDueDay();

        int purchaseDay = date.getDay();
        int impactYear = date.getYear();
        int impactMonth = date.getMonth();

        if(purchaseDay > closingDay) {
            impactMonth++;
            if (impactMonth == 13) {
                impactMonth = 1;
                impactYear++;
            }
        }

        impactDate = Date(dueDay, impactMonth, impactYear);
    }

    return expenseRepository.save(revenueId, expense);
}

