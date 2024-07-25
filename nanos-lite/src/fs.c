#include <fs.h>
#include <am.h>
#include <amdev.h>

size_t ramdisk_read (void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
  size_t open_offset;
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_DEV_EVENT, FD_FBCTL, FD_FB, FD_PROC_DISPINFO, FD_DEV_SBCTL, FD_DEV_SB};

size_t invalid_read(void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_write(const void *buf, size_t offset, size_t len) {
  panic("should not reach here");
  return 0;
}

size_t invalid_read (      void *buf, size_t offset, size_t len);
size_t invalid_write(const void *buf, size_t offset, size_t len);
size_t serial_write (const void *buf, size_t offset, size_t len);
size_t events_read  (      void *buf, size_t offset, size_t len);
size_t fbctl_write  (const void *buf, size_t offset, size_t len);
size_t fb_write     (const void *buf, size_t offset, size_t len);
size_t dispinfo_read(      void *buf, size_t offset, size_t len);
size_t sbctl_read   (      void *buf, size_t offset, size_t len);
size_t sbctl_write  (const void *buf, size_t offset, size_t len);
size_t sb_write     (const void *buf, size_t offset, size_t len);

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
  [FD_STDIN]          = {"stdin",           0, 0, invalid_read, invalid_write },
  [FD_STDOUT]         = {"stdout",          0, 0, invalid_read, serial_write  },
  [FD_STDERR]         = {"stderr",          0, 0, invalid_read, serial_write  },
  [FD_DEV_EVENT]      = {"/dev/events",     0, 0, events_read,  invalid_write },
  [FD_FBCTL]          = {"/dev/fbctl",      0, 0, invalid_read, fbctl_write   },
  [FD_FB]             = {"/dev/fb",         0, 0, invalid_read, fb_write      },
  [FD_PROC_DISPINFO]  = {"/proc/dispinfo",  0, 0, dispinfo_read,invalid_write },
  [FD_DEV_SBCTL]      = {"/dev/sbctl",      0, 0, sbctl_read,   sbctl_write   },
  [FD_DEV_SB]         = {"/dev/sb",         0, 0, invalid_read, sb_write      },
#include "files.h"
};

#define FILE_NUM (sizeof(file_table) / sizeof(file_table[0]))

void sbctl_init();
void init_fs() {
  char s[32];
  dispinfo_read(s, 0, 32);
  int width = 0;
  int height = 0;
  int i = 0;
  for (; s[i] != ' '; i++) {
    width = width * 10 + s[i] - '0';
  }
  i++;
  for (; s[i] != '\0'; i++) {
    height = height * 10 + s[i] - '0';
  }
  file_table[FD_FB].size = width * height * 4;

  sbctl_init();
}

int fs_open(const char *pathname, int flags, int mode) {
  for (int i = 0; i < FILE_NUM; i++) {
    if (strcmp(pathname, file_table[i].name) == 0) {
      file_table[i].open_offset = 0;
      return i;
    }
  }
  int fd;
  io_write(AM_LOCAL_OPEN, (char *)pathname, mode, &fd);
  if (fd != -1) {
    return fd + 128;
  }
  return -1;
}

size_t fs_read(int fd, void *buf, size_t len) {
  if (fd >= 128) {
    fd -= 128;
    size_t n;
    io_write(AM_LOCAL_READ, fd, buf, len, &n);
    return n;
  }

  Finfo info = file_table[fd];
  if (info.read != NULL) {
    return info.read(buf, info.open_offset, len);
  }

  size_t left = info.size - info.open_offset;
  if (left == 0) return 0;
  size_t read_length = left > len ? len : left;
  ramdisk_read(buf, info.disk_offset + info.open_offset, read_length);
  file_table[fd].open_offset += read_length;
  return read_length;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  if (fd >= 128) {
    fd -= 128;
    size_t n;
    io_write(AM_LOCAL_WRITE, fd, (char *)buf, len, &n);
    return n;
  }

  Finfo info = file_table[fd];
  if (info.write != NULL) {
    return info.write(buf, info.open_offset, len);
  }
  
  size_t left = info.size - info.open_offset;
  if (left == 0) {
    return 0;
  }
  size_t write_length = left > len ? len : left;
  ramdisk_write(buf, info.disk_offset + info.open_offset, write_length);
  file_table[fd].open_offset += write_length;
  return write_length;
}

size_t fs_lseek(int fd, off_t offset, int whence) {
  if (fd >= 128) {
    fd -= 128;
    uint64_t off;
    io_write(AM_LOCAL_SEEK, fd, offset, whence, &off);
    return off;
  }

  size_t new_off = 0;
  switch (whence)
  {
  case SEEK_SET: new_off = offset; break;
  case SEEK_CUR: new_off = file_table[fd].open_offset + offset; break;
  case SEEK_END: new_off = file_table[fd].size + offset; break;
  default:
    return -1;
  }
  if (new_off >= 0 && new_off <= file_table[fd].size) {
    file_table[fd].open_offset = new_off;
    return new_off;
  } else {
    return -1;
  }
}

int fs_close(int fd) {
  return 0;
}
