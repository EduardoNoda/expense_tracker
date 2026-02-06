#include "GetMonthlySummaryUseCase.h"

GetMonthlySummaryUseCase::GetMonthlySummaryUseCase(
    ExpenseRepository& expenseRepository, 
    RevenueRepository& revenueRepository) 
    : expenseRepository(expenseRepository), revenueRepository(revenueRepository) {}

Month GetMonthlySummaryUseCase::execute(int month, int year) {
    Month result(month, year);
    
    auto revenues = revenueRepository.findByMonth(month, year);

    for(auto& r : revenues) {
        auto expenses = expenseRepository.findByRevenue(r.getId());
        for(auto& e : expenses) {
            r.addExpense(e);
        }
        result.addRevenue(r);
    }

    return result;
}