#ifndef __TRACER_H__
#define __TRACER_H__

#include <stdint.h>

typedef uint32_t word_t;

class Tracer {
public:
    enum Type{READ, WRITE};
    
    virtual void iter_init();
    virtual word_t iter_next(Type *t);
    virtual bool iter_is_end();
};

#endif
