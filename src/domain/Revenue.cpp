#include "Revenue.h"
#include "Expense.h"
#include "Money.h"
#include <stdexcept>

Revenue::Revenue(int id, Money amount, Date date) 
    : id(id), amount(amount), date(date){
    if(id <= 0){
        throw new std::invalid_argument("Id precisa ser positivo");
    }
}
int Revenue::getId() const {return id;}
const Money& Revenue::getAmount() const {return amount;}
const Date& Revenue::getDate() const {return date;}

void Revenue::addExpense(const Expense& expense) {
    expenses.push_back(expense);
}

Money Revenue::totalExpenses() const{
    Money total(0);

    for(const auto& e : expenses) total = total + e.getAmount();

    return total;
}

Money Revenue::remaining() const{
    return amount - totalExpenses();
}