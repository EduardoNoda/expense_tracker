#include "GetMonthlySummaryUseCase.h"

GetMonthlySummaryUseCase::GetMonthlySummaryUseCase(
    ExpenseRepository& expenseRepository, 
    RevenueRepository& revenueRepository) 
    : expenseRepository(expenseRepository), revenueRepository(revenueRepository) {}

Month GetMonthlySummaryUseCase::execute(int month, int year) {
    Month result(month, year);
    auto revenues = revenueRepository.findByMonth(month, year);

    auto allExpensesInMonth = expenseRepository.findByImpactMonth(month, year);

    if (!revenues.empty()) {
        for (const auto& expense : allExpensesInMonth) {
            bool matched = false;

            for (auto& r : revenues) {
                if (r.getId() == expense.getRevenueId()) {
                    r.addExpense(expense);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                revenues[0].addExpense(expense);
            }
        }
    }
    for(auto& r : revenues) {
        result.addRevenue(r);
    }

    for(auto& e : allExpensesInMonth) {
        result.addExpense(e);
    }
    return result;
}