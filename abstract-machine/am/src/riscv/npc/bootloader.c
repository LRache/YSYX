#include <stdint.h>

// #define HAS_EXTRA

static inline void __memcpy(char *__dst, char *__src, uint32_t size) {
    if (__dst == __src) return;
    uint32_t *dst = (uint32_t *)__dst;
    uint32_t *src = (uint32_t *)__src;
    size /= 4;
    for (uint32_t i = 0; i < size; i++) {
        dst[i] = src[i];
    }
}

extern char _bootloader_start[];
extern char _bootloader_size[];
extern char _bootloader_load_start[];

void _trm_init();

extern char _text_start[];
extern char _text_size[];
extern char _text_load_start[];

extern char _rodata_start[];
extern char _rodata_size[];
extern char _rodata_load_start[];

extern char _data_start[];
extern char _data_size[];
extern char _data_load_start[];

#ifdef HAS_EXTRA

extern char _data_extra_start[];
extern char _data_extra_size[];
extern char _data_extra_load_start[];

#endif

__attribute__((section(".bootloader"))) void _bootloader() {
    __memcpy(  _text_start,   _text_load_start, (uint32_t)  _text_size);
    __memcpy(_rodata_start, _rodata_load_start, (uint32_t)_rodata_size);
    __memcpy(  _data_start,   _data_load_start, (uint32_t)  _data_size);
    #ifdef HAS_EXTRA
    __memcpy(_data_extra_start, _data_extra_load_start, (uint32_t)_data_extra_size);
    #endif
    _trm_init();
}

__attribute__((section(".entry"))) void _entry_bootloader() {
    __memcpy(_bootloader_start, _bootloader_load_start, (uint32_t)_bootloader_size);
    _bootloader();
}
