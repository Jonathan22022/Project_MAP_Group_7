import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
import tensorflow as tf
import numpy as np
import joblib
from tensorflow import keras
import sys

# === 1️⃣ Baca file CSV ===
df = pd.read_csv("task_priority.csv")
df.columns.tolist()

# === 2️⃣ Otomatis deteksi kolom task & priority ===
task_col = None
priority_col = None

for col in df.columns:
    col_lower = col.lower()
    if "task" in col_lower or "descrip" in col_lower:
        task_col = col
    if "priority" in col_lower or "label" in col_lower:
        priority_col = col

if not task_col or not priority_col:
    print("❌ Kolom 'Task' atau 'Priority' tidak ditemukan. Pastikan file punya dua kolom utama.")
    sys.exit()

print(f"✅ Menggunakan kolom teks: '{task_col}' dan kolom label: '{priority_col}'")

# === 3️⃣ Ambil data ===
X = df[task_col].astype(str)
y = df[priority_col].astype(str)

# === 4️⃣ TF-IDF + Logistic Regression ===
print("\n🔧 Sedang melatih model Logistic Regression...")
vectorizer = TfidfVectorizer(max_features=5000)
X_vec = vectorizer.fit_transform(X)
model = LogisticRegression(max_iter=1000)
model.fit(X_vec, y)
print("✅ Model Logistic Regression selesai dilatih!")

# === 5️⃣ Simpan model & vectorizer ===
joblib.dump(model, "priority_model.pkl")
joblib.dump(vectorizer, "vectorizer.pkl")
print("💾 File 'priority_model.pkl' dan 'vectorizer.pkl' berhasil disimpan.")

# === 6️⃣ Buat model dummy untuk ekspor ke TensorFlow Lite ===
print("\n🔁 Mengonversi ke TensorFlow Lite...")
input_layer = keras.Input(shape=(5000,), dtype=tf.float32)
output_layer = keras.layers.Dense(len(np.unique(y)), activation='softmax')(input_layer)
keras_model = keras.Model(inputs=input_layer, outputs=output_layer)
keras_model.compile(optimizer='adam', loss='categorical_crossentropy')

converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
tflite_model = converter.convert()

with open("priority_model.tflite", "wb") as f:
    f.write(tflite_model)

print("✅ File 'priority_model.tflite' berhasil dibuat!")
print("\n🎉 Training dan konversi selesai tanpa error.")