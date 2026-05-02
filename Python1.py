from flask import Flask, jsonify, request

app = Flask(__name__)

# Simulated in-memory database
users = {
    1: {"name": "Alice", "email": "alice@example.com", "user_id": 1},
    2: {"name": "Bob",   "email": "bob@example.com", "user_id": 2}
}

# ─────────────────────────────────────────
# GET - Fetch user(s)
# ─────────────────────────────────────────
@app.route("/", methods=["GET"])
def hello():
    return jsonify({"message": "Hello, World!"})

@app.route("/users", methods=["GET"])
def get_users():
    return jsonify({"users": users})

@app.route("/users/<int:user_id>", methods=["GET"])
def get_user(user_id):
    user = users.get(user_id)
    if not user:
        return jsonify({"error": "User not found"}), 404
    return jsonify({"user": user})

# ─────────────────────────────────────────
# POST - Create a new user
# ─────────────────────────────────────────
@app.route("/greet", methods=["POST"])
def greet():
    data = request.get_json()
    name = data.get("name", "stranger")
    return jsonify({"message": f"Hello, {name}!"})

@app.route("/users", methods=["POST"])
def create_user():
    data = request.get_json()

    if not data or "name" not in data or "email" not in data:
        return jsonify({"error": "Name and email are required"}), 400

    new_id = max(users.keys()) + 1
    users[new_id] = {"name": data["name"], "email": data["email"]}

    return jsonify({"message": "User created", "user": users[new_id], "id": new_id}), 201

# ─────────────────────────────────────────
# PUT - Replace entire user (full update)
# ─────────────────────────────────────────
@app.route("/users/<int:user_id>", methods=["PUT"])
def put_user(user_id):
    if user_id not in users:
        return jsonify({"error": "User not found"}), 404

    data = request.get_json()

    if not data or "name" not in data or "email" not in data:
        return jsonify({"error": "Name and email are required for PUT"}), 400

    # Replaces the entire user object
    users[user_id] = {"name": data["name"], "email": data["email"]}

    return jsonify({"message": "User fully updated", "user": users[user_id]})

# ─────────────────────────────────────────
# PATCH - Update part of a user (partial update)
# ─────────────────────────────────────────
@app.route("/users/<int:user_id>", methods=["PATCH"])
def patch_user(user_id):
    if user_id not in users:
        return jsonify({"error": "User not found"}), 404

    data = request.get_json()

    # Only updates fields that are provided
    if "name" in data:
        users[user_id]["name"] = data["name"]
    if "email" in data:
        users[user_id]["email"] = data["email"]

    return jsonify({"message": "User partially updated", "user": users[user_id]})

# ─────────────────────────────────────────
# DELETE - Remove a user
# ─────────────────────────────────────────
@app.route("/users/<int:user_id>", methods=["DELETE"])
def delete_user(user_id):
    if user_id not in users:
        return jsonify({"error": "User not found"}), 404

    deleted_user = users.pop(user_id)

    return jsonify({"message": "User deleted", "deleted_user": deleted_user})

# ─────────────────────────────────────────
if __name__ == "__main__":
    app.run(debug=True)