#pragma once
#include <string>

class Command {
public:
    virtual ~Command() = default;
    virtual std::string execute() = 0;
};
