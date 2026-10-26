/* SDL calls SDL_main() in libmain.so; forward to the renamed QuakeSpasm main(). */
#include <SDL.h>
extern int quake_main(int argc, char **argv);
int SDL_main(int argc, char **argv) { return quake_main(argc, argv); }
