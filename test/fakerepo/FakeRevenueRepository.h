#pragma once

#include <vector>

#include "repository/RevenueRepository.h"

class FakeRevenueRepository : public RevenueRepository {
public:
    int save(const Revenue& revenue) override {
        Revenue copy = revenue;
        copy.setId(++counter);
        revenues.push_back(copy);
        return copy.getId();
    }

    std::vector<Revenue> findByMonth(int month, int year) override {
        std::vector<Revenue> result;
        for (auto& r : revenues) {
            if (r.getDate().getMonth() == month &&
                r.getDate().getYear() == year) {
                result.push_back(r);
            }
        }
        return result;
    }

    Revenue findById(int id) override {
        for (auto& r : revenues) {
            if (r.getId() == id) return r;
        }
        throw std::runtime_error("Revenue not found");
    }

private:
    int counter = 0;
    std::vector<Revenue> revenues;
};
