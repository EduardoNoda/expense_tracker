#include "Revenue.h"
#include "Money.h"

Revenue::Revenue(
    Money amount, 
    Date date) : 
    amount(amount), 
    date(date){}

const Money& Revenue::getAmount() const {return amount;}
const Date& Revenue::getDate() const {return date;}