#include "AddExpenseUseCase.h"
#include <stdexcept>
#include <android/log.h>

AddExpenseUseCase::AddExpenseUseCase (
    ExpenseRepository& expenseRepository,
    RevenueRepository& revenueRepository,
    PaymentMethodRepository& paymentMethodRepository)
    : expenseRepository(expenseRepository), revenueRepository(revenueRepository), paymentMethodRepository(paymentMethodRepository){}

Date addMonths(const Date& date, int monthsToAdd) {
    int d = date.getDay();
    int m = date.getMonth();
    int y = date.getYear();

    int totalMonths = m + monthsToAdd;

    // Matemática de ano: (totalMonths - 1) / 12 dá quantos anos avançou
    int yearsToAdd = (totalMonths - 1) / 12;
    y += yearsToAdd;

    // Matemática de mês: O resto da divisão ajustado para 1-12
    m = (totalMonths - 1) % 12 + 1;

    return Date(d, m, y);
}

int AddExpenseUseCase::execute(int revenueId, Money money, Date date, Date initialImpactDate,
                               int categoryId, int paymentMethodId, int installments) {

    // 1. Validação básica
    if (installments < 1) installments = 1;
    PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId);
    if (!paymentMethod.isCredit()) installments = 1;

    // 2. Cálculo do valor da parcela (sem centavos perdidos no MVP)
    long long totalCents = money.getCents();
    long long installmentCents = totalCents / installments;
    Money installmentMoney(installmentCents);

    int lastId = 0;

    expenseRepository.beginTransaction();

    // 3. Loop de Parcelas
    for (int i = 0; i < installments; i++) {
        // Data base da parcela 'i' (ex: compra hoje, parcela 2 é hoje + 1 mês)
        Date currentInstallmentDate = addMonths(date, i);

        Date finalImpactDate = currentInstallmentDate;

        // 4. Regra de Cartão de Crédito
        if (paymentMethod.isCredit()) {
            int closingDay = paymentMethod.getClosingDay();
            int dueDay = paymentMethod.getDueDay();

            // Se a compra (ou a parcela virtual) cair depois do fechamento, joga pro mês seguinte
            if (currentInstallmentDate.getDay() > closingDay) {
                // Avança 1 mês no vencimento
                finalImpactDate = addMonths(currentInstallmentDate, 1);
                // Fixa o dia no dia de vencimento
                finalImpactDate = Date(dueDay, finalImpactDate.getMonth(), finalImpactDate.getYear());
            } else {
                // Cai no mesmo mês, mas no dia do vencimento
                finalImpactDate = Date(dueDay, currentInstallmentDate.getMonth(), currentInstallmentDate.getYear());
            }
        }

        Expense expense(revenueId,installmentMoney, currentInstallmentDate, finalImpactDate, categoryId, paymentMethodId);
        lastId = expenseRepository.save(revenueId, expense);

        __android_log_print(ANDROID_LOG_INFO, "CORE_LOGIC",
                            "Parcela %d/%d | Data Base: %s | Vencimento: %s",
                            i+1, installments, currentInstallmentDate.toISO().c_str(), finalImpactDate.toISO().c_str());
    }
    expenseRepository.commitTransaction();

    return lastId;
}
