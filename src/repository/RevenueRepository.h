#pragma once

#include "../domain/Revenue.h"
#include <vector>

class RevenueRepository {
public:
    virtual ~RevenueRepository() = default;

    virtual int save(const Revenue& revenue) = 0;
    virtual std::vector<Revenue> findByMonth(int month, int year) = 0;
    virtual Revenue findById(int revenueId) = 0;
};