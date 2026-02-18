#pragma once

#include "../repository/ExpenseRepository.h"
#include "../repository/RevenueRepository.h"
#include "../repository/PaymentMethodRepository.h"

class AddExpenseUseCase {
public:
    explicit AddExpenseUseCase(
        ExpenseRepository& expenseRepository,
        RevenueRepository& revenueRepository,
        PaymentMethodRepository& paymentMethodRepository
    );

    int execute(int revenueId, Money amountCents, Date date, Date impactDate, int categoryId, int paymentMethodId, int installments);

private:
    ExpenseRepository& expenseRepository;
    RevenueRepository& revenueRepository;
    PaymentMethodRepository& paymentMethodRepository;
};