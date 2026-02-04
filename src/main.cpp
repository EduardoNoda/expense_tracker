#include "domain/Expense.h"
#include "domain/Money.h"
#include "domain/Date.h"
#include "domain/Revenue.h"
#include "domain/Month.h"



int main() {
    Date d(10, 2, 2026);
    Money m(12345);

    Expense e(m, d, 1, 1);

    std::cout << d.toString() << "\n";
    std::cout << m.toString() << "\n";

    std::cout << "----------------------------\n";
    std::cout << "\n";

    Revenue r(1, Money(300000), Date(1,2,2026)); // R$ 3000,00

    r.addExpense(Expense(Money(100000), Date(5,2,2026), 1, 1));
    r.addExpense(Expense(Money(250000), Date(10,2,2026), 1, 1));

    std::cout << r.remaining().toString();
    std::cout << "\n";
    std::cout << "----------------------------\n";
    std::cout << "\n";

    Month feb(2, 2026);

    Revenue r1(1, Money(300000), Date(1,2,2026)); // R$3000
    r1.addExpense(Expense(Money(100000), Date(5,2,2026), 1, 1)); // 1000
    r1.addExpense(Expense(Money(250000), Date(10,2,2026), 1, 1)); // 2500

    Revenue r2(2, Money(200000), Date(15,2,2026)); // R$2000
    r2.addExpense(Expense(Money(50000), Date(20,2,2026), 1, 1)); // 500

    feb.addRevenue(r1);
    feb.addRevenue(r2);

    std::cout << "Total receita: " << feb.totalRevenue().toString() << "\n";
    std::cout << "Total despesas: " << feb.totalExpenses().toString() << "\n";
    std::cout << "Saldo: " << feb.balance().toString() << "\n";

    std::cout << "----------------------------\n";
    std::cout << "\n";
    return 0;
}
