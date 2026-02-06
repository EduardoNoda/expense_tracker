add_library(expense_core
    domain/Date.cpp
    domain/Money.cpp
    domain/Expense.cpp
    domain/Revenue.cpp
    domain/Month.cpp

    repository/Database.cpp
    repository/SQLiteRevenueRepository.cpp
    repository/SQLiteExpenseRepository.cpp

    application/AddExpenseUseCase.cpp
    application/AddRevenueUseCase.cpp
    application/GetMonthlySummaryUseCase.cpp
)

target_include_directories(expense_core
    PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}
)

add_executable(expense_tracker
    main.cpp
)

target_link_libraries(expense_tracker
    PRIVATE expense_core
)
