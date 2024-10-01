#ifndef __CTRACER_H__
#define __CTRACER_H__

#include "itracer.hpp"

template <typename addr_t>
using ICTracerWriter = ITracerWriter<addr_t>;
using ICTracerReader = ITracerReader<addr_t>;

#endif