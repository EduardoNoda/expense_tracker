#pragma once

#include <string>

class Category {
public:
    explicit Category(std::string);
    const std::string& getName() const;
private:
    int id;
    std::string name;
};