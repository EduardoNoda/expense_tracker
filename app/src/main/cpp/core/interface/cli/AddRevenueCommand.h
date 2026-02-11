#pragma once

#include "Command.h"
#include "../../application/AddRevenueUseCase.h"
#include "../../domain/Money.h"
#include "../../domain/Date.h"

class AddRevenueCommand : public Command {
public:
    AddRevenueCommand(
        AddRevenueUseCase& useCase,
        long long amountCents,
        const Date& date
    );

    std::string execute() override;

private:
    AddRevenueUseCase& useCase;
    long long amountCents;
    Date date;
};
