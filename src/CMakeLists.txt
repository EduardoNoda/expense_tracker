add_library(expense_domain
    Date.cpp
    MoneyManager.cpp
    DefaulCategories.cpp
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
