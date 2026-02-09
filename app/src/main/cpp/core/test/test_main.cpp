#include <cassert>
#include <iostream>
#include <stdexcept>

#include "domain/Money.h"
#include "domain/Date.h"
#include "domain/Revenue.h"
#include "domain/Expense.h"
#include "domain/Month.h"

#include "application/AddRevenueUseCase.h"
#include "application/AddExpenseUseCase.h"
#include "application/GetMonthlySummaryUseCase.h"

#include "fakerepo/FakeRevenueRepository.h"
#include "fakerepo/FakeExpenseRepository.h"

void testMoneyOperations() {
    Money a(1000); // R$10,00
    Money b(500);  // R$5,00

    Money c = a + b;
    assert(c.getCents() == 1500);

    Money d = a - b;
    assert(d.getCents() == 500);
}
void testInvalidDate() {
    try {
        Date d(31, 2, 2026); // data inválida
        assert(false);      // não deveria chegar aqui
    } catch (const std::invalid_argument&) {
        assert(true);
    }
}
void testDateISO() {
    Date d(10, 2, 2026);
    std::string iso = d.toISO();

    assert(iso == "2026-02-10");

    Date parsed = Date::fromISO(iso);
    assert(parsed == d);
}
void testRevenueRemaining() {
    Revenue r(Money(300000), Date(1,2,2026)); // 3000

    r.addExpense(Expense(
        Money(100000),
        Date(5,2,2026),
        1,
        1
    ));

    r.addExpense(Expense(
        Money(50000),
        Date(10,2,2026),
        1,
        1
    ));

    assert(r.totalExpenses().getCents() == 150000);
    assert(r.remaining().getCents() == 150000);
}
void testAddExpenseHappyPath() {
    FakeRevenueRepository revenueRepo;
    FakeExpenseRepository expenseRepo;

    AddRevenueUseCase addRevenue(revenueRepo);
    AddExpenseUseCase addExpense(expenseRepo, revenueRepo);

    int revenueId = addRevenue.execute(
        Money(200000),
        Date(1,2,2026)
    );

    int expenseId = addExpense.execute(
        revenueId,
        Money(50000),
        Date(5,2,2026),
        1,
        1
    );

    assert(expenseId > 0);
}
void testAddExpenseInvalidRevenue() {
    FakeRevenueRepository revenueRepo;
    FakeExpenseRepository expenseRepo;

    AddExpenseUseCase addExpense(expenseRepo, revenueRepo);

    try {
        addExpense.execute(
            999,
            Money(1000),
            Date(5,2,2026),
            1,
            1
        );
        assert(false);
    } catch (const std::runtime_error&) {
        assert(true);
    }
}
void testMonthlySummary() {
    FakeRevenueRepository revenueRepo;
    FakeExpenseRepository expenseRepo;

    AddRevenueUseCase addRevenue(revenueRepo);
    AddExpenseUseCase addExpense(expenseRepo, revenueRepo);
    GetMonthlySummaryUseCase summary(expenseRepo, revenueRepo);

    int r1 = addRevenue.execute(Money(300000), Date(1,2,2026));
    addExpense.execute(r1, Money(100000), Date(5,2,2026), 1, 1);

    Month result = summary.execute(2, 2026);

    assert(result.totalRevenue().getCents() == 300000);
    assert(result.totalExpenses().getCents() == 100000);
    assert(result.balance().getCents() == 200000);
}

int main() {
    testMoneyOperations();
    testDateISO();
    testInvalidDate();
    testRevenueRemaining();
    testAddExpenseHappyPath();
    testAddExpenseInvalidRevenue();
    testMonthlySummary();

    std::cout << "ALL TESTS PASSED\n";
    return 0;
}
