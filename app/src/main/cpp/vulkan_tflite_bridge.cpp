#include <jni.h>
#include <vulkan/vulkan.h>
#include <android/log.h>
#include <string>
#include <vector>

#define LOG_TAG "IsquVulkanNdk30"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_isqu_ai_engine_VulkanNativeBridge_probeVulkanGpuDevice(
        JNIEnv* env,
        jobject /* thiz */) {
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "ISQU AI";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "IsquTFLiteVulkanBridge";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_3;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    if (vkCreateInstance(&createInfo, nullptr, &instance) != VK_SUCCESS) {
        return env->NewStringUTF("VulkanUnavailable");
    }

    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        vkDestroyInstance(instance, nullptr);
        return env->NewStringUTF("NoVulkanGpuFound");
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    VkPhysicalDeviceProperties props{};
    vkGetPhysicalDeviceProperties(devices[0], &props);
    LOGI("Detected Vulkan GPU: %s (API %u.%u)",
         props.deviceName,
         VK_VERSION_MAJOR(props.apiVersion),
         VK_VERSION_MINOR(props.apiVersion));

    std::string summary = std::string(props.deviceName) +
        " | Vulkan " + std::to_string(VK_VERSION_MAJOR(props.apiVersion)) +
        "." + std::to_string(VK_VERSION_MINOR(props.apiVersion));

    vkDestroyInstance(instance, nullptr);
    return env->NewStringUTF(summary.c_str());
}
