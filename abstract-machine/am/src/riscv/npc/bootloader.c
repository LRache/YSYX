#include <stdint.h>

static inline void __cpy(char *__dst, char *__src, uint32_t size) {
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

__attribute__((section(".bootloader"))) void _bootloader() {
    __cpy(  _text_start,   _text_load_start, (uint32_t)  _text_size);
    __cpy(_rodata_start, _rodata_load_start, (uint32_t)_rodata_size);
    __cpy(  _data_start,   _data_load_start, (uint32_t)  _data_size);
    _trm_init();
}

__attribute__((section(".entry"))) void _entry_bootloader() {
    __cpy(_bootloader_start, _bootloader_load_start, (uint32_t)_bootloader_size);
    _bootloader();
}
