#pragma once

#include "../repository/RevenueRepository.h"

class AddRevenueUseCase {
public:
    explicit AddRevenueUseCase(RevenueRepository& revenueRepository);

    int execute(const Money& money, const Date& date) const;
private:
    RevenueRepository& revenueRepository;
};