cmake_minimum_required(VERSION 3.20)
project(ExpenseTracker LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

configure_file(
    ${CMAKE_SOURCE_DIR}/src/infrastructure/schema.sql
    ${CMAKE_BINARY_DIR}/schema.sql
    COPYONLY
)

add_subdirectory(src)
add_subdirectory(test)
