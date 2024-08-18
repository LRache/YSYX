#include <am.h>
#include <klib.h>

#define PS2_BASE 0x10011000
#define PS2_REG  (*(volatile uint8_t *)(PS2_BASE + 0))

static inline uint8_t scan() {
  uint8_t key = PS2_REG;
  while (key == 0) key = PS2_REG;
  return key;
}

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  volatile uint8_t key = PS2_REG;
  kbd->keycode = AM_KEY_NONE;
  kbd->keydown = false;
  if (key == 0) return;

  int keycode = AM_KEY_NONE;
  bool keydown = true;
  bool flag = false;

  if (key == 0xe1) {
    uint8_t keys[] = {0x14, 0x77, 0xe1, 0xf0, 0x14, 0xf0, 0x77};
    for (int i = 0; i < 7; i++) {
      key = scan();
      if (key != keys[i]) return;
    }
    kbd->keycode = AM_KEY_PAUSE;
    kbd->keydown = true;
    return;
  }
  
  if (key == 0xe0) {
    key = scan();
    flag = true;
  }

  if (key == 0xf0) {
    key = scan();
    keydown = false;
  }
  if (flag) {
    if (key == 0x12) {
      uint8_t keys[] = {0xe0, 0x7c};
      for (int i = 0; i < 2; i++) {
        key = scan();
        if (key != keys[i]) return;
      }
      kbd->keycode = AM_KEY_PRTSC;
      return;
    }

    switch (key)
    {
      case 0x14: keycode = AM_KEY_RCTRL; break;
      case 0x11: keycode = AM_KEY_RALT;  break;

      case 0x70: keycode = AM_KEY_INSERT; break;
      case 0x6c: keycode = AM_KEY_HOME;   break;
      case 0x7d: keycode = AM_KEY_PAGEUP; break;
      case 0x71: keycode = AM_KEY_DELETE; break;
      case 0x69: keycode = AM_KEY_END;    break;
      case 0x7a: keycode = AM_KEY_PAGEDOWN; break;

      case 0x75: keycode = AM_KEY_UP;     break;
      case 0x6b: keycode = AM_KEY_LEFT;   break;
      case 0x72: keycode = AM_KEY_DOWN;   break;
      case 0x74: keycode = AM_KEY_RIGHT;  break;
    }
  } else {
    switch (key) {
      case 0x45: keycode = AM_KEY_0; break;
      case 0x16: keycode = AM_KEY_1; break;
      case 0x1e: keycode = AM_KEY_2; break;
      case 0x26: keycode = AM_KEY_3; break;
      case 0x25: keycode = AM_KEY_4; break;
      case 0x2e: keycode = AM_KEY_5; break;
      case 0x36: keycode = AM_KEY_6; break;
      case 0x3d: keycode = AM_KEY_7; break;
      case 0x3e: keycode = AM_KEY_8; break;
      case 0x46: keycode = AM_KEY_9; break;
      
      case 0x0e: keycode = AM_KEY_GRAVE;        break;
      case 0x4e: keycode = AM_KEY_MINUS;        break;
      case 0x55: keycode = AM_KEY_EQUALS;       break;
      case 0x66: keycode = AM_KEY_BACKSPACE;    break;
      case 0x54: keycode = AM_KEY_LEFTBRACKET;  break;
      case 0x5b: keycode = AM_KEY_RIGHTBRACKET; break;
      case 0x5d: keycode = AM_KEY_BACKSLASH;    break;
      case 0x4c: keycode = AM_KEY_SEMICOLON;    break;
      case 0x52: keycode = AM_KEY_APOSTROPHE;   break;
      case 0x5a: keycode = AM_KEY_RETURN;       break;
      case 0x41: keycode = AM_KEY_COMMA;        break;
      case 0x49: keycode = AM_KEY_PERIOD;       break;
      case 0x4a: keycode = AM_KEY_SLASH;        break;
      case 0x0d: keycode = AM_KEY_TAB;          break;
      case 0x58: keycode = AM_KEY_CAPSLOCK;     break;
      case 0x12: keycode = AM_KEY_LSHIFT;       break;
      case 0x59: keycode = AM_KEY_RSHIFT;       break;
      case 0x14: keycode = AM_KEY_LCTRL;        break;
      case 0x11: keycode = AM_KEY_LALT;         break;
      case 0x29: keycode = AM_KEY_SPACE;        break;
      case 0x7e: keycode = AM_KEY_SCLK;         break;

      case 0x1c: keycode = AM_KEY_A; break;
      case 0x32: keycode = AM_KEY_B; break;
      case 0x21: keycode = AM_KEY_C; break;
      case 0x23: keycode = AM_KEY_D; break;
      case 0x24: keycode = AM_KEY_E; break;
      case 0x2b: keycode = AM_KEY_F; break;
      case 0x34: keycode = AM_KEY_G; break;
      case 0x33: keycode = AM_KEY_H; break;
      case 0x43: keycode = AM_KEY_I; break;
      case 0x3b: keycode = AM_KEY_J; break;
      case 0x42: keycode = AM_KEY_K; break;
      case 0x4b: keycode = AM_KEY_L; break;
      case 0x3a: keycode = AM_KEY_M; break;
      case 0x31: keycode = AM_KEY_N; break;
      case 0x44: keycode = AM_KEY_O; break;
      case 0x4d: keycode = AM_KEY_P; break;
      case 0x15: keycode = AM_KEY_Q; break;
      case 0x2d: keycode = AM_KEY_R; break;
      case 0x1b: keycode = AM_KEY_S; break;
      case 0x2c: keycode = AM_KEY_T; break;
      case 0x3c: keycode = AM_KEY_U; break;
      case 0x2a: keycode = AM_KEY_V; break;
      case 0x1d: keycode = AM_KEY_W; break;
      case 0x22: keycode = AM_KEY_X; break;
      case 0x35: keycode = AM_KEY_Y; break;
      case 0x1a: keycode = AM_KEY_Z; break;

      case 0x76: keycode = AM_KEY_ESCAPE; break;
      case 0x05: keycode = AM_KEY_F1;     break;
      case 0x06: keycode = AM_KEY_F2;     break;
      case 0x04: keycode = AM_KEY_F3;     break;
      case 0x0c: keycode = AM_KEY_F4;     break;
      case 0x03: keycode = AM_KEY_F5;     break;
      case 0x0b: keycode = AM_KEY_F6;     break;
      case 0x83: keycode = AM_KEY_F7;     break;
      case 0x0a: keycode = AM_KEY_F8;     break;
      case 0x01: keycode = AM_KEY_F9;     break;
      case 0x09: keycode = AM_KEY_F10;    break;
      case 0x78: keycode = AM_KEY_F11;    break;
      case 0x07: keycode = AM_KEY_F12;    break;
    }
  }
  
  kbd->keycode = keycode;
  kbd->keydown = keydown;
}
