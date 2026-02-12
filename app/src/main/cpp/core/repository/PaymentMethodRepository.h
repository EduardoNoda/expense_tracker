#pragma once

#include "../domain/PaymentMethod.h"

class PaymentMethodRepository {
public:
    virtual ~PaymentMethodRepository() = default;

    virtual PaymentMethod findById(int id) = 0;
};