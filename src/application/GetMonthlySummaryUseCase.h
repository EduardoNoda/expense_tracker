#pragma once

#include "../repository/ExpenseRepository.h"
#include "../repository/RevenueRepository.h"
#include "../domain/Month.h"

class GetMonthlySummaryUseCase {
public:
    explicit GetMonthlySummaryUseCase(
        ExpenseRepository& expenseRepository,
        RevenueRepository& revenueRepository
    );

    Month execute(int month, int year);
private:
    ExpenseRepository& expenseRepository;
    RevenueRepository& revenueRepository;
};