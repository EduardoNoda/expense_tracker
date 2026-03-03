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

    // 2. Cálculo do valor da parcela
    long long totalCents = money.getCents();
    long long installmentCents = totalCents / installments;
    Money installmentMoney(installmentCents);
    int firstImpactMonth = date.getMonth();

    int lastId = 0;

    expenseRepository.beginTransaction();

    // 3. Loop de Parcelas
    for (int i = 0; i < installments; i++) {
        // Data base da parcela 'i'
        Date currentInstallmentDate = addMonths(date, i);
        Date finalImpactDate = currentInstallmentDate;

        // 4. REGRA DEFINITIVA DE CARTÃO DE CRÉDITO
        if (paymentMethod.isCredit()) {
            int closingDay = paymentMethod.getClosingDay();
            int dueDay = paymentMethod.getDueDay();
            int monthOffset = 0;

            // Regra A: Comprou no dia do fechamento ou depois? A fatura pulou.
            if (currentInstallmentDate.getDay() >= closingDay) {
                monthOffset += 1;
            }

            // Regra B: O vencimento é antes do fechamento? (Ex: Fecha 25, Vence 05)
            // Significa que o vencimento naturalmente cai no mês calendário seguinte.
            if (dueDay < closingDay) {
                monthOffset += 1;
            }

            // Aplica os meses de "pulo" e seta o dia do vencimento exato
            Date targetMonthDate = addMonths(currentInstallmentDate, monthOffset);
            finalImpactDate = Date(dueDay, targetMonthDate.getMonth(), targetMonthDate.getYear());
        }
        if (i == 0) {
            firstImpactMonth = finalImpactDate.getMonth();
        }

        Expense expense(revenueId, installmentMoney, currentInstallmentDate, finalImpactDate, categoryId, paymentMethodId);
        lastId = expenseRepository.save(revenueId, expense);

        __android_log_print(ANDROID_LOG_INFO, "CORE_LOGIC",
                            "Parcela %d/%d | Compra: %s | Vencimento (Impacto): %s",
                            i+1, installments, currentInstallmentDate.toISO().c_str(), finalImpactDate.toISO().c_str());
    }
    expenseRepository.commitTransaction();

    return firstImpactMonth;
}
