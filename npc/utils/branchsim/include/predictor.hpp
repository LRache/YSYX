#ifndef __PERDICTOR_HPP__
#define __PERDICTOR_HPP__

template <typename addr_t>
class Predictor {
public:
    virtual addr_t predict(addr_t pc) = 0;
    virtual void update(addr_t pc, bool success) = 0;
};

#endif
