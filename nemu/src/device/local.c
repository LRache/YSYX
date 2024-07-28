#include <device/map.h>
#include <unistd.h>
#include <fcntl.h>

#define LOCAL_FD int local_fd = files[ctl[reg_fd]]; if (local_fd == -1) {ctl[reg_arg] = -1; return;}

#define LOCAL_BUF_SIZE 1024

static uint32_t *ctl;
static char *arg;
static char *buf;
static uint32_t ctl_space_size;

/*
int open(
    char *filepath  in buf_space
    int mode        in reg_arg
)
    ret fd in reg_fd

int close(
    int fd          in reg_fd
)
    ret status in reg_arg

int read(
    int fd          in reg_fd
    int length      in reg_arg
)
    ret count in reg_arg

int write(
    int fd          in reg_fd
    int length      in reg_arg
    char *buf       in buf_space
)
    ret count in reg_arg
long seek(
    int fd          in reg_fd
    int whence      in reg_arg
    long offset     in buf_space
)
    ret offset in buf_space
 */

enum {
    reg_op,
    reg_fd,
    reg_arg,
    reg_excute,
    nr_reg
};

enum {
    LOCAL_OPEN,
    LOCAL_CLOSE,
    LOCAL_READ,
    LOCAL_WRITE,
    LOCAL_SEEK,
};

static int files[32];

static void local_open() {
    int fd = 0;
    for (; fd < 32; fd++) {
        if (files[fd] == -1) break;
    }
    if (fd == 32) {
        ctl[reg_fd] = -1;
        return;
    }

    int local_fd = open(arg, ctl[reg_arg]);
    if (local_fd == -1) {
        ctl[reg_fd] = -1;
    } else {
        files[fd] = local_fd;
        ctl[reg_fd] = fd;
    }
}

static void local_close() {
    uint32_t fd = ctl[reg_fd];
    if (fd >= 32) {
        ctl[reg_arg] = -1;
        return ;
    }
    close(files[ctl[reg_fd]]);
}

static void local_read() {
    uint32_t fd = ctl[reg_fd];
    if (fd >= 32) {
        ctl[reg_arg] = 0;
        return ;
    }
    LOCAL_FD;
    size_t nbytes = ctl[reg_arg] < LOCAL_BUF_SIZE ? ctl[reg_arg] : LOCAL_BUF_SIZE;
    ctl[reg_arg] = read(local_fd, buf, nbytes);
}

static void local_write() {
    uint32_t fd = ctl[reg_fd];
    if (fd >= 32) {
        ctl[reg_arg] = 0;
        return ;
    }
    LOCAL_FD;
    size_t nbytes = ctl[reg_arg] < LOCAL_BUF_SIZE ? ctl[reg_arg] : LOCAL_BUF_SIZE;
    ctl[reg_arg] = write(local_fd, buf, nbytes);
}

static void local_seek() {
    uint32_t fd = ctl[reg_fd];
    if (fd >= 32) {
        *(int64_t *)buf = -1;
        return ;
    }
    LOCAL_FD;
    int whence = ctl[reg_arg];
    int64_t offset = *(int64_t *)buf;
    int64_t ret = lseek(local_fd, offset, whence);
    *(int64_t *)buf = ret;
}

static void local_ctl_handler(uint32_t offset, int len, bool is_write) {
    if (!is_write) return;
    if (offset % 4) return;
    int reg = offset / 4;
    if (reg == reg_excute) {
        ctl[reg_excute] = 0;
        uint32_t op = ctl[reg_op];
        switch (op)
        {
        case LOCAL_OPEN:    local_open();   break;
        case LOCAL_CLOSE:   local_close();  break;
        case LOCAL_READ:    local_read();   break;
        case LOCAL_WRITE:   local_write();  break;
        case LOCAL_SEEK:    local_seek();   break;
        default:
            break;
        }
    }
}

static void local_arg_handler(uint32_t offset, int len, bool is_write) {

}

static void local_buf_handler(uint32_t offset, int len, bool is_write) {

}

void init_local() {
    ctl_space_size = sizeof(uint32_t) * nr_reg;
    ctl = (uint32_t*)new_space(ctl_space_size);
    arg = (char *)new_space(48);
    buf = (char *)new_space(LOCAL_BUF_SIZE);

    add_mmio_map("local disk ctl", CONFIG_LOCAL_CTL_MMIO, ctl, ctl_space_size, local_ctl_handler);
    add_mmio_map("local disk arg", CONFIG_LOCAL_ARG_MMIO, arg, 48, local_arg_handler);
    add_mmio_map("local disk buf", CONFIG_LOCAL_BUF_MMIO, buf, LOCAL_BUF_SIZE, local_buf_handler);

    for (int i = 0; i < 32; i++) {
        files[i] = -1;
    }
    files[0] = 0;
    files[1] = 1;
    files[2] = 2;
}
