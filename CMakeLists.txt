cmake_minimum_required(VERSION 3.20)

project(expense_tracker
	LANGUAGES CXX
	VERSION 0.1
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_executable(expense_tracker
	src/main.cpp
	src/domain/Purchase.cpp
)

target_include_directories(expense_tracker
	PRIVATE
	src
)
