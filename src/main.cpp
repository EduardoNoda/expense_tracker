#include <iostream>
#include "domain/Purchase.h"

int main() {
    auto purchase = Purchase::createSummary(
        "Supermercado X",
        123.45
    );

    std::cout << purchase.location() << " - "
              << purchase.total() << "\n";

    return 0;
}
