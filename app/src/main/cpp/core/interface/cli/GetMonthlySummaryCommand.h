#pragma once

#include "Command.h"
#include "../../application/GetMonthlySummaryUseCase.h"

class GetMonthlySummaryCommand : public Command {
public:
    GetMonthlySummaryCommand(
        GetMonthlySummaryUseCase& getMonthlySummaryUseCase,
        int month,
        int year
    );

    std::string execute() override;
private:
    GetMonthlySummaryUseCase& getMonthlySummaryUseCase;
    int month;
    int year;
};