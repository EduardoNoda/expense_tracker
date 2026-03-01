#include "GetMonthlySummaryUseCase.h"

GetMonthlySummaryUseCase::GetMonthlySummaryUseCase(
        ExpenseRepository& expenseRepository,
        RevenueRepository& revenueRepository)
        : expenseRepository(expenseRepository), revenueRepository(revenueRepository) {}

Month GetMonthlySummaryUseCase::execute(int month, int year) {
    Month result(month, year);
    auto revenues = revenueRepository.findByMonth(month, year);

    auto allExpensesInMonth = expenseRepository.findByImpactMonth(month, year);

    for (const auto& expense : allExpensesInMonth) {
        if (expense.getRevenueId() > 0) {
            for (auto& r : revenues) {
                if (r.getId() == expense.getRevenueId()) {
                    r.addExpense(expense);
                    break;
                }
            }
        }
    }

    for(auto& r : revenues) {
        result.addRevenue(r);
    }
    // Todas as despesas (inclusive as de cartão) ficam guardadas no Month!
    for(auto& e : allExpensesInMonth) {
        result.addExpense(e);
    }

    return result;
}