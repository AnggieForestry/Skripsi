from flask import Flask, request, jsonify
import cv2
import numpy as np
import base64
import tensorflow as tf
from tensorflow.keras.models import load_model

app = Flask(__name__)
model = load_model("best_model_24_fold_1.h5")
labels = ["Cyst",
    "Normal",
    "Stone",
    "Tumor"]

@app.route('/api/classify', methods=['POST'])
def classify():
    try:
        data = request.get_json(force=True)
        base64_image = data.get("image")

        if not base64_image:
            return jsonify({"error": "No image provided"}), 400

        image_data = np.frombuffer(base64.b64decode(base64_image), np.uint8)
        image = cv2.imdecode(image_data, cv2.IMREAD_COLOR)

        if image is None:
            return jsonify({"error": "Invalid image format"}), 400

        image = cv2.resize(image, (224,224), interpolation=cv2.INTER_AREA)
        image = image / 255.0
        image = np.expand_dims(image, axis=0)

        result = model.predict(image)
        predicted_class = np.argmax(result, axis=1)[0]
        print(predicted_class)
        print({"message": "sukses", "result": labels[predicted_class], "confidence": result[0][predicted_class]})
        return jsonify({"message": "sukses", "result": labels[predicted_class], "confidence": str(result[0][predicted_class])}), 201

    except Exception as e:
        print(e)
        return jsonify({"error": str(e)}), 500


# Menjalankan aplikasi
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
