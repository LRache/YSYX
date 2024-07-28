#include <NDL.h>
#include <sdl-timer.h>
#include <stdio.h>

extern void CallbackHelper();

SDL_TimerID SDL_AddTimer(uint32_t interval, SDL_NewTimerCallback callback, void *param) {
  return NULL;
}

int SDL_RemoveTimer(SDL_TimerID id) {
  return 1;
}

extern uint32_t SDL_startTicks;
uint32_t SDL_GetTicks() {
  CallbackHelper();
  return (NDL_GetTicks() / 1000 - SDL_startTicks);
}

void SDL_Delay(uint32_t ms) {
  CallbackHelper();
  uint32_t start = SDL_GetTicks();
  while (SDL_GetTicks() - start < ms);
}
