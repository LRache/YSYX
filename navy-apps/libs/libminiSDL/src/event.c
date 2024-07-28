#include <NDL.h>
#include <SDL.h>
#include <unistd.h>
#include <string.h>

#define keyname(k) #k,

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

extern void CallbackHelper();

int SDL_PushEvent(SDL_Event *ev) {
  return 0;
}

int SDL_PollEvent(SDL_Event *event) {
  char buffer[16];
  int r = read(3, buffer, sizeof(buffer));
  if (r == 0) return 0;

  char type[6], arg[12];
  sscanf(buffer, "%s %s", type, arg);
  if (strcmp(type, "mmap") == 0) {
    event->type = SDL_USEREVENT;
  } 
  else if (strcmp(type, "kd") == 0 || strcmp(type, "ku") == 0) {
    if (type[1] == 'u') event->type = SDL_KEYUP;
    else if (type[1] == 'd') event->type = SDL_KEYDOWN;
    for (int i = 0; i < SDLK_COUNT; i++) {
      if (strcmp(arg, keyname[i]) == 0) {
        event->key.keysym.sym = i;
        break;
      }
    }
  }
  CallbackHelper();
  return 1;
}

int SDL_WaitEvent(SDL_Event *event) {
  CallbackHelper();
  char buffer[16];
  while (read(3, buffer, sizeof(buffer)) == 0);

  char type[6], arg[12];
  sscanf(buffer, "%s %s", type, arg);
  if (strcmp(type, "mmap") == 0) {
    event->type = SDL_USEREVENT;
  } 
  else if (strcmp(type, "kd") == 0 || strcmp(type, "ku") == 0) {
    if (type[1] == 'u') event->type = SDL_KEYUP;
    else if (type[1] == 'd') event->type = SDL_KEYDOWN;
    for (int i = 0; i < SDLK_COUNT; i++) {
      if (strcmp(arg, keyname[i]) == 0) {
        event->key.keysym.sym = i;
        break;
      }
    }
  }
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  return NULL;
}
