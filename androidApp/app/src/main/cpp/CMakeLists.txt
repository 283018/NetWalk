# Minimum CMake version required
cmake_minimum_required(VERSION 3.21)

# Define iPerf version ONCE
set(PACKAGE_VERSION "3.21")

# Project name
project("netwalkIperf")

# Print current CMake version
message(STATUS "CMake version in use: ${CMAKE_VERSION}")

# Define source folder paths
set(IPERF_SRC_DIR ${CMAKE_SOURCE_DIR}/iperf/third_party/src)
set(JNI_SRC_FILE ${CMAKE_SOURCE_DIR}/iperf/iperf_jni.c)

#  Inject custom Android config header for iPerf
# Generate iperf_config.h from template
configure_file(
        ${CMAKE_SOURCE_DIR}/iperf/iperf_config_android.h
        ${IPERF_SRC_DIR}/iperf_config.h
)

# Generate version.h from version.h.in
configure_file(
        ${IPERF_SRC_DIR}/version.h.in
        ${IPERF_SRC_DIR}/version.h
        @ONLY
)


# Set C standard
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Possibly prevents linker policy violation
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Declare native shared library
add_library(netwalkIperf SHARED
        ${JNI_SRC_FILE}                        # JNI interface
        ${IPERF_SRC_DIR}/iperf_pthread.c      # pthread workaround
        ${IPERF_SRC_DIR}/iperf_api.c
        ${IPERF_SRC_DIR}/iperf_client_api.c
        ${IPERF_SRC_DIR}/iperf_util.c
        ${IPERF_SRC_DIR}/iperf_udp.c
        ${IPERF_SRC_DIR}/iperf_tcp.c
        ${IPERF_SRC_DIR}/net.c
        ${IPERF_SRC_DIR}/timer.c
        ${IPERF_SRC_DIR}/units.c
        ${IPERF_SRC_DIR}/iperf_error.c
        ${IPERF_SRC_DIR}/iperf_time.c
        ${IPERF_SRC_DIR}/iperf_locale.c
        ${IPERF_SRC_DIR}/cjson.c
        ${IPERF_SRC_DIR}/tcp_info.c
        ${IPERF_SRC_DIR}/dscp.c
)

# Add include directories
target_include_directories(netwalkIperf PRIVATE
        ${IPERF_SRC_DIR}       # for iperf headers
        ${CMAKE_SOURCE_DIR}/iperf  # for iperf_jni.h if needed
)

# Preprocessor macros
target_compile_definitions(netwalkIperf PRIVATE HAVE_PTHREAD)

# silence cont conversion warning for cJSON
target_compile_options(netwalkIperf PRIVATE -Wno-implicit-const-int-float-conversion)

# Android-specific macro
if (ANDROID)
    target_compile_definitions(netwalkIperf PRIVATE __ANDROID__)
endif()

# Required libraries
find_package(Threads REQUIRED)
target_link_libraries(netwalkIperf
        Threads::Threads
        log
        android
)
