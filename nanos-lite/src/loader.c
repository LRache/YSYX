#include <proc.h>
#include <elf.h>
#include <fs.h>

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
# define Elf_Shdr Elf64_Shdr
# define Elf_Sym  Elf64_Sym
# define Elf_Addr Elf64_Addr
# define Elf_Word int64_t
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
# define Elf_Shdr Elf32_Shdr
# define Elf_Sym  Elf32_Sym
# define Elf_Addr Elf32_Addr
# define Elf_Word int32_t
#endif

#if defined(__ISA_AM_NATIVE__)
# define EXPECT_ISA EM_X86_64
#elif defined(__ISA_X86__)
# define EXPECT_ISA EM_386
#elif defined(__ISA_RISCV32__)
# define EXPECT_ISA EM_RISCV
#endif

static const char ELF_MAGIC_NUMBER[] = {0x7f, 'E', 'L', 'F'};

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t get_ramdisk_size();

uintptr_t loader(PCB *pcb, const char *filename, AddrSpace *as) {
  int fd = fs_open(filename, 0, 0);
  assert(fd != -1);
  
  size_t r;
  Elf_Ehdr elfHeader;
  r = fs_read(fd, &elfHeader, sizeof(elfHeader));
  assert(r == sizeof(elfHeader));
  assert(memcmp(elfHeader.e_ident, ELF_MAGIC_NUMBER, 4) == 0);
  assert(elfHeader.e_ident[EI_CLASS] == ELFCLASS32);
  assert(elfHeader.e_machine == EXPECT_ISA);

  Elf_Phdr phdrArray[elfHeader.e_phnum];
  fs_lseek(fd, elfHeader.e_phoff, SEEK_SET);
  r = fs_read(fd, phdrArray, sizeof(phdrArray));
  assert(r == sizeof(phdrArray));

  for (int i = 0; i < elfHeader.e_phnum; i++) {
    Elf_Phdr phdr = phdrArray[i];
    if (phdr.p_type == PT_LOAD) {
      fs_lseek(fd, phdr.p_offset, SEEK_SET);
      Elf_Addr vaddr = phdr.p_vaddr;
      Elf_Word filesz = phdr.p_filesz;
      Elf_Word memsz = phdr.p_memsz;
      
      void *paddr;
      if (vaddr % PGSIZE != 0) {
        paddr = pg_alloc(PGSIZE);
        map(as, (void *)(vaddr - vaddr % PGSIZE), paddr, 1);
        
        uint32_t size = PGSIZE - (vaddr % PGSIZE);
        size = filesz < size ? filesz : size;
        r = fs_read(fd, paddr + vaddr % PGSIZE, size);
        assert(r == size);
        filesz -= size;
        memsz -= size;
        vaddr += PGSIZE - vaddr % PGSIZE;
      }
      while (filesz > PGSIZE) {
        paddr = pg_alloc(PGSIZE);
        map(as, (void *)vaddr, paddr, 1);
        r = fs_read(fd, paddr, PGSIZE);
        assert(r == PGSIZE);
        filesz -= PGSIZE;
        memsz -= PGSIZE;
        vaddr += PGSIZE;
      }
      if (filesz != 0) {
        paddr = pg_alloc(PGSIZE);
        map(as, (void *)vaddr, paddr, 1);
        r = fs_read(fd, paddr, filesz);
        assert(r == filesz);
        memsz -= filesz;
        vaddr += filesz;
      }
      memsz -= PGSIZE - vaddr % PGSIZE;
      vaddr += PGSIZE - vaddr % PGSIZE;
      while (memsz >= 0)
      {
        paddr = pg_alloc(PGSIZE);
        map(as, (void *)vaddr, paddr, 1);
        memsz -= PGSIZE;
        vaddr += PGSIZE;
      }
    }
  }

  fs_close(fd);
  return elfHeader.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename, NULL);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}
