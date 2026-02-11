#pragma once

#include "PaymentMethodRepository.h"
#include "../infrastructure/Database.h"

class SQLitePaymentMethodRepository : public PaymentMethodRepository {
public:
    explicit SQLitePaymentMethodRepository(Database& db);

    PaymentMethod findById(int id) override;
private:
    Database& database;
};