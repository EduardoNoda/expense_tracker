add_library(expense_domain
    domain/Date.cpp
    domain/MoneyManager.cpp
)

target_include_directories(expense_domain
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)

add_executable(expense_tracker
    main.cpp
)

target_link_libraries(expense_tracker
    PRIVATE
    expense_domain
)
