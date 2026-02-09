#pragma once

#include "../../infrastructure/Database.h"
#include "../../application/AddExpenseUseCase.h"
#include "../../domain/Money.h"
#include "../../domain/Date.h"
#include "Command.h"

class AddExpenseCommand : public Command {
public:
    AddExpenseCommand(
        AddExpenseUseCase& uc,
        int revenueId,
        Money money,
        Date date,
        int categoryId,
        int paymentMethodId
    );

    std::string execute() override;
private:
    AddExpenseUseCase& uc;
    int revenueId;
    Money money;
    Date date;
    int categoryId;
    int paymentMethodId;
};