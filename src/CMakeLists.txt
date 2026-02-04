cmake_minimum_required(VERSION 3.20)

add_library(expense_domain
    domain/Date.cpp
    domain/Money.cpp
    domain/Expense.cpp
    domain/Revenue.cpp
    domain/Month.cpp
)

target_include_directories(expense_domain
    PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}
)

add_executable(expense_tracker
    main.cpp
)

target_link_libraries(expense_tracker
    PRIVATE expense_domain
)
