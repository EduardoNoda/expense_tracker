#include "CommandFactory.h"

#include "AddRevenueCommand.h"

#include "../../repository/SQLiteRevenueRepository.h"
#include "../../infrastructure/Database.h"
#include "../../infrastructure/Schema.h"

#include <stdexcept>

CommandFactory::CommandFactory()
    : db("expense.db"),
      revenueRepo(db),
      addRevenueUC(revenueRepo)
{
    Schema schema(db);
    schema.ensure();
}

std::unique_ptr<Command> CommandFactory::create(const ParsedCommand& parsed) {
    if (parsed.name == "add-revenue") {
        if (parsed.args.size() != 2) {
            throw std::runtime_error("Usage: add-revenue <amount_cents> <YYYY-MM-DD>");
        }

        long long cents = std::stoll(parsed.args[0]);
        Date date = Date::fromISO(parsed.args[1]);

        return std::make_unique<AddRevenueCommand>(
            addRevenueUC,
            cents,
            date
        );
    }

    throw std::runtime_error("Unknown command: " + parsed.name);
}

