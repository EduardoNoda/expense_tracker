cmake_minimum_required(VERSION 3.20)

project(expense_core)

add_library(
        native-lib
        SHARED

        bridge/core_bridge.cpp

        core/domain/Date.cpp
        core/domain/Money.cpp
        core/domain/Expense.cpp
        core/domain/Revenue.cpp
        core/domain/Month.cpp

        core/repository/SQLiteRevenueRepository.cpp
        core/repository/SQLiteExpenseRepository.cpp

        core/infrastructure/Database.cpp
        core/infrastructure/Schema.cpp

        core/application/AddExpenseUseCase.cpp
        core/application/AddRevenueUseCase.cpp
        core/application/GetMonthlySummaryUseCase.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native-lib
    ${log-lib}
)
