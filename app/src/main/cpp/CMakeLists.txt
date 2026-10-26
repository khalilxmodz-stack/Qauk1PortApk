cmake_minimum_required(VERSION 3.22)
project(khalilxmodz_port C CXX)

get_filename_component(REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../.." ABSOLUTE)
set(TP "${REPO_ROOT}/third_party")
foreach(d SDL2 quakespasm gl4es)
  if(NOT EXISTS "${TP}/${d}")
    message(FATAL_ERROR "third_party/${d} missing - run scripts/fetch_deps.sh first")
  endif()
endforeach()

# Always optimise (AGP's Debug config would otherwise be -O0).
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -fcommon -w")

# ---- SDL2 (shared) ---------------------------------------------------------
set(SDL_SHARED ON  CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST   OFF CACHE BOOL "" FORCE)
add_subdirectory("${TP}/SDL2" SDL2-build)

# ---- gl4es (desktop GL 2.1 on top of GLES2); produces libGL.so --------------
set(NOX11     ON  CACHE BOOL "" FORCE)
set(STATICLIB OFF CACHE BOOL "" FORCE)
add_subdirectory("${TP}/gl4es" gl4es-build)

# ---- QuakeSpasm engine ------------------------------------------------------
set(QS "${TP}/quakespasm/Quake")
file(GLOB QUAKE_SRC "${QS}/*.c")
list(FILTER QUAKE_SRC EXCLUDE REGEX "(_win|_wins|_wipx|_wso|_osx|_mac|_dos)\\.c$|cd_sdl\\.c$")

# The engine's main() becomes quake_main(); android_main.c provides SDL_main().
set_source_files_properties("${QS}/main_sdl.c" "${QS}/sys_sdl_unix.c" PROPERTIES
  COMPILE_DEFINITIONS "main=quake_main;SDL_MAIN_HANDLED")

add_library(main SHARED ${QUAKE_SRC} android_input.c android_main.c)
target_include_directories(main PRIVATE "${QS}" "${TP}/SDL2/include")
target_compile_definitions(main PRIVATE USE_SDL2 GL_GLEXT_PROTOTYPES=0)
target_link_libraries(main GL SDL2 z log android dl m)
