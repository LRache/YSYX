/***************************************************************************************
* Copyright (c) 2014-2022 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <elf.h>
#include "isa.h"
#include "memory/paddr.h"
#include "tracer.h"

enum IMG_TYEP{
  IMG_IMAGE, IMG_ELF
};

int imgType = IMG_IMAGE;
static const char ELF_MAGIC_NUMBER[] = {0x7f, 'E', 'L', 'F'};

void init_rand();
void init_log(const char *log_file);
void init_mem();
void init_difftest(char *ref_so_file, long img_size, int port);
void init_device();
void init_sdb();
void init_disasm(const char *triple);

static void welcome() {
  Log("Trace: %s", MUXDEF(CONFIG_TRACE, ANSI_FMT("ON", ANSI_FG_GREEN), ANSI_FMT("OFF", ANSI_FG_RED)));
  IFDEF(CONFIG_TRACE, Log("If trace is enabled, a log file will be generated "
        "to record the trace. This may lead to a large log file. "
        "If it is not necessary, you can disable it in menuconfig"));
  Log("Build time: %s, %s", __TIME__, __DATE__);
  printf("Welcome to %s-NEMU!\n", ANSI_FMT(str(__GUEST_ISA__), ANSI_FG_YELLOW ANSI_BG_RED));
  printf("For help, type \"help\"\n");
}

#ifndef CONFIG_TARGET_AM
#include <getopt.h>

void sdb_set_batch_mode();

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static char *img_file_type = NULL;
static int difftest_port = 1234;

static long load_normal_image() {
  FILE *fp = fopen(img_file, "rb");
  Assert(fp, "Can not open '%s'", img_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The image is %s, size = %ld", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

static long load_elf() {
  Log("Loading elf file");
  FILE *fp = fopen(img_file, "rb");
  Assert(fp, "Can not open '%s'", img_file);

  Elf32_Ehdr elfHeader;
  int r = fread(&elfHeader, sizeof(elfHeader), 1, fp);
  Assert(r == 1, "Read error.");
  Assert(memcmp(ELF_MAGIC_NUMBER, elfHeader.e_ident, 4) == 0, "Bad elf head");
  Assert(elfHeader.e_ident[EI_CLASS] == ELFCLASS32, "Bad elf class");
  Assert(elfHeader.e_machine == EM_RISCV, "Bad isa");
  
  // Elf32_Phdr programHeaderArray[elfHeader.e_phnum];
  // r = fread(programHeaderArray, sizeof(Elf32_Phdr), elfHeader.e_phnum, fp);
  // Assert(r == elfHeader.e_phnum, "Read error.");
  
  fseek(fp, elfHeader.e_shoff, SEEK_SET);
  Elf32_Shdr sectionHeaderArray[elfHeader.e_shnum];
  r = fread(sectionHeaderArray, sizeof(Elf32_Shdr), elfHeader.e_shnum, fp);
  Assert(r == elfHeader.e_shnum, "Read error.");

  char *stringTable = NULL;
  for (int i = 0; i < elfHeader.e_shnum; i++) {
    Elf32_Shdr shdr = sectionHeaderArray[i];
    if (shdr.sh_type == SHT_STRTAB) {
      long offset = shdr.sh_offset;
      fseek(fp, offset, SEEK_SET);
      stringTable = malloc(shdr.sh_size);
      r = fread(stringTable, shdr.sh_size, 1, fp);
      Assert(r == 1, "Read string table error");
      break;
    }
  }
  
  memset(guest_to_host(RESET_VECTOR), 0, CONFIG_MSIZE);
  long size = 0;
  for (int i = 0; i < elfHeader.e_shnum; i++) {
    Elf32_Shdr shdr = sectionHeaderArray[i];
    if (shdr.sh_flags & SHF_ALLOC) {
      size += shdr.sh_size;
      if (shdr.sh_type == SHT_PROGBITS) {
        fseek(fp, shdr.sh_offset, SEEK_SET);
        r = fread(guest_to_host(shdr.sh_addr), shdr.sh_size, 1, fp);
        Assert(r == 1, "Read error.");
      }
    } else if (shdr.sh_type == SHT_SYMTAB) {
      fseek(fp, shdr.sh_offset, SEEK_SET);
      int count = shdr.sh_size / shdr.sh_entsize;
      for (int i = 0; i < count; i++) {
        Elf32_Sym entry;
        r = fread(&entry, sizeof(entry), 1, fp);
        Assert(r == 1, "Read error.");
        if (ELF32_ST_TYPE(entry.st_info) == STT_FUNC) {
          add_sym_table_entry(entry.st_value, entry.st_size, &stringTable[entry.st_name]);
        }
      }
    }
  }
  
  fclose(fp);
  free(stringTable);
  return size;
}

static long load_img() {
  if (img_file == NULL) {
    Log("No image is given. Use the default build-in image.");
    return 4096; // built-in image size
  }

  long size = 0;
  if (img_file_type == NULL) {
    size = load_normal_image();
  } else if (strcmp(img_file_type, "elf") == 0) {
    size = load_elf();
  } else {
    panic("Invalid type of image file.");
  }
  
  return size;
}

static int parse_args(int argc, char *argv[]) {
  const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"type"     , required_argument, NULL, 't'},
    {0          , 0                , NULL,  0 },
  };

  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:d:p:t", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 't': img_file_type = optarg; break;
      case 1: img_file = optarg; break;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\n");
        exit(0);
    }
  }
  return 0;
}

void init_monitor(int argc, char *argv[]) {
  /* Perform some global initialization. */

  /* Parse arguments. */
  parse_args(argc, argv);

  /* Set random seed. */
  init_rand();

  /* Open the log file. */
  init_log(log_file);

  /* Initialize memory. */
  init_mem();

  /* Initialize devices. */
  IFDEF(CONFIG_DEVICE, init_device());

  /* Perform ISA dependent initialization. */
  init_isa();

  /* Load the image to memory. This will overwrite the built-in image. */
  long img_size = load_img();

  /* Initialize differential testing. */
  init_difftest(diff_so_file, img_size, difftest_port);

  /* Initialize the simple debugger. */
  init_sdb();

#ifndef CONFIG_ISA_loongarch32r
  IFDEF(CONFIG_ITRACE, init_disasm(
    MUXDEF(CONFIG_ISA_x86,     "i686",
    MUXDEF(CONFIG_ISA_mips32,  "mipsel",
    MUXDEF(CONFIG_ISA_riscv,
      MUXDEF(CONFIG_RV64,      "riscv64",
                               "riscv32"),
                               "bad"))) "-pc-linux-gnu"
  ));
#endif

  /* Display welcome message. */
  welcome();
}
#else // CONFIG_TARGET_AM
static long load_img() {
  extern char bin_start, bin_end;
  size_t size = &bin_end - &bin_start;
  Log("img size = %ld", size);
  memcpy(guest_to_host(RESET_VECTOR), &bin_start, size);
  return size;
}

void am_init_monitor() {
  init_rand();
  init_mem();
  init_isa();
  load_img();
  IFDEF(CONFIG_DEVICE, init_device());
  welcome();
}
#endif
