cmake_minimum_required(VERSION 3.22)

project(cactus_jni C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CACTUS_DIR "${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_ABI}")

if(NOT EXISTS "${CACTUS_DIR}/libcactus.a")
    message(FATAL_ERROR "Cactus engine for ${ANDROID_ABI} is missing at ${CACTUS_DIR}")
endif()

add_library(cactus_jni SHARED cactus_jni.cpp)

target_include_directories(cactus_jni PRIVATE "${CACTUS_DIR}")
target_link_libraries(cactus_jni "${CACTUS_DIR}/libcactus.a" log android m)

set_target_properties(cactus_jni PROPERTIES LINKER_LANGUAGE CXX)

target_compile_options(cactus_jni PRIVATE
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -ffunction-sections
        -fdata-sections)
target_link_options(cactus_jni PRIVATE -Wl,--no-undefined -Wl,--gc-sections -Wl,--exclude-libs,ALL)

if(NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_options(cactus_jni PRIVATE -O3)
endif()
