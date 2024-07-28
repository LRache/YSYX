#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/time.h>

#define FD_EVT_DEV 3
#define FD_FB_CTL  4
#define FD_FB_DEV  5
#define FD_PROC_DISPINFO 6
#define FD_SB_CTL  7
#define FD_SB_DEV  8

static int screen_w = 0, screen_h = 0;

uint32_t NDL_GetTicks() {
  struct timeval t;
  gettimeofday(&t, NULL);
  return t.tv_sec * 1000000 + t.tv_usec;
}

int NDL_PollEvent(char *buf, int len) {
  return read(3, buf, len);
}

void NDL_OpenCanvas(int *w, int *h) {
  char buf[64];
  if (*w == 0 && *h == 0) {
    read(FD_PROC_DISPINFO, buf, sizeof(buf));
    close(FD_PROC_DISPINFO);
    sscanf(buf, "%d %d", &screen_w, &screen_h);
    *w = screen_w;
    *h = screen_h;
  } else {
    screen_w = *w; screen_h = *h;
  }
  int len = sprintf(buf, "%d %d", screen_w, screen_h);
  // let NWM resize the window and create the frame buffer
  write(FD_FB_CTL, buf, len);
  while (1) {
    int nread = read(FD_EVT_DEV, buf, sizeof(buf) - 1);
    if (nread <= 0) continue;
    buf[nread] = '\0';
    if (strcmp(buf, "mmap ok") == 0) break;
  }
  close(FD_FB_CTL);
}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  if (x == 0 && y == 0 && w == 0 && h == 0) {
    write(5, pixels, screen_w * screen_h * 4);
  } else {
    for (int x_ = 0; x_ < h; x_++) {
      int offset = ((x + x_) * screen_w + y) * 4;
      lseek(5, offset, SEEK_SET);
      write(5, pixels, w*4);
      pixels += w;
    }
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {
  uint32_t buf[3] = {freq, channels, samples};
  write(FD_SB_CTL, (void *)buf, 12);
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  int r = write(FD_SB_DEV, buf, len);
  return r;
}

int NDL_QueryAudio() {
  uint32_t r;
  read(FD_SB_CTL, (void *)&r, 4);
  return r;
}

int NDL_Init(uint32_t flags) {
  return 0;
}

void NDL_Quit() {
}
