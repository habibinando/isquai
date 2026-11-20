cmake_minimum_required(VERSION 3.22.1)
project("isqu_vulkan_bridge")

add_library(isqu_vulkan_bridge SHARED vulkan_tflite_bridge.cpp)

find_library(log-lib log)
find_library(vulkan-lib vulkan)

target_link_libraries(
    isqu_vulkan_bridge
    ${vulkan-lib}
    ${log-lib}
)
