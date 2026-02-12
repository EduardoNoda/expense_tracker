cmake_minimum_required(VERSION 3.20)

project(expense_core)

add_library(
        native-lib
        SHARED
        bridge/core_bridge.cpp
        sqlite3/sqlite3.c

        core/domain/Date.cpp
        core/domain/Money.cpp
        core/domain/Expense.cpp
        core/domain/Revenue.cpp
        core/domain/Month.cpp
        core/domain/PaymentMethod.cpp

        core/repository/SQLiteRevenueRepository.cpp
        core/repository/SQLiteExpenseRepository.cpp
        core/repository/SQLitePaymentMethodRepository.cpp

        core/infrastructure/Database.cpp
        core/infrastructure/Schema.cpp

        core/application/AddExpenseUseCase.cpp
        core/application/AddRevenueUseCase.cpp
        core/application/GetMonthlySummaryUseCase.cpp
)

target_include_directories(
        native-lib
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/core
        ${CMAKE_CURRENT_SOURCE_DIR}/sqlite3
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native-lib
    ${log-lib}
)
