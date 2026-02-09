#include "GetMonthlySummaryCommand.h"

GetMonthlySummaryCommand::GetMonthlySummaryCommand(
    GetMonthlySummaryUseCase& getMonthlySummaryUseCase,
    int month, 
    int year
) : getMonthlySummaryUseCase(getMonthlySummaryUseCase), month(month), year(year) {}

std::string GetMonthlySummaryCommand::execute() {
    getMonthlySummaryUseCase.execute(month, year);
    return "MonthlySummary added successfully";
}