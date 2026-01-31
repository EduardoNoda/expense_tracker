#include "DefaultCategories.h"
#include "Category.h"
#include <vector>

std::vector<Category> DefaultCategories::defaultCategories(){
    return {
        Category("Despesa Variável Essencial"),
        Category("Despesa Fixa Essencial"),
        Category("Despesa Variável Não Essencial"),
        Category("Despesa Discricionária"),
        Category("Investimeto")
    };
}