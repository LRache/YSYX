#include <am.h>
#include <nemu.h>

#include "klib.h"

#define SYNC_ADDR (VGACTL_ADDR + 4)

static AM_GPU_CONFIG_T gpuConfig;

void __am_gpu_config(AM_GPU_CONFIG_T *cfg);

void __am_gpu_init() {
  __am_gpu_config(&gpuConfig);
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  uint32_t screenSize = inl(VGACTL_ADDR);
  int width = screenSize >> 16;
  int height = screenSize & 0x7fff;
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = width, .height = height,
    .vmemsz = 0
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
  }
  int width = gpuConfig.width; 
  uint32_t *fb = (uint32_t *)(uintptr_t)FB_ADDR;
  uint32_t *pixels = (uint32_t *)ctl->pixels;
  for (int y = 0; y < ctl->h; y ++) {
    for (int x = 0; x < ctl->w; x++) {
      *(fb+width*(ctl->y+y)+(ctl->x+x)) = *(pixels+y*ctl->w+x);
    }
  }
}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
