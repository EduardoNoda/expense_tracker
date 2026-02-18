#pragma once

#include "PaymentMethodRepository.h"
#include "../infrastructure/Database.h"

class SQLitePaymentMethodRepository : public PaymentMethodRepository {
public:
    explicit SQLitePaymentMethodRepository(Database& db);

    PaymentMethod findById(int id) override;

    void save(const std::string& name, const std::string& type, int closingDay, int dueDay);
private:
    Database& database;
};