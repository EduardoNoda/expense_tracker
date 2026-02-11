#include "AddRevenueCommand.h"

AddRevenueCommand::AddRevenueCommand(
    AddRevenueUseCase& uc,
    long long amountCents,
    const Date& date
)
    : useCase(uc), amountCents(amountCents), date(date) {}

std::string AddRevenueCommand::execute() {
    useCase.execute(Money(amountCents), date);
    return "Revenue added successfully";
}
