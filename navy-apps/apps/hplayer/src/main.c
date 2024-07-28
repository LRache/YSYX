#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <SDL.h>
#include <fixedptc.h>

#define MUSIC_PATH "/share/music/xitiejie.pcm"
#define SAMPLES 1024
#define FPS 10
#define W 400
#define H 100
#define MAX_VOLUME 128

#define FREQ 10000
#define CHANNELS 1

SDL_Surface *screen = NULL;
int is_end = 0;
int16_t *stream_save = NULL;
int volume = MAX_VOLUME;

uint8_t *buf = NULL;
uint32_t played = 0;
uint32_t buf_length = 0;

static void drawVerticalLine(int x, int y0, int y1, uint32_t color)
{
    assert(y0 <= y1);
    int i;
    uint32_t *p = (void *)screen->pixels;
    for (i = y0; i <= y1; i++)
    {
        p[i * W + x] = color;
    }
}

static void visualize(int16_t *stream, int samples)
{
    int i;
    static int color = 0;
    SDL_FillRect(screen, NULL, 0);
    int center_y = H / 2;
    for (i = 0; i < samples; i++)
    {
        fixedpt multipler = fixedpt_cos(fixedpt_divi(fixedpt_muli(FIXEDPT_PI, 2 * i), samples));
        int x = i * W / samples;
        int y = center_y - fixedpt_toint(fixedpt_muli(fixedpt_divi(fixedpt_muli(multipler, stream[i]), 32768), H / 2));
        if (y < center_y)
            drawVerticalLine(x, y, center_y, color);
        else
            drawVerticalLine(x, center_y, y, color);
        color++;
        color &= 0xffffff;
    }
    SDL_UpdateRect(screen, 0, 0, 0, 0);
}

static void AdjustVolume(int16_t *stream, int samples)
{
    if (volume == MAX_VOLUME)
        return;
    if (volume == 0)
    {
        memset(stream, 0, samples * sizeof(stream[0]));
        return;
    }
    int i;
    for (i = 0; i < samples; i++)
    {
        stream[i] = stream[i] * volume / MAX_VOLUME;
    }
}

void FillAudio(void *userdata, uint8_t *stream, int len)
{
    uint32_t nbyte;
    if (len <= buf_length) nbyte = len;
    else {nbyte = buf_length;}
    buf_length -= nbyte;

    memcpy(stream, buf + played, nbyte);
    played += nbyte;
    if (nbyte < len) {
        memset(stream + nbyte, 0, len - nbyte);
        is_end = 1;
    }
    
    memcpy(stream_save, stream, len);
}

int main(int argc, char *argv[])
{
    SDL_Init(0);
    screen = SDL_SetVideoMode(W, H, 32, SDL_HWSURFACE);
    SDL_FillRect(screen, NULL, 0);
    SDL_UpdateRect(screen, 0, 0, 0, 0);

    char *music_path = MUSIC_PATH;
    int freq = FREQ;
    if (argc > 1) {
        music_path = argv[1];
    }
    if (argc > 2) {
        freq = atoi(argv[2]);
    }
    FILE *fp = fopen(music_path, "r");
    assert(fp);

    fseek(fp, 0, SEEK_END);
    size_t size = ftell(fp);
    buf = malloc(size);
    assert(buf);
    memset(buf, 0, size);
    buf_length = size;
    fseek(fp, 0, SEEK_SET);
    int ret = fread(buf, size, 1, fp);
    printf("%d\n", ret);
    assert(ret == 1);
    fclose(fp);

    SDL_AudioSpec spec;
    spec.freq = freq;
    spec.channels = CHANNELS;
    spec.samples = SAMPLES;
    spec.format = AUDIO_S16SYS;
    spec.userdata = NULL;
    spec.callback = FillAudio;
    SDL_OpenAudio(&spec, NULL);

    stream_save = malloc(size);
    assert(stream_save);
    printf("Playing %s(freq = %d, channels = %d)...\n", music_path, freq, CHANNELS);
    SDL_PauseAudio(0);

    while (!is_end)
    {
        SDL_Event ev;
        while (SDL_PollEvent(&ev))
        {
            if (ev.type == SDL_KEYDOWN)
            {
                switch (ev.key.keysym.sym)
                {
                case SDLK_MINUS:
                    if (volume >= 8)
                        volume -= 8;
                    break;
                case SDLK_EQUALS:
                    if (volume <= MAX_VOLUME - 8)
                        volume += 8;
                    break;
                }
            }
        }
        SDL_Delay(1000 / FPS);
        visualize(stream_save, SAMPLES * CHANNELS);
    }

    SDL_CloseAudio();
    SDL_Quit();
    free(stream_save);
    free(buf);

    return 0;
}