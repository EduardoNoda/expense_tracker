#include "AddExpenseUseCase.h"

AddExpenseUseCase::AddExpenseUseCase (
    ExpenseRepository& expenseRepository,
    RevenueRepository& revenueRepository)
    : expenseRepository(expenseRepository), revenueRepository(revenueRepository) {}

int AddExpenseUseCase::execute(int revenueId, Money money, Date date, int categoryId, int paymentMethodId) {
    Revenue revenue = revenueRepository.findById(revenueId);

    if (revenue.getDate().getMonth() != date.getMonth() ||
        revenue.getDate().getYear() != date.getYear()) {
        throw std::invalid_argument("Expense date must belong to revenue month");
    }

    Expense expense(money, date, categoryId, paymentMethodId);

    return expenseRepository.save(revenueId, expense);
}