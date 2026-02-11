#pragma once
#include <string>
#include <vector>

struct ParsedCommand {
    std::string name;
    std::vector<std::string> args;
};

class ArgParser {
public:
    ParsedCommand parse(int argc, char* argv[]) const;
};
