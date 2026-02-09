add_executable(expense_tracker_tests
    test_main.cpp
)

target_link_libraries(expense_tracker_tests
    PRIVATE expense_core
)

target_include_directories(expense_tracker_tests
    PRIVATE 
        ${PROJECT_SOURCE_DIR}/src
        ${PROJECT_SOURCE_DIR}/test
)