#ifndef __NORMALPREDICTOR_HPP__
#define __NORMALPREDICTOR_HPP__

#include "predictor.hpp"

template <typename addr_t>
class NormalPredictor : public Predictor<addr_t> {
public:
    addr_t predict(addr_t pc) override;
    void update(addr_t pc, bool success) override;
};

template <typename addr_t>
addr_t NormalPredictor<addr_t>::predict(addr_t pc) {
    return pc + 4;
}

template <typename addr_t>
void NormalPredictor<addr_t>::update(addr_t pc, bool success) {
}

#endif