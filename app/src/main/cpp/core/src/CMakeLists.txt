add_library(expense_core
    domain/Date.cpp
    domain/Money.cpp
    domain/Expense.cpp
    domain/Revenue.cpp
    domain/Month.cpp

    repository/SQLiteRevenueRepository.cpp
    repository/SQLiteExpenseRepository.cpp

    infrastructure/Database.cpp
    infrastructure/Schema.cpp

    application/AddExpenseUseCase.cpp
    application/AddRevenueUseCase.cpp
    application/GetMonthlySummaryUseCase.cpp

    interface/cli/AddRevenueCommand.cpp
)

target_include_directories(expense_core
    PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}
)

add_executable(expense_tracker
    main.cpp
    interface/cli/CommandFactory.cpp
    interface/cli/ArgParser.cpp
)

target_link_libraries(expense_tracker
    PRIVATE expense_core
)

target_include_directories(expense_tracker
    PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}
)

find_package(SQLite3 REQUIRED)

target_link_libraries(expense_core
    PUBLIC SQLite::SQLite3
)