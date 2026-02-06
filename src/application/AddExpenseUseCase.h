#pragma once

#include "../repository/ExpenseRepository.h"
#include "../repository/RevenueRepository.h"


class AddExpenseUseCase {
public:
    explicit AddExpenseUseCase(
        ExpenseRepository& expenseRepository,
        RevenueRepository& revenueRepository
    );

    int execute(int revenueId, Money amountCents, Date date, int categoryId, int paymentMethodId);
private:
    ExpenseRepository& expenseRepository;
    RevenueRepository& revenueRepository;
};