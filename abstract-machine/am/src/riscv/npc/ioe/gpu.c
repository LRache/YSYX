#include <am.h>

#include "klib.h"

#define VGA_WIDTH 640
#define VGA_HEIGHT 480

#define VGA_BASE 0x21000000
#define VGA_FB (volatile uint32_t *)(VGA_BASE + 0)

static AM_GPU_CONFIG_T gpuConfig;

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  int width = VGA_WIDTH;
  int height = VGA_HEIGHT;
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = width, .height = height,
    .vmemsz = 0
  };
}

void __am_gpu_init() {
  __am_gpu_config(&gpuConfig);
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int width = gpuConfig.width; 
  volatile uint32_t *fb = VGA_FB;
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
