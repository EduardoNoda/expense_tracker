#pragma once

#include "../domain/PaymentMethod.h"

class PaymentMethodRepository {
public:
    virtual ~PaymentMethodRepository() = default;

    virtual PaymentMethod findById(int id) = 0;
    virtual void save(const std::string& name, const std::string& type, int closingDay, int dueDay) = 0;
};