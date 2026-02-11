#include "AddRevenueUseCase.h"

AddRevenueUseCase::AddRevenueUseCase(RevenueRepository& revenueRepository) 
    : revenueRepository(revenueRepository){}

int AddRevenueUseCase::execute(const Money& money, const Date& date) const{
    Revenue revenue(money, date);

    return revenueRepository.save(revenue);
}