#include "AddExpenseCommand.h"

AddExpenseCommand::AddExpenseCommand(
        AddExpenseUseCase& uc,
        int revenueId,
        Money money,
        Date date,
        int categoryId,
        int paymentMethodId ) 
        : uc(uc), revenueId(revenueId), money(money), date(date), categoryId(categoryId), paymentMethodId(paymentMethodId){}

std::string AddExpenseCommand::execute() {
    uc.execute(revenueId, money, date, categoryId, paymentMethodId);
    return "Expense added successfully";
}