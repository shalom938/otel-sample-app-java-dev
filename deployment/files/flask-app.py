from flask import Flask
import uuid
import os
from flask import request
import json
app = Flask(__name__)
data_dir = os.environ.get("DATA_DIR", "./data")
print(f"data dir: {data_dir}")

@app.route("/")
def hello():
    return "Hello from FlaskDb in Kubernetes!"

@app.route("/feedbacks", methods=["POST"])
def insert():
    try:
        text = request.get_data(as_text=True)
        filename = f"{data_dir}/{uuid.uuid4()}.txt"
        with open(filename, "w") as f:
            # Write the original text
            f.write(text)
            f.write("A" * 10 * 1024 * 1024) # 10MB
        return f"File created at {filename}", 201
    except Exception as e:
        return str(e), 500

@app.route("/feedbacks", methods=["GET"])
def get():
    files = [f for f in os.listdir(data_dir) if os.path.isfile(os.path.join(data_dir, f))]
    files = sorted(files, reverse=True)[:10]
    feedbacks = []
    for fname in files:
        path = os.path.join(data_dir, fname)
        with open(path, "r") as f:
            content = f.read()
            content = content.split('A'*100, 1)[0]
            try:
                feedback = json.loads(content)
            except Exception:
                feedback = {}
        feedbacks.append(feedback)
    return feedbacks, 200

@app.route("/feedbacks", methods=["DELETE"])
def clear():
    try:
        files = [f for f in os.listdir(data_dir) if os.path.isfile(os.path.join(data_dir, f))]
        for fname in files:
            os.remove(os.path.join(data_dir, fname))
        return {"deleted": len(files)}, 200
    except Exception as e:
        return str(e), 500

@app.route("/feedbacks/count", methods=["GET"])
def count():
    files = [f for f in os.listdir(data_dir) if os.path.isfile(os.path.join(data_dir, f))]
    return {"count": len(files)}, 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=27017)