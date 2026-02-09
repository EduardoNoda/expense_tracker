#pragma once

#include "ArgParser.h"
#include "Command.h"
#include <memory>
#include "../../../../ExpenseTracker/src/infrastructure/Database.h"
#include "../../../../ExpenseTracker/src/repository/SQLiteRevenueRepository.h"
#include "../../../../ExpenseTracker/src/application/AddRevenueUseCase.h"

class CommandFactory {
public:
    CommandFactory();
    std::unique_ptr<Command> create(const ParsedCommand& parsed);
private:
    Database db;
    SQLiteRevenueRepository revenueRepo;
    AddRevenueUseCase addRevenueUC;
};
