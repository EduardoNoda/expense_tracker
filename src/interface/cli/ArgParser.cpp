#include "ArgParser.h"
#include <stdexcept>

ParsedCommand ArgParser::parse(int argc, char* argv[]) const {
    if (argc < 2) {
        throw std::runtime_error("No command provided");
    }

    ParsedCommand result;
    result.name = argv[1];

    for (int i = 2; i < argc; ++i) {
        result.args.emplace_back(argv[i]);
    }

    return result;
}
