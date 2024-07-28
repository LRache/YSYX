#include <NDL.h>
#include <SDL.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>

static int pause_on = 0;
static uint8_t *buffer = NULL;
static void (*callback)(void *userdata, uint8_t *stream, int len) = NULL;
static uint16_t lengthWanted = 0;

static int is_CallbackHelper_reenter = 0;

void CallbackHelper() {
  if (is_CallbackHelper_reenter) return;
  is_CallbackHelper_reenter = 1;
  if (NDL_QueryAudio() >= lengthWanted) {
    if (callback != NULL) {
      callback(NULL, buffer, lengthWanted);
      NDL_PlayAudio(buffer, lengthWanted);
    }
  }
  is_CallbackHelper_reenter = 0;
}

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained) {
  NDL_OpenAudio(desired->freq, desired->channels, desired->samples);
  callback = desired->callback;
  lengthWanted = desired->samples * 2 * desired->channels;
  buffer = malloc(lengthWanted);
  return 0;
}

void SDL_CloseAudio() {
  lengthWanted = 0;
  free(buffer);
}

void SDL_PauseAudio(int pause_on) {
}

void SDL_MixAudio(uint8_t *dst, uint8_t *src, uint32_t len, int volume) {
  int16_t *_dst = (int16_t *)dst;
  int16_t *_src = (int16_t *)src;
  int32_t _len = len / 2;
  for (int i = 0; i < _len; i++) {
    int32_t v = (int32_t) *_src + *_dst * volume / SDL_MIX_MAXVOLUME;
    if (v < -32768) v = -32768;
    else if (v > 32767) v = 32767;
    *_dst = (int16_t)v;
    _src++;
    _dst++;
  }
}

#define Read(b, s) r = read(fd, &b, s); if (r != s) return NULL;
#define Seek(s) r = lseek(fd, s, SEEK_SET); if (r != s) return NULL;

SDL_AudioSpec *SDL_LoadWAV(const char *file, SDL_AudioSpec *spec, uint8_t **audio_buf, uint32_t *audio_len) {
  int fd = open(file, O_RDONLY);
  int r;
  uint32_t chunckID;
  Read(chunckID, 4);
  if (chunckID != 0x46464952) {
    return NULL;
  }
  uint16_t channels, freq, samples;
  Seek(22);
  Read(channels, 2);
  Read(freq, 2);
  Seek(34);
  Read(samples, 2);
  
  uint32_t dataLength;
  Seek(40);
  Read(dataLength, 4);
  
  uint8_t *buffer = malloc(dataLength);
  r = read(fd, buffer, dataLength);
  if (r != dataLength) {
    free(buffer);
    return NULL;
  }

  *audio_buf = buffer;
  *audio_len = dataLength;
  spec->freq = freq;
  spec->samples = samples;
  spec->channels = channels;
  return spec;
}

void SDL_FreeWAV(uint8_t *audio_buf) {
  free(audio_buf);
}

void SDL_LockAudio() {
}

void SDL_UnlockAudio() {
}
