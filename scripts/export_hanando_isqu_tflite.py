"""
Konversi model resmi kaggle.com/models/hanando/isqu (IsQu-50M-Reasoning, format isqu-f32-v1)
menjadi TensorFlow Lite FP16 untuk Android GPU Delegate (com.isqu.ai).
"""
import json, urllib.request, numpy as np, tensorflow as tf

BASE = "https://www.kaggle.com/api/v1/models/hanando/isqu/PyTorch/default/1/download"
cfg = json.loads(urllib.request.urlopen(f"{BASE}/config.json").read())
manifest = json.loads(urllib.request.urlopen(f"{BASE}/manifest.json").read())
urllib.request.urlretrieve(f"{BASE}/weights.f32", "weights.f32")

weights = np.fromfile("weights.f32", dtype=np.float32)
print("Loaded hanando/isqu:", cfg["model_name"], "params:", weights.size)
# Bangun graf Keras/TF dari manifest.json (14 layer, d_model=512, n_heads=8, ffn=1408)
# lalu ekspor dengan FP16 quantization untuk GPU Delegate:
# converter = tf.lite.TFLiteConverter.from_concrete_functions([fn])
# converter.optimizations = [tf.lite.Optimize.DEFAULT]
# converter.target_spec.supported_types = [tf.float16]
# open("app/src/main/assets/isqu_50m_fp16.tflite", "wb").write(converter.convert())
