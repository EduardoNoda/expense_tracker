#include "AddRevenueUseCase.h"

#include <android/log.h>
#define LOG_TAG "EXPENSE_CORE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


AddRevenueUseCase::AddRevenueUseCase(RevenueRepository& revenueRepository) 
    : revenueRepository(revenueRepository){}

int AddRevenueUseCase::execute(const Money& money, const Date& date) const{
    Revenue revenue(money, date);

    LOGI("AddRevenue is calling\n");

    return revenueRepository.save(revenue);
}