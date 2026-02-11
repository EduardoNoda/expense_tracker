#include <iostream>

#include "interface/cli/ArgParser.h"
#include "interface/cli/CommandFactory.h"

int main(int argc, char* argv[]) {
    try {
        ArgParser parser;
        ParsedCommand parsed = parser.parse(argc, argv);

        CommandFactory factory;
        auto command = factory.create(parsed);

        std::string result = command->execute();
        std::cout << result << "\n";

        return 0;
    }
    catch (const std::exception& ex) {
        std::cerr << "Error: " << ex.what() << "\n";
        return 1;
    }
}
